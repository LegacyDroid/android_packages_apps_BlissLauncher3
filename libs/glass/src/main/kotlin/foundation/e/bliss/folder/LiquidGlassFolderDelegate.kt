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
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.Shader.TileMode
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView

/**
 * Manages liquid glass backdrop effect for folder views.
 * Uses plain ImageView + View.setRenderEffect blur.
 */
class LiquidGlassFolderDelegate(private val resolver: android.content.ContentResolver) {

    companion object {
        private const val SETTING = "legacydroid_liquid_glass"
        private const val TAG = "LiquidGlass"
    }

    fun isEnabled(): Boolean =
        Settings.Global.getInt(resolver, SETTING, 0) == 1

    private var blurView: ImageView? = null
    private var highlightView: ImageView? = null

    fun applyToFolderPage(container: FrameLayout, cornerRadiusDp: Float) {
        removeFromFolderPage(container)

        val w = container.width.coerceAtLeast(1)
        val h = container.height.coerceAtLeast(1)

        // Layer 1: source gradient → apply View-level blur
        val source = createGradientBitmap(w, h)

        blurView = ImageView(container.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setImageBitmap(source)
            scaleType = ImageView.ScaleType.FIT_XY
            isClickable = false
            isFocusable = false
            tag = "liquid_glass_blur"
        }

        // Apply blur + vibrancy via View.setRenderEffect (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Step 1: blur
            val blurEffect = RenderEffect.createBlurEffect(
                30f, 30f, TileMode.MIRROR
            )
            // Step 2: vibrancy (boost saturation 1.5x) chained on top of blur
            val cm = ColorMatrix().apply { setSaturation(1.5f) }
            val vibrancyEffect = RenderEffect.createColorFilterEffect(
                ColorMatrixColorFilter(cm), blurEffect
            )
            blurView!!.setRenderEffect(vibrancyEffect)
        }

        // Layer 2: glass highlight (sharp, not blurred)
        val highlight = createHighlightBitmap(w, h)
        highlightView = ImageView(container.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setImageBitmap(highlight)
            scaleType = ImageView.ScaleType.FIT_XY
            isClickable = false
            isFocusable = false
            tag = "liquid_glass_highlight"
        }

        container.addView(blurView, 0)
        container.addView(highlightView, 1)
        container.requestLayout()

        Log.d(TAG, "applyToFolderPage: ${w}x${h} corner=$cornerRadiusDp")
    }

    fun removeFromFolderPage(container: FrameLayout) {
        blurView?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                it.setRenderEffect(null)
            }
            container.removeView(it)
        }
        highlightView?.let { container.removeView(it) }
        blurView = null
        highlightView = null
    }

    /** Wallpaper-like gradient bitmap — smooth tones so blur + vibrancy are visible */
    private fun createGradientBitmap(w: Int, h: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = android.graphics.Paint()
        paint.shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(
                0xFF1B3A5C.toInt(),  // deep blue
                0xFF2D5F8A.toInt(),  // mid blue
                0xFF4A3F6B.toInt(),  // purple
                0xFF6B3A5C.toInt(),  // mauve
                0xFF3D2B4A.toInt()   // dark purple
            ),
            null, TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        // Add some light spots for visual interest
        paint.shader = android.graphics.RadialGradient(
            w * 0.3f, h * 0.25f, w * 0.4f,
            intArrayOf(0x44FFFFFF.toInt(), 0x00000000),
            null, TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        return bitmap
    }

    /** Glass specular highlight — radial + edge gradient */
    private fun createHighlightBitmap(w: Int, h: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = android.graphics.Paint()

        // Top-left specular
        paint.shader = android.graphics.RadialGradient(
            w * 0.3f, h * 0.2f, w * 0.6f,
            intArrayOf(
                0x33FFFFFF.toInt(),
                0x14FFFFFF.toInt(),
                0x00000000
            ),
            null, TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        // Bottom edge light
        paint.shader = LinearGradient(
            0f, h * 0.85f, 0f, h.toFloat(),
            intArrayOf(0x00000000, 0x0DFFFFFF.toInt()),
            null, TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        return bitmap
    }
}
