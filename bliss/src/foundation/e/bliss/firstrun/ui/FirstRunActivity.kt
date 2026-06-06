/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/src/foundation/e/bliss/firstrun/ui/FirstRunActivity.kt
 * Module:  bliss source-set  (foundation.e.bliss.firstrun.ui)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/firstrun/ui/):
 *   ├── FirstRunActivity.kt              — single-Activity host for the wizard  ← THIS FILE
 *   ├── FirstRunStepFragment.kt          — base class for step Fragments
 *   ├── LayoutModeStepFragment.kt        — UI for LayoutModeStep
 *   ├── ImportLawnchairStepFragment.kt   — UI for ImportLawnchairStep
 *   └── activity_first_run.xml           — host layout (single fragment-container slot)
 *
 * Purpose:
 *   Single-Activity host that walks FirstRunWizard.stepsFor(this) one
 *   step at a time, replacing the fragment in @id/first_run_container as
 *   each step reports done. After the last pending step completes, the
 *   Activity finishes itself; FirstRunStateStore.markStepDone has already
 *   flipped FIRST_RUN_LAYOUT_CHOICE_DONE so the wizard never re-shows.
 *
 * Consumed by:
 *   - foundation.e.bliss.firstrun.FirstRunWizard            — Intent target of launch()
 *   - foundation.e.bliss.firstrun.ui.FirstRunStepFragment   — completeStep callback
 *
 * Calls into:
 *   - foundation.e.bliss.firstrun.FirstRunStateStore        — markStepDone
 *   - foundation.e.bliss.firstrun.FirstRunWizard            — stepsFor
 *
 * Plan reference: Plans/Migration04/07-first-run-wizard.md §2, §6
 */
package foundation.e.bliss.firstrun.ui

import android.os.Bundle
import android.os.Process
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.android.launcher3.R
import foundation.e.bliss.firstrun.FirstRunStateStore
import foundation.e.bliss.firstrun.FirstRunStep
import foundation.e.bliss.firstrun.FirstRunWizard
import foundation.e.bliss.multimode.MultiModeController

/**
 * Hosts the wizard's step Fragments.
 *
 * One queue snapshot is taken at onCreate (so a step's side-effects can't accidentally re-trigger
 * themselves); the queue is consumed on each completeStep call until empty, at which point the
 * Activity finishes.
 */
class FirstRunActivity : AppCompatActivity() {

    private lateinit var queue: ArrayDeque<FirstRunStep>
    private var current: FirstRunStep? = null

    /**
     * The layout mode the launcher process booted with. Its view hierarchy (app drawer vs. no
     * drawer, QSB first page, hotseat search) is wired up in Launcher.onCreate() and a live pref
     * change cannot rebuild it, so if the user's choice flips this we restart the launcher once the
     * wizard finishes.
     */
    private var bootSingleLayer: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_run)

        // Capture the booted mode before any step writes the pref. Preserve it
        // across config changes so a rotation mid-wizard can't lose it.
        bootSingleLayer =
            if (savedInstanceState?.containsKey(KEY_BOOT_SINGLE_LAYER) == true) {
                savedInstanceState.getBoolean(KEY_BOOT_SINGLE_LAYER)
            } else {
                MultiModeController.isSingleLayerMode
            }

        // Swallow the back press: progressing requires picking an option.
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Intentionally swallow.
                }
            },
        )

        // Snapshot the pending steps once. If the queue is empty (e.g. the
        // launcher start-up gate raced and fired this Activity after a
        // sibling instance already completed), finish immediately.
        queue = ArrayDeque(FirstRunWizard.stepsFor(this))
        if (queue.isEmpty()) {
            finish()
            return
        }
        if (savedInstanceState == null) {
            advance()
        } else {
            // Re-attach `current` from the queue head. The state store
            // is the source of truth for what's already done; if a config
            // change happens mid-step we just keep showing the same step.
            current = queue.firstOrNull()
        }
    }

    /** Called by [FirstRunStepFragment] when the user finishes a step. */
    fun completeStep(result: Bundle) {
        val step = current ?: return
        try {
            step.onComplete(applicationContext, result)
        } finally {
            FirstRunStateStore(applicationContext).markStepDone(step.id)
        }
        // Pop the just-completed step and advance to the next one.
        if (queue.isNotEmpty() && queue.first().id == step.id) {
            queue.removeFirst()
        }
        advance()
    }

    /**
     * Show the next pending step, or finish the Activity if the queue is drained. Steps that became
     * inapplicable mid-flow (shouldShow flips to false) are silently skipped.
     */
    private fun advance() {
        while (queue.isNotEmpty()) {
            val next = queue.first()
            if (!next.shouldShow(this)) {
                queue.removeFirst()
                FirstRunStateStore(applicationContext).markStepDone(next.id)
                continue
            }
            current = next
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.first_run_container, next.fragment())
                .commitNow()
            return
        }
        current = null
        finishWizard()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_BOOT_SINGLE_LAYER, bootSingleLayer)
    }

    /**
     * Close the wizard. If the user's layout choice changed the mode the launcher is currently
     * running, restart the process cleanly so the new structure (drawer/home layout) fully applies
     * — otherwise the home is left empty or stale until a manual restart. Every wizard pref was
     * written synchronously (putSync) before we reach here, so the new value is already on disk and
     * the relaunched launcher reads it.
     */
    private fun finishWizard() {
        val modeChanged = MultiModeController.isSingleLayerMode != bootSingleLayer
        finish()
        if (modeChanged) {
            Process.killProcess(Process.myPid())
        }
    }

    companion object {
        private const val KEY_BOOT_SINGLE_LAYER = "boot_single_layer"
    }
}
