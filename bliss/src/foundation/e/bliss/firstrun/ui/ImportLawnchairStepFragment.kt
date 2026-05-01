/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/src/foundation/e/bliss/firstrun/ui/ImportLawnchairStepFragment.kt
 * Module:  bliss source-set  (foundation.e.bliss.firstrun.ui)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/firstrun/ui/):
 *   ├── FirstRunActivity.kt              — single-Activity host for the wizard
 *   ├── FirstRunStepFragment.kt          — base class for step Fragments
 *   ├── LayoutModeStepFragment.kt        — UI for LayoutModeStep
 *   ├── ImportLawnchairStepFragment.kt   — UI for ImportLawnchairStep  ← THIS FILE
 *   └── activity_first_run.xml           — host layout (single fragment-container slot)
 *
 * Purpose:
 *   The "do you want to import your Lawnchair backup?" UI. Two buttons:
 *   Import (runs LawnchairImportHelper.importFromLawnchair off the main
 *   thread on the file the detector found, then toasts the outcome) and
 *   Skip (no-op besides marking the step done). Either way the step
 *   completes and the wizard advances.
 *
 * Consumed by:
 *   - foundation.e.bliss.firstrun.steps.ImportLawnchairStep  — fragment() factory returns this
 *
 * Calls into:
 *   - foundation.e.bliss.backup.LawnchairImportHelper        — importFromLawnchair
 *   - foundation.e.bliss.firstrun.LawnchairBackupDetector    — pulls path back at runtime
 *
 * Plan reference: Plans/Migration04/07-first-run-wizard.md §5
 */
package foundation.e.bliss.firstrun.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.android.launcher3.R
import foundation.e.bliss.backup.ImportResult
import foundation.e.bliss.backup.LawnchairImportHelper
import foundation.e.bliss.firstrun.LawnchairBackupDetector
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Two-button UI: Import / Skip. Both routes complete the step.
 *
 * Import disables the buttons, runs the importer on a worker thread, and posts the toast back to
 * the main thread before reporting completion. If the detector can't re-resolve the file at
 * click-time (e.g. the user deleted it between launch and tap), we toast a failure and complete
 * anyway — the wizard never gets stuck.
 */
class ImportLawnchairStepFragment : FirstRunStepFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val ctx = requireContext()
        val pad = (24 * resources.displayMetrics.density).toInt()
        val gap = (16 * resources.displayMetrics.density).toInt()
        val small = (8 * resources.displayMetrics.density).toInt()

        val scroll =
            ScrollView(ctx).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                isFillViewport = true
                setPadding(pad, pad, pad, pad)
            }

        val root =
            LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
            }

        root.addView(
            TextView(ctx).apply {
                setText(R.string.first_run_import_lawnchair_title)
                setTypeface(typeface, Typeface.BOLD)
                textSize = 22f
            }
        )
        root.addView(verticalSpacer(ctx, gap))
        root.addView(
            TextView(ctx).apply {
                setText(R.string.first_run_import_lawnchair_summary)
                textSize = 14f
            }
        )
        root.addView(verticalSpacer(ctx, gap * 2))

        val importBtn =
            Button(ctx).apply {
                setText(R.string.first_run_import_lawnchair_action_import)
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
            }
        val skipBtn =
            Button(ctx).apply {
                setText(R.string.first_run_import_lawnchair_action_skip)
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
            }

        importBtn.setOnClickListener {
            importBtn.isEnabled = false
            skipBtn.isEnabled = false
            runImport()
        }
        skipBtn.setOnClickListener { complete() }

        root.addView(importBtn)
        root.addView(verticalSpacer(ctx, small))
        root.addView(skipBtn)

        scroll.addView(root)
        return scroll
    }

    /**
     * Runs the importer on a background thread, toasts the result, and reports step completion. The
     * file path is re-resolved at click time so a backup deleted between gate and tap is reported
     * as failure rather than crashing.
     */
    private fun runImport() {
        val appCtx = requireContext().applicationContext
        val path = LawnchairBackupDetector.detect(appCtx)
        if (path == null) {
            toastAndComplete(getString(R.string.first_run_import_lawnchair_toast_no_file))
            return
        }
        Thread(
                {
                    val result: ImportResult =
                        try {
                            FileInputStream(File(path)).use { stream: InputStream ->
                                LawnchairImportHelper.importFromLawnchair(appCtx, stream)
                            }
                        } catch (t: Throwable) {
                            ImportResult.failure(t.message ?: "import failed")
                        }
                    val msg =
                        if (result.success) {
                            getString(R.string.first_run_import_lawnchair_toast_success)
                        } else {
                            getString(
                                R.string.first_run_import_lawnchair_toast_failure,
                                result.message ?: "",
                            )
                        }
                    view?.post { toastAndComplete(msg) }
                },
                "first-run-lawnchair-import",
            )
            .start()
    }

    private fun toastAndComplete(message: String) {
        if (!isAdded) return
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        complete()
    }

    private fun verticalSpacer(ctx: android.content.Context, height: Int): View =
        View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
        }
}
