/*
 * Copyright (C) 2025 MURENA SAS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */
package foundation.e.bliss.blur

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import com.android.launcher3.Utilities
import com.android.launcher3.util.DisplayController
import com.android.launcher3.util.Executors
import com.android.launcher3.util.MainThreadInitializedObject
import com.android.launcher3.util.SafeCloseable
import foundation.e.bliss.utils.Logger
import foundation.e.bliss.utils.runOnMainThread
import foundation.e.bliss.utils.safeForEach
import kotlin.math.ceil

@SuppressLint("NewApi")
class BlurWallpaperProvider(val context: Context) : SafeCloseable {

    private val mWallpaperManager: WallpaperManager = WallpaperManager.getInstance(context)
    private val mListeners = ArrayList<Listener>()
    private var mDisplaySize = DisplayController.INSTANCE.get(context).info.currentSize

    var wallpapers: BlurSizes? = null
        private set(value) {
            if (field != value) {
                field = value
            }
        }

    var placeholder: Bitmap? = null
        private set(value) {
            if (field != value) {
                field = value
            }
        }

    private val mVibrancyPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    private var mWallpaperWidth: Int = 0

    private val mUpdateRunnable = Runnable { updateWallpaper() }

    private val wallpaperFilter = BlurWallpaperFilter()
    private var applyTask: WallpaperFilter.ApplyTask<BlurSizes>? = null

    private var updatePending = false

    private var isLiveWallpaper = false

    private var lastOffset = 0.5f

    private var lastScrollOffset = Float.NaN

    init {
        isEnabled = getEnabledStatus()
        updateAsync()
    }

    private fun getEnabledStatus() = mWallpaperManager.wallpaperInfo == null

    fun updateAsync() {
        Executors.THREAD_POOL_EXECUTOR.execute(mUpdateRunnable)
    }

    fun setLiveWallpaper(isLive: Boolean) {
        isLiveWallpaper = isLive
    }

    @SuppressLint("MissingPermission")
    private fun updateWallpaper() {
        if (applyTask != null) {
            updatePending = true
            return
        }

        mDisplaySize = DisplayController.INSTANCE.get(context).info.currentSize
        val width = mDisplaySize.x
        val height = mDisplaySize.y

        // Prepare a placeholder before hand so that it can be used in case wallpaper is null
        placeholder = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(placeholder!!)
        canvas.drawColor(0x44000000)

        if (!isEnabled) {
            wallpapers = null
            runOnMainThread { mListeners.safeForEach(Listener::onEnabledChanged) }
        }

        var wallpaper =
            try {
                val wall = mWallpaperManager.drawable?.toBitmap()
                if (isLiveWallpaper) {
                    wall?.let { createTransparentBitmap(it.width, wall.height) }
                } else {
                    wall
                }
            } catch (e: Exception) {
                runOnMainThread {
                    val msg = "Failed: ${e.message}"
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    notifyWallpaperChanged()
                }
                return
            }

        wallpaper = wallpaper?.let { scaleAndCropToScreenSize(it) }
        if (wallpaper == null) return
        mWallpaperWidth = wallpaper.width

        val offsetY: Float
        if (wallpaper.height >= height) {
            offsetY = (wallpaper.height - height) * 0.5f
            mListeners.forEach { it.onOffsetChanged(offsetY) }
        }

        wallpaper = applyVibrancy(wallpaper)
        Logger.d(TAG, "starting blur")

        applyTask =
            wallpaperFilter.apply(wallpaper).setCallback { result, error ->
                if (error == null) {
                    this@BlurWallpaperProvider.wallpapers = result
                    runOnMainThread(::notifyWallpaperChanged)
                    wallpaper.recycle()
                } else {
                    if (error is OutOfMemoryError) {
                        runOnMainThread {
                            Toast.makeText(context, "Failed!", Toast.LENGTH_LONG).show()
                        }
                        notifyWallpaperChanged()
                    }
                    wallpaper.recycle()
                }
            }
        applyTask = null
        if (updatePending) {
            updatePending = false
            updateWallpaper()
        }
    }

