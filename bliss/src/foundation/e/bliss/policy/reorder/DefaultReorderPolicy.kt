/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/src/foundation/e/bliss/policy/reorder/DefaultReorderPolicy.kt
 * Module:  bliss main source-set  (foundation.e.bliss.policy.reorder)
 *
 * Tree (foundation/e/bliss/policy/reorder/):
 *   ├── DefaultReorderPolicy.kt        — Bliss default (this file)             ← THIS FILE
 *   └── PreserveGapsReorderPolicy.kt   — honours user's intentional empty cells
 *
 * Purpose:
 *   Default Bliss reorder policy: forces the swap direction-vector to
 *   `(-1, 0)` in single-layer mode (or for non-widget items) to drive the
 *   iOS-style top-left compaction that BlissLauncher's home screen has had
 *   since Migration03. Selected by LauncherPolicy.reorder() when
 *   PRESERVE_LAYOUT_GAPS is false (the bliss-on-stock-install default).
 *
 *   Returns null so the rest of the AOSP solution-selection logic runs
 *   unmodified — only the direction-vector is pre-shaped here.
 *
 * Consumed by:
 *   - foundation.e.bliss.policy.LauncherPolicy                 — reorder()
 *
 * Calls into:
 *   - foundation.e.bliss.multimode.MultiModeController         — isSingleLayerMode()
 */
package foundation.e.bliss.policy.reorder

import foundation.e.bliss.multimode.MultiModeController
import foundation.e.bliss.policy.ReorderPolicy

/**
 * Default Bliss policy. Reproduces the historical direction-vector override (`mDirectionVector =
 * (-1, 0)` in single-layer mode or for non- widget cell layouts) and then defers solution selection
 * to the AOSP algorithm by returning null.
 */
val DefaultReorderPolicy = ReorderPolicy { cellLayout, _, _ ->
    if (MultiModeController.isSingleLayerMode || !cellLayout.isWidget) {
        cellLayout.mDirectionVector[0] = -1
        cellLayout.mDirectionVector[1] = 0
    }
    null
}
