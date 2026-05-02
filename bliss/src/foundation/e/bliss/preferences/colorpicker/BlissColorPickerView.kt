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
// PLAN-DRIFT-M02: Phase 5 — View-based HSV color picker (no Compose). Implements §5.1–§5.7.
package foundation.e.bliss.preferences.colorpicker

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.annotation.ColorInt
import androidx.core.content.edit
import com.android.launcher3.LauncherFiles
import com.android.launcher3.R
import com.android.launcher3.util.Themes
import foundation.e.bliss.preferences.BlissPrefs
import org.json.JSONArray

/**
 * View-based HSV color picker. Self-contained, no Compose. Public API: [setColor] / [getColor].
 *
 * Components:
 * - [SaturationValueGradient] — 2D gradient (X = saturation, Y = inverse value).
 * - Hue [SeekBar] — vertical-style rainbow slider (we use horizontal SeekBar for simplicity, with a
 *   rainbow shader behind it via a custom progress drawable layer).
 * - Alpha [SeekBar] — horizontal slider with checkerboard + current-color overlay background.
 * - Hex [EditText] — 8-char ARGB live-validated.
 * - Swatch [View] — 48dp preview circle.
 * - Recent colors row + system-accent + wallpaper-primary swatches.
 */
class BlissColorPickerView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    private val hsv = floatArrayOf(0f, 1f, 1f)
    private var alpha = 0xFF
    private var suppressHexSync = false
    private var suppressSliderSync = false

    private val saturationValueView: SaturationValueGradient
    private val hueSlider: SeekBar
    private val alphaSlider: SeekBar
    private val hexInput: EditText
    private val swatchView: View
    private val recentPalette: LinearLayout
    private val systemAccentSwatch: ImageButton
    private val wallpaperPrimarySwatch: ImageButton

    private val debounceHandler = Handler(Looper.getMainLooper())
    private var debounceRunnable: Runnable? = null

    interface OnColorChangeListener {
        fun onColorChange(@ColorInt color: Int)
    }

    var listener: OnColorChangeListener? = null

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.bliss_color_picker, this, true)

        saturationValueView = findViewById(R.id.sat_value_view)
        hueSlider = findViewById(R.id.hue_slider)
        alphaSlider = findViewById(R.id.alpha_slider)
        hexInput = findViewById(R.id.hex_input)
        swatchView = findViewById(R.id.swatch)
        recentPalette = findViewById(R.id.recent_palette)
        systemAccentSwatch = findViewById(R.id.swatch_system_accent)
        wallpaperPrimarySwatch = findViewById(R.id.swatch_wallpaper_primary)

        setupSaturationView()
        setupHueSlider()
        setupAlphaSlider()
        setupHexInput()
        setupSpecialSwatches()
        setupRecentColors()
        refreshAll()
    }

    fun setColor(@ColorInt color: Int) {
        alpha = (color ushr 24) and 0xFF
        Color.colorToHSV(color and 0xFFFFFF or (0xFF shl 24), hsv)
        refreshAll()
    }

    @ColorInt fun getColor(): Int = (alpha shl 24) or (Color.HSVToColor(hsv) and 0xFFFFFF)

    private fun setupSaturationView() {
        saturationValueView.contentDescription =
            context.getString(R.string.color_picker_saturation_value_cd)
        saturationValueView.onChange = { s, v ->
            hsv[1] = s
            hsv[2] = v
            suppressSliderSync = true
            refreshSwatchAndHex()
            suppressSliderSync = false
            notifyChange()
        }
    }

    private fun setupHueSlider() {
        hueSlider.max = 360
        hueSlider.contentDescription = context.getString(R.string.color_picker_hue_cd)
        // Rainbow gradient backdrop via shader on a LayerDrawable (kept simple: tint via
        // background).
        hueSlider.progressDrawable = makeHueProgressDrawable()
        hueSlider.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    hsv[0] = progress.toFloat()
                    saturationValueView.setHue(hsv[0])
                    refreshSwatchAndHex()
                    notifyChange()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}

                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            }
        )
    }

    private fun setupAlphaSlider() {
        alphaSlider.max = 255
        alphaSlider.contentDescription = context.getString(R.string.color_picker_alpha_cd)
        alphaSlider.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    alpha = progress
                    refreshSwatchAndHex()
                    notifyChange()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}

                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            }
        )
    }

    private fun setupHexInput() {
        hexInput.filters = arrayOf(InputFilter.LengthFilter(9), InputFilter.AllCaps())
        hexInput.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                    // No-op: only afterTextChanged drives the parse + debounce path.
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // No-op: see beforeTextChanged.
                }

                override fun afterTextChanged(s: Editable?) {
                    if (suppressHexSync) return
                    debounceRunnable?.let(debounceHandler::removeCallbacks)
                    val text = s?.toString() ?: return
                    val r = Runnable {
                        try {
                            val parsed = Color.parseColor(text)
                            // Force #AARRGGBB (parseColor accepts #RRGGBB and returns 0xFFRRGGBB).
                            val effective =
                                if (text.length == 7) (alpha shl 24) or (parsed and 0xFFFFFF)
                                else parsed
                            alpha = (effective ushr 24) and 0xFF
                            Color.colorToHSV(effective and 0xFFFFFF or (0xFF shl 24), hsv)
                            suppressHexSync = true
                            refreshSlidersAndSwatch()
                            suppressHexSync = false
                            hexInput.error = null
                            notifyChange()
                        } catch (_: Throwable) {
                            hexInput.error = context.getString(R.string.color_picker_invalid_hex)
                        }
                    }
                    debounceRunnable = r
                    debounceHandler.postDelayed(r, 250L)
                }
            }
        )
    }

    private fun setupSpecialSwatches() {
        val accent = Themes.getColorAccent(context)
        systemAccentSwatch.setBackgroundColor(accent)
        systemAccentSwatch.contentDescription =
            context.getString(R.string.color_picker_system_accent_cd)
        systemAccentSwatch.setOnClickListener { setColor(accent or (0xFF shl 24)) }

        val wallpaperColor: Int? =
            try {
                WallpaperManager.getInstance(context)
                    ?.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                    ?.primaryColor
                    ?.toArgb()
            } catch (_: Throwable) {
                null
            }
        if (wallpaperColor != null) {
            wallpaperPrimarySwatch.setBackgroundColor(wallpaperColor)
            wallpaperPrimarySwatch.contentDescription =
                context.getString(R.string.color_picker_wallpaper_primary_cd)
            wallpaperPrimarySwatch.setOnClickListener { setColor(wallpaperColor or (0xFF shl 24)) }
        } else {
            wallpaperPrimarySwatch.visibility = GONE
        }
    }

    private fun setupRecentColors() {
        val sp =
            context.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
        val raw = sp.getString(BlissPrefs.PREF_RECENT_COLORS, "[]") ?: "[]"
        val recents = parseRecents(raw)
        recentPalette.removeAllViews()
        val sizePx = (context.resources.displayMetrics.density * 32).toInt()
        val marginPx = (context.resources.displayMetrics.density * 4).toInt()
        recents.forEach { c ->
            val btn =
                ImageButton(context).apply {
                    layoutParams =
                        LinearLayout.LayoutParams(sizePx, sizePx).also {
                            it.setMargins(marginPx, 0, marginPx, 0)
                        }
                    setBackgroundColor(c)
                    contentDescription =
                        context.getString(
                            R.string.color_picker_recent_cd,
                            String.format("#%08X", c),
                        )
                    setOnClickListener { setColor(c) }
                }
            recentPalette.addView(btn)
        }
    }

    private fun refreshAll() {
        suppressHexSync = true
        suppressSliderSync = true
        saturationValueView.setHue(hsv[0])
        saturationValueView.setSaturationValue(hsv[1], hsv[2])
        hueSlider.progress = hsv[0].toInt()
        alphaSlider.progress = alpha
        refreshSwatchAndHex()
        suppressHexSync = false
        suppressSliderSync = false
    }

    private fun refreshSlidersAndSwatch() {
        saturationValueView.setHue(hsv[0])
        saturationValueView.setSaturationValue(hsv[1], hsv[2])
        hueSlider.progress = hsv[0].toInt()
        alphaSlider.progress = alpha
        updateSwatch()
    }

    private fun refreshSwatchAndHex() {
        updateSwatch()
        if (!suppressHexSync) {
            suppressHexSync = true
            hexInput.setText(String.format("#%08X", getColor()))
            hexInput.setSelection(hexInput.text.length)
            suppressHexSync = false
        }
    }

    private fun updateSwatch() {
        swatchView.setBackgroundColor(getColor())
    }

    private fun makeHueProgressDrawable(): android.graphics.drawable.Drawable {
        val w = 1024
        val h = 24
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val colors = IntArray(361) { hue -> Color.HSVToColor(floatArrayOf(hue.toFloat(), 1f, 1f)) }
        val paint =
            Paint().apply {
                shader =
                    LinearGradient(0f, 0f, w.toFloat(), 0f, colors, null, Shader.TileMode.CLAMP)
            }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        return android.graphics.drawable.BitmapDrawable(context.resources, bmp)
    }

    private fun notifyChange() {
        listener?.onColorChange(getColor())
    }

    companion object {
        // Same SharedPreferences file as LauncherPrefs default backed-up storage so the
        // BlissPrefs.PREF_RECENT_COLORS / LauncherPrefs.RECENT_COLORS view round-trips.
        private const val MAX_RECENTS = 8

        @JvmStatic
        fun rememberRecentColor(context: Context, @ColorInt color: Int) {
            val sp =
                context.getSharedPreferences(
                    LauncherFiles.SHARED_PREFERENCES_KEY,
                    Context.MODE_PRIVATE,
                )
            val raw = sp.getString(BlissPrefs.PREF_RECENT_COLORS, "[]") ?: "[]"
            val list = parseRecents(raw).toMutableList()
            list.removeAll { it == color }
            list.add(0, color)
            while (list.size > MAX_RECENTS) list.removeAt(list.size - 1)
            val arr = JSONArray()
            list.forEach { arr.put(String.format("#%08X", it)) }
            sp.edit { putString(BlissPrefs.PREF_RECENT_COLORS, arr.toString()) }
        }

        private fun parseRecents(raw: String): List<Int> {
            return try {
                val arr = JSONArray(raw)
                val out = ArrayList<Int>(arr.length())
                for (i in 0 until arr.length()) {
                    val s = arr.optString(i)
                    if (s.isNotEmpty()) {
                        try {
                            out.add(Color.parseColor(s))
                        } catch (_: Throwable) {
                            // Skip unparseable recents; persisted JSON may pre-date a format
                            // change.
                        }
                    }
                }
                out
            } catch (_: Throwable) {
                emptyList()
            }
        }
    }
}
