package foundation.e.bliss.folder

import android.app.WallpaperManager
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.ServiceManager
import android.provider.Settings
import android.util.Log
import android.view.IWindowManager
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

class GlassFolderDelegate(private val resolver: ContentResolver) {

    companion object {
        private const val SETTING = "legacydroid_liquid_glass"
        private const val TAG = "GlassFolder"
    }

    fun isEnabled(): Boolean =
        Settings.Global.getInt(resolver, SETTING, 0) == 1

    private var composeView: ComposeView? = null
    private var observer: android.database.ContentObserver? = null
    private var toggleState = mutableStateOf(false)
    private var savedBackgroundAlpha: Int = -1
    private val wallpaperBitmap = mutableStateOf<Bitmap?>(null)
    private var captureThread: Thread? = null

    private fun createPlaceholderBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(20, 20, 30))
        return bitmap
    }

    private fun captureWallpaper(context: Context): Bitmap? {
        try {
            val wms = IWindowManager.Stub.asInterface(ServiceManager.getService("window"))
            val screenshot = wms.screenshotWallpaper()
            if (screenshot != null) {
                Log.d(TAG, "Captured wallpaper via IWindowManager: ${screenshot.width}x${screenshot.height}")
                return screenshot
            }
        } catch (e: Exception) {
            Log.e(TAG, "IWindowManager.screenshotWallpaper failed: ${e.message}")
        }

        try {
            val wm = WallpaperManager.getInstance(context)
            val drawable = wm.peekDrawable() ?: return null
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            Log.d(TAG, "Loaded wallpaper via peekDrawable: ${bitmap.width}x${bitmap.height}")
            return bitmap
        } catch (e: Exception) {
            Log.e(TAG, "peekDrawable fallback failed: ${e.message}")
            return null
        }
    }

    fun updateWallpaper(bitmap: Bitmap?) {
        wallpaperBitmap.value = bitmap
    }

    fun applyToFolderPage(container: FrameLayout, cornerRadiusDp: Float, wallpaperBitmap: Bitmap? = null) {
        removeFromFolderPage(container)

        toggleState.value = isEnabled()

        if (wallpaperBitmap != null) {
            this.wallpaperBitmap.value = wallpaperBitmap
        } else {
            this.wallpaperBitmap.value = null
            captureThread = Thread {
                val captured = captureWallpaper(container.context)
                Handler(Looper.getMainLooper()).post {
                    this@GlassFolderDelegate.wallpaperBitmap.value = captured
                }
            }.also { it.start() }
        }

        val context = container.context

        val cv = ComposeView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isClickable = false
            isFocusable = false
            tag = "glass_folder"
            setContent {
                val backdrop = rememberLayerBackdrop()
                val fallbackBitmap = remember { createPlaceholderBitmap() }

                Box(Modifier.fillMaxSize()) {
                    val bmp = this@GlassFolderDelegate.wallpaperBitmap.value ?: fallbackBitmap
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .layerBackdrop(backdrop)
                            .fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    GlassFolderOverlay(
                        backdrop = backdrop,
                        isEnabled = toggleState.value
                    )
                }
            }
        }

        composeView = cv
        container.addView(cv, 0)
        updateFolderBackground(container)
        container.requestLayout()

        val uri = Settings.Global.getUriFor(SETTING)
        val obs = object : android.database.ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                toggleState.value = isEnabled()
                updateFolderBackground(container)
            }
        }
        observer = obs
        resolver.registerContentObserver(uri, false, obs)

        Log.d(TAG, "applyToFolderPage: corner=$cornerRadiusDp enabled=${isEnabled()}")
    }

    private fun updateFolderBackground(container: FrameLayout) {
        val bg = container.background ?: return
        if (isEnabled()) {
            // Save original alpha, then make transparent — preserves GradientDrawable type
            if (savedBackgroundAlpha < 0) {
                savedBackgroundAlpha = bg.alpha
            }
            bg.alpha = 0
        } else if (savedBackgroundAlpha >= 0) {
            bg.alpha = savedBackgroundAlpha
            savedBackgroundAlpha = -1
        }
    }

    fun removeFromFolderPage(container: FrameLayout) {
        captureThread?.interrupt()
        captureThread = null
        observer?.let { resolver.unregisterContentObserver(it) }
        observer = null
        composeView?.let { container.removeView(it) }
        composeView = null
        // Restore background alpha (we mutate in-place, never replace)
        if (savedBackgroundAlpha >= 0) {
            container.background?.alpha = savedBackgroundAlpha
            savedBackgroundAlpha = -1
        }
    }
}
