/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/src/foundation/e/bliss/policy/ReorderPolicy.kt
 * Module:  bliss main source-set  (foundation.e.bliss.policy)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/policy/):
 *   ├── LauncherPolicy.kt          — singleton accessor
 *   ├── ReorderPolicy.kt           — strategy interface (this file)             ← THIS FILE
 *   ├── FolderPreviewPolicy.kt     — strategy interface for folder previews
 *   ├── IdleAppPolicy.kt           — strategy interface for idle-app auto-fill
 *   ├── reorder/                   — ReorderPolicy implementations
 *   ├── folder/                    — FolderPreviewPolicy implementations
 *   └── idle/                      — IdleAppPolicy implementations
 *
 * Purpose:
 *   Strategy hook for ReorderAlgorithm.calculateReorder. Lifts the pref-gated
 *   PRESERVE_LAYOUT_GAPS branch out of the upstream AOSP file so future
 *   rebases stay mechanical and the policy is unit-testable in isolation.
 *
 * Consumed by:
 *   - com.android.launcher3.celllayout.ReorderAlgorithm        — calculateReorder
 *
 * Plan reference: Plans/Migration04/05-launcher-policy-strategies.md §4.1
 */
package foundation.e.bliss.policy

import com.android.launcher3.CellLayout
import com.android.launcher3.celllayout.ItemConfiguration
import com.android.launcher3.celllayout.ReorderParameters

/**
 * Hook called by [com.android.launcher3.celllayout.ReorderAlgorithm.calculateReorder] after the
 * AOSP `getDirectionVectorForDrop` call and after the `dropInPlaceSolution` has been computed.
 *
 * Implementations may mutate [cellLayout]'s `mDirectionVector` directly to pre-shape the swap
 * direction for the rest of the AOSP algorithm (the default Bliss behaviour forces `(-1, 0)` to
 * drive iOS-style top-left compaction in single-layer mode). Returning a non-null
 * [ItemConfiguration] short-circuits the rest of the algorithm and returns that solution
 * unconditionally; returning null lets the AOSP algorithm proceed unmodified (with whatever
 * `mDirectionVector` was set to here).
 */
fun interface ReorderPolicy {
    fun overrideDirectionAndPreference(
        cellLayout: CellLayout,
        params: ReorderParameters,
        dropInPlaceSolution: ItemConfiguration,
    ): ItemConfiguration?
}
