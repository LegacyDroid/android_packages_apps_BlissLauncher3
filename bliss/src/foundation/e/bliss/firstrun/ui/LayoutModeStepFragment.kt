/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/src/foundation/e/bliss/firstrun/ui/LayoutModeStepFragment.kt
 * Module:  bliss source-set  (foundation.e.bliss.firstrun.ui)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/firstrun/ui/):
 *   ├── FirstRunActivity.kt              — single-Activity host for the wizard
 *   ├── FirstRunStepFragment.kt          — base class for step Fragments
 *   ├── LayoutModeStepFragment.kt        — UI for LayoutModeStep  ← THIS FILE
 *   ├── ImportLawnchairStepFragment.kt   — UI for ImportLawnchairStep
 *   └── activity_first_run.xml           — host layout (single fragment-container slot)
 *
 * Purpose:
 *   Direct visual port of the deleted LayoutModeChooser AlertDialog. Same
 *   strings (first_run_layout_*), same two choices, same wording, just
 *   inflated as a fragment so the wizard host Activity can swap it in
 *   place without spawning a Dialog window. Posts back which mode the
 *   user picked via Bundle(LayoutModeStep.RESULT_KEY_MODE = …); the
 *   LayoutModeStep.onComplete side-effects then fire from the Activity.
 *
 * Consumed by:
 *   - foundation.e.bliss.firstrun.steps.LayoutModeStep   — fragment() factory returns this
 *
 * Plan reference: Plans/Migration04/07-first-run-wizard.md §4
 */
package foundation.e.bliss.firstrun.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.android.launcher3.R
import foundation.e.bliss.firstrun.steps.LayoutModeStep

/**
 * Renders the Simple-vs-Classic chooser. Built programmatically (no dedicated layout XML) because
 * the original LayoutModeChooser already composed its body at runtime from the existing string
 * resources, and matching that look is purely a stack of TextViews + two buttons.
 */
class LayoutModeStepFragment : FirstRunStepFragment() {

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

        // Title
        root.addView(
            TextView(ctx).apply {
                setText(R.string.first_run_layout_title)
                setTypeface(typeface, Typeface.BOLD)
                textSize = 22f
                setTextColor(ContextCompat.getColor(ctx, android.R.color.primary_text_dark))
                setTextColor(currentTextColor) // keep theme default; just a no-op assist
            }
        )

        // Spacer
        root.addView(verticalSpacer(ctx, gap))

        // Simple-mode block
        root.addView(
            TextView(ctx).apply {
                setText(R.string.first_run_layout_simple_title)
                setTypeface(typeface, Typeface.BOLD)
                textSize = 17f
            }
        )
        root.addView(
            TextView(ctx).apply {
                setText(R.string.first_run_layout_simple_summary)
                textSize = 14f
                setPadding(0, small, 0, 0)
            }
        )

        // Spacer
        root.addView(verticalSpacer(ctx, gap))

        // Classic-mode block
        root.addView(
            TextView(ctx).apply {
                setText(R.string.first_run_layout_classic_title)
                setTypeface(typeface, Typeface.BOLD)
                textSize = 17f
            }
        )
        root.addView(
            TextView(ctx).apply {
                setText(R.string.first_run_layout_classic_summary)
                textSize = 14f
                setPadding(0, small, 0, 0)
            }
        )

        // Spacer
        root.addView(verticalSpacer(ctx, gap))

        // Trailing message ("you can change this later …")
        root.addView(
            TextView(ctx).apply {
                setText(R.string.first_run_layout_message)
                textSize = 13f
                alpha = 0.75f
            }
        )

        // Spacer before buttons
        root.addView(verticalSpacer(ctx, gap * 2))

        // Buttons row (stacked on phones; side-by-side requires more layout work
        // and we want the dialog-equivalent dead-simple).
        val simpleButton =
            Button(ctx).apply {
                setText(R.string.first_run_layout_choose_simple)
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                setOnClickListener { reportChoice(LayoutModeStep.VALUE_SIMPLE) }
            }
        val classicButton =
            Button(ctx).apply {
                setText(R.string.first_run_layout_choose_classic)
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                setOnClickListener { reportChoice(LayoutModeStep.VALUE_CLASSIC) }
            }
        root.addView(simpleButton)
        root.addView(verticalSpacer(ctx, small))
        root.addView(classicButton)
        root.gravity = Gravity.CENTER_HORIZONTAL

        scroll.addView(root)
        return scroll
    }

    private fun reportChoice(value: String) {
        val result = Bundle().apply { putString(LayoutModeStep.RESULT_KEY_MODE, value) }
        complete(result)
    }

    private fun verticalSpacer(ctx: android.content.Context, height: Int): View =
        View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
        }
}
