/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    bliss-animation-safety/src/main/kotlin/foundation/e/bliss/animations/safety/AnimatorFallback.kt
 * Module:  :bliss-animation-safety
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/animations/safety/):
 *   ├── FloatGuard.kt          — float validator + scan helper
 *   ├── AnimatorFallback.kt    — try/fallback wrapper for animator construction  ← THIS FILE
 *   └── README.md              — when and why to use these
 *
 * Purpose:
 *   Wraps animator construction in a Throwable catch. Centralises the
 *   build-or-fallback policy so every animator port (Folder.buildOpenCloseAnimator
 *   today; future ports tomorrow) shares one consistent log + fallback path.
 *   Replaces the inline catch (Throwable) introduced in Migration02 Phase 2.4.
 *
 * Consumed by:
 *   - com.android.launcher3.folder.Folder#buildOpenCloseAnimator
 *
 * Plan reference: Plans/Migration04/06-animation-safety.md §4
 */
package foundation.e.bliss.animations.safety

import android.util.Log

object AnimatorFallback {
    @PublishedApi internal const val TAG = "AnimatorFallback"

    /**
     * Try [build]; if it returns null or throws, log and return [fallback]. `inline` so Kotlin call
     * sites pay no lambda allocation. Java callers invoke this as `AnimatorFallback.tryBuild(name,
     * build, fallback)` with two `kotlin.jvm.functions.Function0` lambdas — explicit
     * `kotlin.jvm.functions.Function0` import required from Java; see Folder.buildOpenCloseAnimator
     * for the canonical Java call site.
     */
    @JvmStatic
    inline fun <T> tryBuild(name: String, build: () -> T?, fallback: () -> T): T =
        try {
            build()
                ?: run {
                    Log.w(TAG, "$name: build() returned null; using fallback")
                    fallback()
                }
        } catch (t: Throwable) {
            Log.w(TAG, "$name: build() threw; using fallback", t)
            fallback()
        }
}
