/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/src/foundation/e/bliss/firstrun/FirstRunStateStore.kt
 * Module:  bliss source-set  (foundation.e.bliss.firstrun)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/firstrun/):
 *   ├── FirstRunStep.kt              — interface every wizard step implements
 *   ├── FirstRunStateStore.kt        — Set<String>-backed completion ledger  ← THIS FILE
 *   ├── FirstRunWizard.kt            — orchestrator: ALL_STEPS list, gates, launch()
 *   ├── LawnchairBackupDetector.kt   — scans Documents/Downloads for .lawnchairbackup
 *   ├── steps/                       — concrete steps (LayoutModeStep, ImportLawnchairStep)
 *   └── ui/                          — FirstRunActivity + step-fragment UI
 *
 * Purpose:
 *   The persistence side of the wizard. Tracks which step ids have run
 *   to completion in a single Set<String> pref
 *   (BlissPrefs.PREF_FIRST_RUN_STEPS_DONE), and flips the existing
 *   single-bit gate FIRST_RUN_LAYOUT_CHOICE_DONE to true once every step
 *   in FirstRunWizard.ALL_STEPS has reported done. The legacy gate stays
 *   the on/off switch the rest of the launcher reads — the new ledger is
 *   purely an internal mechanism for the wizard's own bookkeeping.
 *
 * Consumed by:
 *   - foundation.e.bliss.firstrun.FirstRunWizard            — shouldShow / stepsFor
 *   - foundation.e.bliss.firstrun.ui.FirstRunActivity       — markStepDone after each step
 *
 * Calls into:
 *   - com.android.launcher3.LauncherPrefs                   — getter / putSync wrappers
 *
 * Plan reference: Plans/Migration04/07-first-run-wizard.md §3.3
 */
package foundation.e.bliss.firstrun

import android.content.Context
import com.android.launcher3.LauncherPrefs

/**
 * Reads / writes the per-step completion ledger. Cheap to construct; holds no mutable state of its
 * own (everything lives in SharedPreferences).
 */
class FirstRunStateStore(private val ctx: Context) {

    /** True once the wizard's overall gate is set — never re-show. */
    fun isComplete(): Boolean =
        LauncherPrefs.get(ctx).get(LauncherPrefs.FIRST_RUN_LAYOUT_CHOICE_DONE)

    /** Read the set of step ids that have already been completed. */
    fun completed(): Set<String> = LauncherPrefs.get(ctx).get(LauncherPrefs.FIRST_RUN_STEPS_DONE)

    /**
     * Record that [stepId] is done. If this satisfies every step in [FirstRunWizard.ALL_STEPS],
     * also flip the legacy gate so the wizard never re-runs.
     *
     * Synchronous (`putSync`) because callers want to be sure the bit has landed before tearing
     * down the wizard Activity.
     */
    fun markStepDone(stepId: String) {
        val prefs = LauncherPrefs.get(ctx)
        val updated = completed().toMutableSet().also { it += stepId }
        prefs.putSync(LauncherPrefs.FIRST_RUN_STEPS_DONE.to(updated as Set<String>))
        if (updated.containsAll(FirstRunWizard.allStepIds())) {
            prefs.putSync(LauncherPrefs.FIRST_RUN_LAYOUT_CHOICE_DONE.to(true))
        }
    }
}
