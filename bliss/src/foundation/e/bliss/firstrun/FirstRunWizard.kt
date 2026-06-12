/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/src/foundation/e/bliss/firstrun/FirstRunWizard.kt
 * Module:  bliss source-set  (foundation.e.bliss.firstrun)
 *
 * Tree (foundation/e/bliss/firstrun/):
 *   ├── FirstRunStep.kt              — interface every wizard step implements
 *   ├── FirstRunStateStore.kt        — Set<String>-backed completion ledger
 *   ├── FirstRunWizard.kt            — orchestrator: ALL_STEPS list, gates, launch()  ← THIS FILE
 *   ├── LawnchairBackupDetector.kt   — scans Documents/Downloads for .lawnchairbackup
 *   ├── steps/                       — concrete steps (LayoutModeStep, ImportLawnchairStep)
 *   └── ui/                          — FirstRunActivity + step-fragment UI
 *
 * Purpose:
 *   Single entry point the launcher start-up calls. Owns the canonical
 *   ordered list of every wizard step (currently LayoutModeStep,
 *   ImportLawnchairStep), exposes shouldShow() so Launcher.onCreate can
 *   decide whether to launch the host Activity, and provides launch() to
 *   actually start it. Replaces the dialog-based LayoutModeChooser.
 *
 * Consumed by:
 *   - com.android.launcher3.Launcher                        — shouldShow / launch
 *   - foundation.e.bliss.firstrun.FirstRunStateStore        — allStepIds()
 *   - foundation.e.bliss.firstrun.ui.FirstRunActivity       — stepsFor(ctx)
 *
 * Calls into:
 *   - foundation.e.bliss.firstrun.FirstRunStateStore
 *   - foundation.e.bliss.firstrun.steps.LayoutModeStep
 *   - foundation.e.bliss.firstrun.steps.ImportLawnchairStep
 *
 * Module boundary:
 *   The wizard orchestrator may move after first-run state and step contracts
 *   stop reading LauncherPrefs directly. UI fragments stay app-owned while
 *   they depend on app resources and backup import flows.
 */
package foundation.e.bliss.firstrun

import android.content.Context
import android.content.Intent
import foundation.e.bliss.firstrun.steps.ImportLawnchairStep
import foundation.e.bliss.firstrun.steps.LayoutModeStep
import foundation.e.bliss.firstrun.ui.FirstRunActivity

/**
 * Step list + navigation + completion gating.
 *
 * The ordered list of known steps lives here as the single source of truth. Adding a new step is a
 * one-line edit: append the `object` to ALL_STEPS, implement [FirstRunStep], done.
 */
object FirstRunWizard {

    /** Ordered list of all known steps. */
    private val ALL_STEPS: List<FirstRunStep> = listOf(LayoutModeStep, ImportLawnchairStep)

    /** Whether the wizard should run. Called from Launcher.onCreate. */
    @JvmStatic
    fun shouldShow(ctx: Context): Boolean {
        val store = FirstRunStateStore(ctx)
        if (store.isComplete()) return false
        val done = store.completed()
        return ALL_STEPS.any { it.id !in done && it.shouldShow(ctx) }
    }

    /** Launch the FirstRunActivity to walk pending steps. */
    @JvmStatic
    fun launch(ctx: Context) {
        val intent = Intent(ctx, FirstRunActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }

    /** Pending steps for the current launch (skips done + skips self-skipped). */
    @JvmStatic
    fun stepsFor(ctx: Context): List<FirstRunStep> {
        val store = FirstRunStateStore(ctx)
        val done = store.completed()
        return ALL_STEPS.filter { it.id !in done && it.shouldShow(ctx) }
    }

    /** Internal helper for [FirstRunStateStore.markStepDone] to know when "all done" holds. */
    internal fun allStepIds(): Set<String> = ALL_STEPS.map { it.id }.toSet()
}
