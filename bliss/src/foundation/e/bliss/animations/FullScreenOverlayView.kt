/*
 * Copyright (C) 2026 MURENA SAS — adapted from Lawnchair (Apache-2.0).
 *
 * Direct port of Lawnchair's `FullScreenOverlayView`
 * (`lawnchair/src/app/lawnchair/views/FullScreenOverlayView.kt`):
 *  - [pointyEndView] captures the tapped icon's screen-space center
 *  - [suckAnimation] runs the AnimatorSet (translation + scale + outline-rounding
 *    + alpha) Lawnchair uses for SUCK_IN, with the Material emphasized-decelerate
 *    `PathInterpolator(0.22f, 1f, 0.36f, 1f)`
 *  - [fadeIn] does the FADE_IN sequence (scrim alpha in, then out, with an ARGB
 *    color animation back to TRANSPARENT)
 *
 * The overlay self-detaches from its parent in the AnimatorSet's onAnimationEnd
 * regardless of mode, so a launch failure or an exception during animation
 * setup never leaves a stuck scrim on top of the launcher.
 */
package foundation.e.bliss.animations

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.android.launcher3.R

class FullScreenOverlayView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {

    private var endX = 0f
    private var endY = 0f
    private val startColor = ContextCompat.getColor(context, R.color.bliss_close_overlay_color)
    private val endColor = Color.TRANSPARENT
    private var cornerRadius = 0f

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        setBackgroundColor(startColor)
        clipToOutline = true
    }

    fun pointyEndView(view: View) {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        endX = location[0] + view.width / 2f
        endY = location[1] + view.height / 2f
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        outlineProvider =
            object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
                }
            }
    }

    fun suckAnimation(duration: Long = 600, onEnd: (() -> Unit)? = null) {
        post {
            if (!isAttachedToWindow) return@post
            if (endX == 0f || endY == 0f) {
                endX = width / 2f
                endY = height / 2f
            }
            val moveX = ObjectAnimator.ofFloat(this, TRANSLATION_X, 0f, endX - width / 2)
            val moveY = ObjectAnimator.ofFloat(this, TRANSLATION_Y, 0f, endY - height / 2)
            val scaleAnimator = buildSuckScaleAnimator(duration)
            val fadeAnimator = ObjectAnimator.ofFloat(this, ALPHA, 1f, 0f)
            val fadeTrigger = buildSuckFadeTrigger(duration, fadeAnimator)
            AnimatorSet().apply {
                playTogether(scaleAnimator, moveX, moveY, fadeTrigger)
                play(fadeAnimator).after(fadeTrigger)
                this.duration = duration
                interpolator = PathInterpolator(0.22f, 1f, 0.36f, 1f)
                addListener(detachAndCallback(onEnd))
                start()
            }
        }
    }

    private fun buildSuckScaleAnimator(duration: Long): ValueAnimator =
        ValueAnimator.ofFloat(1f, 0.85f, 0.6f, 0.3f, 0.0f).apply {
            this.duration = duration
            addUpdateListener { animator ->
                val progress = animator.animatedFraction
                val scaleFactor = animator.animatedValue as Float
                scaleX = scaleFactor
                scaleY = scaleFactor
                val changeScale = progress < 0.8f
                val minSize = width.coerceAtMost(height) * scaleFactor
                cornerRadius = (width / 2f) * progress
                outlineProvider = suckOutlineProvider(changeScale, minSize)
                invalidate()
            }
        }

    private fun suckOutlineProvider(changeScale: Boolean, minSize: Float): ViewOutlineProvider =
        object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(
                    0,
                    0,
                    if (!changeScale) minSize.toInt() * 6 else view.width,
                    if (!changeScale) minSize.toInt() * 6 else view.height,
                    cornerRadius,
                )
            }
        }

    private fun buildSuckFadeTrigger(duration: Long, fadeAnimator: ObjectAnimator): ValueAnimator =
        ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            addUpdateListener {
                if (animatedFraction > 0.5f && !fadeAnimator.isRunning) fadeAnimator.start()
            }
        }

    fun fadeIn(durationIn: Long = 200, durationOut: Long = 500, onEnd: (() -> Unit)? = null) {
        post {
            if (!isAttachedToWindow) return@post
            val ai =
                ObjectAnimator.ofFloat(this, ALPHA, 0f, 1f).apply {
                    duration = durationIn
                    interpolator = DecelerateInterpolator()
                }
            val ao =
                ObjectAnimator.ofFloat(this, ALPHA, 1f, 0f).apply {
                    duration = durationOut
                    interpolator = AccelerateInterpolator()
                    startDelay = durationIn
                }
            val co =
                ValueAnimator.ofObject(ArgbEvaluator(), startColor, endColor).apply {
                    duration = durationIn + durationOut
                    addUpdateListener { setBackgroundColor(it.animatedValue as Int) }
                }
            AnimatorSet().apply {
                playTogether(ai, ao, co)
                addListener(detachAndCallback(onEnd))
                start()
            }
        }
    }

    private fun detachAndCallback(onEnd: (() -> Unit)?) =
        object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(a: Animator) {
                (parent as? ViewGroup)?.removeView(this@FullScreenOverlayView)
                onEnd?.invoke()
            }
        }

    companion object {
        /**
         * Show the overlay matching the user's [CloseOverlayMode] preference. Returns immediately;
         * the overlay self-detaches when its animation ends (or, for [CloseOverlayMode.NONE], is
         * removed before any animation starts).
         *
         * The overlay must never block icon launch — the call is wrapped in a broad try/catch and
         * the AnimatorSet's listener always detaches the view.
         *
         * @param tappedIcon the BubbleTextView the user actually tapped, or null for non-icon
         *   launches (e.g. drag-and-drop fallback). When null, [suckAnimation] falls back to the
         *   screen center.
         */
        @JvmStatic
        fun showForCloseTransition(activity: Activity, tappedIcon: View?) {
            try {
                val mode = CloseOverlayMode.fromContext(activity)
                if (mode == CloseOverlayMode.NONE) return
                val root =
                    activity.window?.decorView?.findViewById<ViewGroup>(android.R.id.content)
                        ?: return
                val overlay = FullScreenOverlayView(activity)
                if (tappedIcon != null) overlay.pointyEndView(tappedIcon)
                root.addView(overlay)
                root.post {
                    when (mode) {
                        CloseOverlayMode.SUCK_IN -> overlay.suckAnimation()
                        CloseOverlayMode.FADE_IN -> overlay.fadeIn()
                        CloseOverlayMode.NONE -> root.removeView(overlay)
                    }
                }
            } catch (_: Throwable) {
                // Best-effort overlay; never block icon launch on failure.
            }
        }
    }
}
