/*
 * Copyright (C) 2026 The LegacyDroid Project
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
package foundation.e.bliss.folder

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/**
 * Liquid glass Compose content.
 *
 * The backdrop library's drawBackdrop renderEffect pipeline doesn't function
 * in this AOSP Compose build — effects silently don't apply.
 *
 * Instead this renders the source bitmap directly with glass-like overlays:
 * - Source bitmap (blurred at View level via RenderEffect set in the delegate)
 * - White gradient overlay for glass specular highlights
 * - Subtle border for glass edge definition
 *
 * Blur is applied from LiquidGlassFolderDelegate via composeView.setRenderEffect().
 */
@Composable
fun LiquidGlassContent(
    cornerRadiusDp: Float,
    wallpaperBitmap: Bitmap? = null
) {
    Log.d("LiquidGlass", "LiquidGlassContent: bitmap=${wallpaperBitmap != null} ${wallpaperBitmap?.width}x${wallpaperBitmap?.height} corner=$cornerRadiusDp")

    Box(modifier = Modifier.fillMaxSize()) {
        // Source layer — blurred at View level by the delegate
        wallpaperBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Glass specular highlight — top-left to bottom-right gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.20f),
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(0.3f, 0.2f),
                        radius = 800f
                    )
                )
        )

        // Glass edge light — subtle white border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.25f)
                )
        )
    }
}
