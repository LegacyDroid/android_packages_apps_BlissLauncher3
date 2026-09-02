package foundation.e.bliss.folder

import android.app.WallpaperManager
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
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

    private fun loadWallpaper(context: Context): Bitmap? {
        try {
            val wm = WallpaperManager.getInstance(context)
            val pfd = wm.getWallpaperFile(WallpaperManager.FLAG_SYSTEM)
            if (pfd != null) {
                val bitmap = BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor)
                pfd.close()
                if (bitmap != null) {
                    Log.d(TAG, "Loaded wallpaper via getWallpaperFile: ${bitmap.width}x${bitmap.height}")
                    return bitmap
                }
            }
            Log.w(TAG, "getWallpaperFile returned null")
        } catch (e: Exception) {
            Log.e(TAG, "getWallpaperFile failed: ${e.message}")
        }
        return null
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
                val loaded = loadWallpaper(container.context)
                Handler(Looper.getMainLooper()).post {
                    this@GlassFolderDelegate.wallpaperBitmap.value = loaded
                }
            }.also { it.start() }
        }

        val cv = ComposeView(container.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isClickable = false
            isFocusable = false
            tag = "glass_folder"
            setContent {
                val backdrop = rememberLayerBackdrop()
                val bmp = this@GlassFolderDelegate.wallpaperBitmap.value

                Box(Modifier.fillMaxSize()) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .layerBackdrop(backdrop)
                                .fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
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
        if (savedBackgroundAlpha >= 0) {
            container.background?.alpha = savedBackgroundAlpha
            savedBackgroundAlpha = -1
        }
    }
}
