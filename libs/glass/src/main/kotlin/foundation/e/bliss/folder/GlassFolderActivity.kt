package foundation.e.bliss.folder

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.components.FakeGlass
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Standalone Compose Activity with liquid glass overlay.
 * Uses WallpaperManager to render the real device wallpaper behind the glass effect.
 *
 * For launcher folder integration, see GlassFolderDelegate which
 * replicates the same pattern (ComposeView in launcher Activity).
 */
class GlassFolderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            object : androidx.compose.ui.platform.AbstractComposeView(this) {
                @Composable
                override fun Content() {
                    GlassFolderScreen()
                }
            }
        )
    }
}

private const val TAG = "GlassFolderActivity"

private fun logRuntimeShaderSupport() {
    val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    Log.d(TAG, "RuntimeShader support: $supported (API ${Build.VERSION.SDK_INT}, need >= ${Build.VERSION_CODES.TIRAMISU})")
    if (!supported) {
        Log.w(TAG, "Lens effect requires Android 13+ (API 33). Device is API ${Build.VERSION.SDK_INT}.")
    }
}

private fun createPlaceholderBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Color.rgb(20, 20, 30))
    return bitmap
}

private fun loadWallpaperBitmap(context: android.content.Context): Bitmap? {
    return try {
        val wm = WallpaperManager.getInstance(context)
        val drawable = wm.drawable
        if (drawable == null) {
            Log.w(TAG, "WallpaperManager.drawable is null")
            return null
        }
        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        Log.d(TAG, "Loaded wallpaper drawable: ${width}x${height}")
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load wallpaper via WallpaperManager", e)
        null
    }
}

@Composable
fun GlassFolderScreen() {
    val context = LocalContext.current
    logRuntimeShaderSupport()
    var isEnabled by remember {
        mutableStateOf(
            Settings.Global.getInt(context.contentResolver, "legacydroid_liquid_glass", 0) == 1
        )
    }

    val resolver = context.contentResolver
    DisposableEffect(resolver) {
        val uri = Settings.Global.getUriFor("legacydroid_liquid_glass")
        val observer = object : android.database.ContentObserver(android.os.Handler(context.mainLooper)) {
            override fun onChange(selfChange: Boolean) {
                isEnabled = Settings.Global.getInt(resolver, "legacydroid_liquid_glass", 0) == 1
            }
        }
        resolver.registerContentObserver(uri, false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }

    val backdrop = rememberLayerBackdrop()
    var wallpaperBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val fallbackBitmap = remember { createPlaceholderBitmap() }

    DisposableEffect(Unit) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            val bmp = loadWallpaperBitmap(context)
            withContext(Dispatchers.Main) {
                wallpaperBitmap = bmp
            }
        }
        onDispose { job.cancel() }
    }

    Box(Modifier.fillMaxSize()) {
        Image(
            bitmap = (wallpaperBitmap ?: fallbackBitmap).asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .layerBackdrop(backdrop)
                .fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        GlassFolderOverlay(backdrop = backdrop, isEnabled = isEnabled)
    }
}

@Composable
fun GlassFolderOverlay(
    backdrop: LayerBackdrop,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val cornerRadius = 24f
    if (isEnabled) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(cornerRadius.dp) },
                    effects = {
                        val minDimension = size.minDimension
                        vibrancy()
                        blur(8f.dp.toPx())
                        lens(
                            refractionHeight = 0.2f * minDimension * 0.5f,
                            refractionAmount = 0.2f * minDimension,
                            depthEffect = true,
                            chromaticAberration = false
                        )
                    },
                    highlight = { Highlight.Plain }
                )
        )
    } else {
        FakeGlass(
            modifier = modifier
                .fillMaxSize(),
            shape = RoundedRectangle(cornerRadius.dp),
            cornerRadius = cornerRadius.dp
        ) {
            Box(Modifier.fillMaxSize())
        }
    }
}
