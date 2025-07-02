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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import com.android.launcher3.taskbar.BlurredBitmapDrawable

class BlurWallpaperFilter(private val context: Context) :
    WallpaperFilter<BlurWallpaperProvider.BlurSizes> {

    override fun apply(
        wallpaper: Bitmap
    ): WallpaperFilter.ApplyTask<BlurWallpaperProvider.BlurSizes> {
        return WallpaperFilter.ApplyTask.create { emitter ->
            var blurBackground: Bitmap? = null
            var blurDock: Bitmap? = null
            var blurAppGroup: Bitmap? = null
            var blurWidget: Bitmap? = null
            try {
                blurBackground = blur(wallpaper, BlurWallpaperProvider.blurConfigBackground)
                blurDock = blur(wallpaper, BlurWallpaperProvider.blurConfigDock)
                blurAppGroup = blur(wallpaper, BlurWallpaperProvider.blurConfigAppGroup)
                blurWidget = blur(wallpaper, BlurWallpaperProvider.blurConfigWidget)
                emitter.onSuccess(
                    BlurWallpaperProvider.BlurSizes(
                        blurBackground,
                        blurDock,
                        blurAppGroup,
                        blurWidget
                    )
                )
            } catch (t: Throwable) {
                blurBackground?.recycle()
                blurDock?.recycle()
                blurAppGroup?.recycle()
                blurWidget?.recycle()
                emitter.onError(t)
            }
        }
    }

    private fun blur(wallpaper: Bitmap, config: BlurWallpaperProvider.BlurConfig): Bitmap {
        val source = if (config.scale == 1) {
            Bitmap.createBitmap(wallpaper)
        } else {
            Bitmap.createScaledBitmap(
                wallpaper,
                wallpaper.width / config.scale,
                wallpaper.height / config.scale,
                true
            )
        }

        if (config.radius == 0 && config.scale == 1) {
            return source
        }
        return applyBlur(source, config.radius.toFloat())
    }

    private fun applyBlur(bitmap: Bitmap, radius: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val blurredDrawable = BlurredBitmapDrawable(bitmap, radius)
        blurredDrawable.setBounds(0, 0, bitmap.width, bitmap.height)
        blurredDrawable.draw(canvas)
        return output
    }
}
