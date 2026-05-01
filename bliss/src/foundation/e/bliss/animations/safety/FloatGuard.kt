/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    bliss/src/foundation/e/bliss/animations/safety/FloatGuard.kt
 * Module:  bliss source-set  (foundation.e.bliss.animations.safety)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/animations/safety/):
 *   ├── FloatGuard.kt          — float validator + scan helper                  ← THIS FILE
 *   ├── AnimatorFallback.kt    — try/fallback wrapper for animator construction
 *   └── README.md              — when and why to use these
 *
 * Purpose:
 *   Single point of float validation for animator-derived values. Every float
 *   that flows from a measure-time computation into an Android Animator
 *   (setScaleX, setTranslationY, …) goes through here. In debug, throws so
 *   real-state bugs surface early; in release, logs at WARN and returns a
 *   finite fallback so the animator can fall back via [AnimatorFallback].
 *   Consolidates the hand-rolled NaN guards added in Migration03 §1 to
 *   FolderAnimationData.
 *
 * Consumed by:
 *   - com.android.launcher3.folder.FolderAnimationData          — measure-time divide + post-construction scan
 *
 * Plan reference: Plans/Migration04/06-animation-safety.md §3
 */
package foundation.e.bliss.animations.safety

import android.util.Log
import com.android.launcher3.BuildConfig

object FloatGuard {

    private const val TAG = "FloatGuard"

    /**
     * Test-only override of the debug-vs-release switch. Production code never mutates this;
     * [FloatGuardTest] flips it to exercise release-mode behaviour without rebuilding. See Plan §6.
     */
    internal var debugMode: Boolean = BuildConfig.DEBUG

    /**
     * Asserts every value in [floats] is a finite float. The [name] identifies the call site for
     * diagnostics. Returns true if all finite; on failure, throws (debug) or logs WARN and returns
     * false (release).
     */
    fun requireAllFinite(name: String, vararg floats: Float): Boolean {
        for (i in floats.indices) {
            if (!floats[i].isFinite()) {
                fail(name, i, floats[i])
                return false
            }
        }
        return true
    }

    /**
     * Computes [numerator] / [denominator] with safety. Returns the quotient if both inputs are
     * finite and the denominator is strictly positive; otherwise fails (debug crash, release
     * log + [fallback]).
     */
    fun safeDivide(
        name: String,
        numerator: Float,
        denominator: Float,
        fallback: Float = 1f,
    ): Float {
        if (denominator <= 0f || !denominator.isFinite() || !numerator.isFinite()) {
            fail(name, -1, denominator)
            return fallback
        }
        return numerator / denominator
    }

    private fun fail(name: String, idx: Int, value: Float) {
        val msg = "FloatGuard: non-finite at $name [idx=$idx, value=$value]"
        if (debugMode) throw IllegalStateException(msg)
        Log.w(TAG, msg)
    }
}
