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
import androidx.appcompat.app.AppCompatActivity
import com.android.launcher3.R
import foundation.e.bliss.firstrun.FirstRunStateStore
import foundation.e.bliss.firstrun.FirstRunStep
import foundation.e.bliss.firstrun.FirstRunWizard

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_run)

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
        finish()
    }

    /** No-op: the wizard is one-shot; back press shouldn't dismiss it mid-flow. */
    @Deprecated("AOSP back-press shim retained for safety; intentional no-op.")
    override fun onBackPressed() {
        // Intentionally swallow: progressing requires picking an option.
    }
}
