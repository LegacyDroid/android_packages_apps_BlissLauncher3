/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/src/foundation/e/bliss/firstrun/steps/ImportLawnchairStep.kt
 * Module:  bliss source-set  (foundation.e.bliss.firstrun.steps)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/firstrun/steps/):
 *   ├── LayoutModeStep.kt        — Simple-vs-Classic chooser, ported from LayoutModeChooser
 *   └── ImportLawnchairStep.kt   — optional Lawnchair-backup import step  ← THIS FILE
 *
 * Purpose:
 *   The second wizard step. shouldShow() returns true only when
 *   LawnchairBackupDetector finds at least one `.lawnchairbackup` file in
 *   the user's public storage; otherwise the step self-skips and the
 *   wizard treats it as not-applicable (without blocking completion).
 *   When the user picks Import, the fragment runs
 *   LawnchairImportHelper.importFromLawnchair off the main thread and
 *   surfaces a Toast; Skip is a no-op besides marking the step done.
 *
 * Consumed by:
 *   - foundation.e.bliss.firstrun.FirstRunWizard            — listed in ALL_STEPS
 *   - foundation.e.bliss.firstrun.ui.FirstRunActivity       — calls fragment() + onComplete()
 *   - foundation.e.bliss.firstrun.ui.ImportLawnchairStepFragment — pulls path from detector
 *
 * Calls into:
 *   - foundation.e.bliss.firstrun.LawnchairBackupDetector
 *
 * Plan reference: Plans/Migration04/07-first-run-wizard.md §5
 */
package foundation.e.bliss.firstrun.steps

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import foundation.e.bliss.firstrun.FirstRunStep
import foundation.e.bliss.firstrun.LawnchairBackupDetector
import foundation.e.bliss.firstrun.ui.ImportLawnchairStepFragment

/**
 * Optional first-run step: offer to import a Lawnchair backup if one is lying in the user's public
 * storage.
 *
 * This step has no pref-write side effects of its own; the fragment runs the importer directly and
 * toasts the result. onComplete is a no-op because step-completion is reported by the host
 * Activity, not by us.
 */
object ImportLawnchairStep : FirstRunStep {

    override val id: String = "import_lawnchair_v1"

    override fun shouldShow(ctx: Context): Boolean = LawnchairBackupDetector.detect(ctx) != null

    override fun fragment(): Fragment = ImportLawnchairStepFragment()

    override fun onComplete(ctx: Context, result: Bundle) {
        // Intentionally empty: the import side-effects ran inside the
        // fragment (off the main thread). Nothing more to persist here.
    }
}
