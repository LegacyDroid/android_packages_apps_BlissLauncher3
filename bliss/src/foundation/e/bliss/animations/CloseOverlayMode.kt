/*
 * Copyright (C) 2026 MURENA SAS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
/*
 * File:    bliss/src/foundation/e/bliss/animations/CloseOverlayMode.kt
 * Module:  bliss source-set  (foundation.e.bliss.animations)
 * Role:    REFACTORED
 *
 * Tree (foundation/e/bliss/animations/):
 *   ├── CloseOverlayMode.kt        — pref-string → mode enum                    ← THIS FILE
 *   ├── FullScreenOverlayView.kt   — Lawnchair-port suck-in / fade-in overlay
 *   └── safety/                    — FloatGuard + AnimatorFallback (Migration04 §06)
 *
 * Purpose:
 *   Maps the [LauncherPrefs.CLOSING_APP_OVERLAY] string pref to the three
 *   modes Lawnchair ships (NONE / FADE_IN / SUCK_IN). Migration04 §06 audit
 *   pass: this file holds no measured floats, no divisions, no animator
 *   build — it is a pref-keyed lookup, so no FloatGuard call sites apply.
 *   The actual SUCK_IN animator with its width/height-based scaling lives
 *   in FullScreenOverlayView; that file's existing fallback (if endX == 0f,
 *   recenter to width/2) and the broad try/catch in showForCloseTransition
 *   already cover the partial-layout case the plan §5.4 describes.
 *   Header refreshed to Form A so the audit decision is auditable.
 *
 * Consumed by:
 *   - foundation.e.bliss.animations.FullScreenOverlayView
 *
 * Plan reference: Plans/Migration04/06-animation-safety.md §5.4
 */
package foundation.e.bliss.animations

import android.content.Context
import com.android.launcher3.LauncherPrefs

/**
 * Lawnchair-aligned closing-app overlay mode. Maps the [LauncherPrefs.CLOSING_APP_OVERLAY] string
 * pref to the three modes Lawnchair actually ships:
 * - [NONE] — no overlay drawn
 * - [FADE_IN] — scrim fades in then out as the started activity covers the screen
 * - [SUCK_IN] — scrim translates / scales toward the tapped icon location
 *
 * The Migration02 / Phase 4 port lives in [FullScreenOverlayView]; this enum is just the
 * pref-string -> kind mapping. The Migration01-era `getDurationMultiplier` /
 * `getInterpolatorOverride` helpers were dropped — the View owns its own animation timing and
 * easing now.
 */
enum class CloseOverlayMode(val prefValue: String) {
    NONE("none"),
    FADE_IN("fade_in"),
    SUCK_IN("suck_in");

    companion object {
        @JvmStatic
        fun fromContext(context: Context): CloseOverlayMode =
            try {
                val raw = LauncherPrefs.get(context).get(LauncherPrefs.CLOSING_APP_OVERLAY)
                entries.firstOrNull { it.prefValue == raw } ?: NONE
            } catch (_: Throwable) {
                NONE
            }
    }
}
