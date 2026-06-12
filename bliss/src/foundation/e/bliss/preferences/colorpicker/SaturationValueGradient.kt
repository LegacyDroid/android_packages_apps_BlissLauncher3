/*
 * Copyright (C) 2026 MURENA SAS
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
 */
/*
 * File:    bliss/src/foundation/e/bliss/preferences/colorpicker/SaturationValueGradient.kt
 * Module:  bliss root app source-set
 * Role:    Custom 2D saturation/value gradient view with touch tracking for color selection.
 */
// PLAN-DRIFT-M02: Phase 5 — extracted top-level so the layout XML can reference it without
// the inner-class `$` syntax that aapt2 / XML parsing rejects.
package foundation.e.bliss.preferences.colorpicker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Custom 2D saturation/value gradient. Saturation = X axis (0..1), Value = Y axis (1..0
 * top→bottom). Drawn by compositing two LinearGradient shaders. Touch events deliver raw X/Y so RTL
 * is handled correctly (XC-cross-cutting alignment).
 */
class SaturationValueGradient
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var hue = 0f
    private var sat = 1f
    private var value = 1f
    var onChange: ((Float, Float) -> Unit)? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cursorPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.WHITE
        }
    private val cursorOuter =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = Color.BLACK
        }

    fun setHue(h: Float) {
        hue = h
        invalidate()
    }

    fun setSaturationValue(s: Float, v: Float) {
        sat = s
        value = v
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        // Saturation gradient white -> pure hue
        val pureHue = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
        val satShader = LinearGradient(0f, 0f, w, 0f, Color.WHITE, pureHue, Shader.TileMode.CLAMP)
        paint.shader = satShader
        paint.xfermode = null
        canvas.drawRect(0f, 0f, w, h, paint)
        // Value overlay transparent -> black, multiplied so saturation is preserved.
        val valShader =
            LinearGradient(0f, 0f, 0f, h, 0x00000000, 0xFF000000.toInt(), Shader.TileMode.CLAMP)
        paint.shader = valShader
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.xfermode = null
        paint.shader = null
        // Selection cursor.
        val cx = sat * w
        val cy = (1f - value) * h
        canvas.drawCircle(cx, cy, 14f, cursorOuter)
        canvas.drawCircle(cx, cy, 14f, cursorPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return false
        // PLAN-DRIFT-M02: use raw event.x/y (not direction-flipped) for RTL correctness.
        val rawX = event.x.coerceIn(0f, w)
        val rawY = event.y.coerceIn(0f, h)
        sat = rawX / w
        value = 1f - (rawY / h)
        invalidate()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE,
            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                onChange?.invoke(sat, value)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