    private fun createTransparentBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint =
            Paint().apply {
                color = Color.argb(68, 0, 0, 0) // Black with semi-transparency
                isAntiAlias = true
            }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }

    private fun notifyWallpaperChanged() {
        mListeners.forEach(Listener::onWallpaperChanged)
    }

    private fun applyVibrancy(wallpaper: Bitmap): Bitmap {
        mVibrancyPaint.colorFilter =
            ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(1.25f) })

        // Single allocation: draw the source through the vibrancy filter into a fresh bitmap.
        return try {
            val bitmap =
                Bitmap.createBitmap(wallpaper.width, wallpaper.height, Bitmap.Config.ARGB_8888)
            Canvas(bitmap).drawBitmap(wallpaper, 0f, 0f, mVibrancyPaint)
            wallpaper.recycle()
            bitmap
        } catch (e: Exception) {
            Logger.e(TAG, "applyVibrancy failed, using source", e)
            wallpaper
        }
    }

    /**
     * The sub-rectangle of the wallpaper bitmap the system shows on this display. With multi-crop
     * the wallpaper parallaxes across a per-orientation crop, not the whole bitmap, so we match it.
     */
    private fun getWallpaperCrop(wallpaper: Bitmap): Rect {
        val full = Rect(0, 0, wallpaper.width, wallpaper.height)
        val crop =
            try {
                mWallpaperManager
                    .getBitmapCrops(
                        listOf(Point(mDisplaySize.x, mDisplaySize.y)),
                        WallpaperManager.FLAG_SYSTEM,
                        false,
                    )
                    ?.firstOrNull()
            } catch (e: Throwable) {
                Logger.e(TAG, "getBitmapCrops failed, using full bitmap", e)
                null
            }

        val safe = crop?.takeUnless { it.isEmpty } ?: return full
        return Rect(
                safe.left.coerceIn(0, wallpaper.width),
                safe.top.coerceIn(0, wallpaper.height),
                safe.right.coerceIn(0, wallpaper.width),
                safe.bottom.coerceIn(0, wallpaper.height),
            )
            .takeIf { it.width() > 0 && it.height() > 0 } ?: full
    }

    /**
     * Crop the wallpaper to [getWallpaperCrop] and scale it to cover the screen. The scaled crop's
     * surplus width is the parallax range the real wallpaper travels, so the 1:1 offset lines up.
     */
    private fun scaleAndCropToScreenSize(wallpaper: Bitmap): Bitmap {
        val width = mDisplaySize.x
        val height = mDisplaySize.y
        val crop = getWallpaperCrop(wallpaper)

        val coverFactor = maxOf(width.toFloat() / crop.width(), height.toFloat() / crop.height())
        if (coverFactor <= 0f) return wallpaper

        val scaledWidth = width.coerceAtLeast(ceil(crop.width() * coverFactor).toInt())
        val scaledHeight = height.coerceAtLeast(ceil(crop.height() * coverFactor).toInt())

        val result = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
        Canvas(result)
            .drawBitmap(
                wallpaper,
                crop,
                Rect(0, 0, scaledWidth, scaledHeight),
                Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG),
            )
        return result
    }

    fun addListener(listener: Listener) {
        mListeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        mListeners.remove(listener)
    }

    fun createBlurDrawable(config: BlurConfig = blurConfigDock) = BlurDrawable(this, config)

    fun setWallpaperOffset(inputOffset: Float?) {
        val offset = inputOffset ?: lastOffset
        lastOffset = offset

        if (!isEnabled) return
        if (wallpapers == null) return

        val availableWidth = mDisplaySize.x - mWallpaperWidth
        var xPixels = availableWidth / 2
        if (availableWidth < 0) {
            xPixels += (availableWidth * (offset - .5f) + .5f).toInt()
        }

        val scrollOffset =
            Utilities.boundToRange(
                (-xPixels).toFloat(),
                0f,
                (mWallpaperWidth - mDisplaySize.x).toFloat()
            )

        if (scrollOffset == lastScrollOffset) return
        lastScrollOffset = scrollOffset

        runOnMainThread { mListeners.forEach { it.onScrollOffsetChanged(scrollOffset) } }
    }

    fun orientationChanged() {
        updateWallpaper()
    }

    interface Listener {
        fun onWallpaperChanged() {}

        fun onEnabledChanged() {}

        fun onScrollOffsetChanged(offset: Float) {}

        fun onOffsetChanged(offset: Float) {}
    }

    data class BlurSizes(
        val background: Bitmap,
        val dock: Bitmap,
        val appGroup: Bitmap,
        val widget: Bitmap
    ) {
        fun recycle() {
            background.recycle()
            dock.recycle()
            appGroup.recycle()
            widget.recycle()
        }
    }

    data class BlurConfig(val getDrawable: (BlurSizes) -> Bitmap, val scale: Int, val radius: Int)

    companion object {
        val INSTANCE = MainThreadInitializedObject { context: Context ->
            BlurWallpaperProvider(context)
        }

        fun getInstance(context: Context): BlurWallpaperProvider {
            return INSTANCE.get(context)
        }

        const val TAG = "BlurWallpaperProvider"

        @JvmField val blurConfigBackground = BlurConfig({ it.background }, 2, 8)

        @JvmField val blurConfigDock = BlurConfig({ it.dock }, 1, 0)

        @JvmField val blurConfigAppGroup = BlurConfig({ it.appGroup }, 6, 8)

        @JvmField val blurConfigWidget = BlurConfig({ it.widget }, 6, 10)

        var isEnabled: Boolean = false
    }

    override fun close() {}
}
