/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/src/foundation/e/bliss/firstrun/ui/FirstRunStepFragment.kt
 * Module:  bliss source-set  (foundation.e.bliss.firstrun.ui)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/firstrun/ui/):
 *   ├── FirstRunActivity.kt              — single-Activity host for the wizard
 *   ├── FirstRunStepFragment.kt          — base class for step Fragments  ← THIS FILE
 *   ├── LayoutModeStepFragment.kt        — UI for LayoutModeStep
 *   ├── ImportLawnchairStepFragment.kt   — UI for ImportLawnchairStep
 *   └── activity_first_run.xml           — host layout (single fragment-container slot)
 *
 * Purpose:
 *   Tiny base class every step fragment extends. Owns the `complete(...)`
 *   helper that delegates back to FirstRunActivity to (a) invoke the
 *   step's onComplete with a result Bundle and (b) move on to the next
 *   step. Keeps each step fragment focused on rendering its own UI.
 *
 * Consumed by:
 *   - foundation.e.bliss.firstrun.ui.LayoutModeStepFragment
 *   - foundation.e.bliss.firstrun.ui.ImportLawnchairStepFragment
 *
 * Calls into:
 *   - foundation.e.bliss.firstrun.ui.FirstRunActivity   — completeStep(...)
 *
 * Plan reference: Plans/Migration04/07-first-run-wizard.md §2, §3.1
 */
package foundation.e.bliss.firstrun.ui

import android.os.Bundle
import androidx.fragment.app.Fragment

/**
 * Base for first-run step fragments. The only thing it adds is a convenience method [complete] that
 * hands a result bundle back to the hosting [FirstRunActivity] and triggers the move to the next
 * step.
 *
 * Subclasses focus purely on rendering and event-binding.
 */
abstract class FirstRunStepFragment : Fragment() {

    /**
     * Report completion of this step. The provided [result] is forwarded to the step's
     * [foundation.e.bliss.firstrun.FirstRunStep.onComplete] before the host Activity advances.
     */
    protected fun complete(result: Bundle = Bundle.EMPTY) {
        (activity as? FirstRunActivity)?.completeStep(result)
    }
}
