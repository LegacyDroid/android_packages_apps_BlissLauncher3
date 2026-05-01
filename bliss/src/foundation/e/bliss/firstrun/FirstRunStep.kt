/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/src/foundation/e/bliss/firstrun/FirstRunStep.kt
 * Module:  bliss source-set  (foundation.e.bliss.firstrun)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/firstrun/):
 *   ├── FirstRunStep.kt              — interface every wizard step implements  ← THIS FILE
 *   ├── FirstRunStateStore.kt        — Set<String>-backed completion ledger
 *   ├── FirstRunWizard.kt            — orchestrator: ALL_STEPS list, gates, launch()
 *   ├── LawnchairBackupDetector.kt   — scans Documents/Downloads for .lawnchairbackup
 *   ├── steps/                       — concrete steps (LayoutModeStep, ImportLawnchairStep)
 *   └── ui/                          — FirstRunActivity + step-fragment UI
 *
 * Purpose:
 *   The minimal contract a wizard step must satisfy: a stable id used by
 *   FirstRunStateStore to mark it done, a `shouldShow` gate so steps can
 *   self-skip when not applicable (e.g. ImportLawnchairStep when no
 *   backup file is present), a `fragment()` factory the host Activity
 *   inflates, and an `onComplete(Bundle)` hook for the side-effects the
 *   step's UI posts back. Bundle (not a typed sealed result) keeps each
 *   step's payload schema private to itself.
 *
 * Consumed by:
 *   - foundation.e.bliss.firstrun.FirstRunWizard               — owns the ALL_STEPS list
 *   - foundation.e.bliss.firstrun.steps.LayoutModeStep         — implements
 *   - foundation.e.bliss.firstrun.steps.ImportLawnchairStep    — implements
 *   - foundation.e.bliss.firstrun.ui.FirstRunActivity          — calls fragment() + onComplete()
 *
 * Plan reference: Plans/Migration04/07-first-run-wizard.md §3.1
 */
package foundation.e.bliss.firstrun

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment

/**
 * One step in the first-run wizard.
 *
 * Implementations are typically Kotlin `object`s (the steps are stateless; persistence lives in
 * [FirstRunStateStore]).
 */
interface FirstRunStep {

    /** Stable identifier; used to mark the step done in the state store. */
    val id: String

    /**
     * Whether to offer this step to the user. Skipped steps don't count toward "wizard complete".
     * Allows e.g. ImportLawnchairStep to skip itself when no Lawnchair backup is available.
     */
    fun shouldShow(ctx: Context): Boolean = true

    /** Build the fragment that renders this step's UI. */
    fun fragment(): Fragment

    /**
     * Side-effects to run when the user dismisses or completes the step. Receives the `result`
     * payload that the fragment posted (anything the step's UI wants to communicate back).
     */
    fun onComplete(ctx: Context, result: Bundle) {}
}
