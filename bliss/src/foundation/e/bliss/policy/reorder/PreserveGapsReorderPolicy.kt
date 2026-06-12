/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/src/foundation/e/bliss/policy/reorder/PreserveGapsReorderPolicy.kt
 * Module:  bliss main source-set  (foundation.e.bliss.policy.reorder)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/policy/reorder/):
 *   ├── DefaultReorderPolicy.kt        — Bliss default (forces top-left compaction)
 *   └── PreserveGapsReorderPolicy.kt   — honours user's intentional gaps (this file) ← THIS FILE
 *
 * Purpose:
 *   ReorderPolicy used when PRESERVE_LAYOUT_GAPS is on (set automatically by
 *   the Lawnchair importer). Honours the user's intentional layout in two
 *   ways:
 *
 *     1. Does NOT override the direction-vector. The natural drag direction
 *        produced by the AOSP `getDirectionVectorForDrop` call earlier in
 *        calculateReorder is left intact. (The DefaultReorderPolicy's
 *        `(-1, 0)` clobber would otherwise drive top-left compaction.)
 *
 *     2. Short-circuits with the in-place drop solution whenever one
 *        exists, bypassing the AOSP swap/closest-space tie-breaker that
 *        packs the rest of the workspace toward (-1, 0) and obliterates
 *        the gaps.
 *
 * Consumed by:
 *   - foundation.e.bliss.policy.LauncherPolicy                 — reorder()
 *
 * Plan reference: Plans/Migration04/05-launcher-policy-strategies.md §4.1
 */
package foundation.e.bliss.policy.reorder

import foundation.e.bliss.policy.ReorderPolicy

val PreserveGapsReorderPolicy = ReorderPolicy { _, _, dropInPlaceSolution ->
    if (dropInPlaceSolution.isSolution) dropInPlaceSolution else null
}
