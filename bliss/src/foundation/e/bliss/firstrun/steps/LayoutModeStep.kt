/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/src/foundation/e/bliss/firstrun/steps/LayoutModeStep.kt
 * Module:  bliss source-set  (foundation.e.bliss.firstrun.steps)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/firstrun/steps/):
 *   ├── LayoutModeStep.kt        — Simple-vs-Classic chooser, ported from LayoutModeChooser  ← THIS FILE
 *   └── ImportLawnchairStep.kt   — optional Lawnchair-backup import step
 *
 * Purpose:
 *   The first wizard step: ask the user to pick "Simple" (single-layer,
 *   no drawer) or "Classic" (drawer-based). Direct port of the deleted
 *   LayoutModeChooser.java; the same three SharedPreferences writes
 *   happen, with byte-identical values, just lifted out of the AlertDialog
 *   click handler into onComplete(Bundle). The fragment-side UI lives in
 *   foundation.e.bliss.firstrun.ui.LayoutModeStepFragment.
 *
 * Consumed by:
 *   - foundation.e.bliss.firstrun.FirstRunWizard            — listed in ALL_STEPS
 *   - foundation.e.bliss.firstrun.ui.FirstRunActivity       — calls fragment() + onComplete()
 *   - foundation.e.bliss.firstrun.ui.LayoutModeStepFragment — uses RESULT_KEY_MODE / VALUE_*
 *
 * Calls into:
 *   - com.android.launcher3.LauncherPrefs                   — putSync of three bools
 *
 * Plan reference: Plans/Migration04/07-first-run-wizard.md §4
 */
package foundation.e.bliss.firstrun.steps

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.android.launcher3.LauncherPrefs
import foundation.e.bliss.firstrun.FirstRunStep
import foundation.e.bliss.firstrun.ui.LayoutModeStepFragment

/**
 * Single-step wizard equivalent of the deleted LayoutModeChooser dialog.
 *
 * The fragment posts its result back via a Bundle keyed by [RESULT_KEY_MODE] with one of
 * [VALUE_SIMPLE] or [VALUE_CLASSIC]; onComplete writes the three prefs LayoutModeChooser used to
 * write inline.
 */
object LayoutModeStep : FirstRunStep {

    private const val TAG = "LayoutModeStep"

    /** Bundle key the fragment uses to report its choice. */
    const val RESULT_KEY_MODE: String = "mode"

    /** Single-layer / iOS-style mode value. */
    const val VALUE_SIMPLE: String = "simple"

    /** Drawer-based / classic-Android mode value. */
    const val VALUE_CLASSIC: String = "classic"

    override val id: String = "layout_mode_v1"

    override fun fragment(): Fragment = LayoutModeStepFragment()

    override fun onComplete(ctx: Context, result: Bundle) {
        val mode = result.getString(RESULT_KEY_MODE) ?: return
        val singleLayer =
            when (mode) {
                VALUE_SIMPLE -> true
                VALUE_CLASSIC -> false
                else -> {
                    Log.w(TAG, "Ignoring unknown layout-mode result: $mode")
                    return
                }
            }
        try {
            val prefs = LauncherPrefs.get(ctx)
            // Same three writes the deleted LayoutModeChooser performed,
            // in the same order, with the same values. Both modes get
            // gap-preservation by default — picking a layout mode shouldn't
            // also impose iOS-style auto-pack on the user. VerifyIdleAppTask
            // still adds new apps in single-layer mode, but via the
            // end-of-list space finder so user gaps stay intact.
            prefs.putSync(LauncherPrefs.IS_SINGLE_LAYER_ENABLED.to(singleLayer))
            prefs.putSync(LauncherPrefs.PRESERVE_LAYOUT_GAPS.to(true))
            // The legacy single-bit gate is also written so any code path
            // that still reads it directly (rather than via FirstRunWizard)
            // sees the wizard as complete the moment the user picks a mode.
            prefs.putSync(LauncherPrefs.FIRST_RUN_LAYOUT_CHOICE_DONE.to(true))
            Log.i(TAG, "Layout mode chosen: " + if (singleLayer) "single-layer" else "classic")
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to persist layout mode choice", t)
        }
    }
}
