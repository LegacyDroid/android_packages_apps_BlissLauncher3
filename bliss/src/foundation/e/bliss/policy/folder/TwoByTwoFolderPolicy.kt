/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/src/foundation/e/bliss/policy/folder/TwoByTwoFolderPolicy.kt
 * Module:  bliss main source-set  (foundation.e.bliss.policy.folder)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/policy/folder/):
 *   ├── ThreeByThreeFolderPolicy.kt    — Bliss default 3×3 grid
 *   └── TwoByTwoFolderPolicy.kt        — Lawnchair-style 2×2 grid (this file)  ← THIS FILE
 *
 * Purpose:
 *   Lawnchair-style 2×2 closed-folder preview policy with 44% icon scale.
 *   Selected by LauncherPolicy.folderPreview() when GRID_FOLDER_PREVIEW is
 *   true (set automatically by the Lawnchair importer). The 0.44f scale
 *   matches Lawnchair's ClippedFolderIconLayoutRule.MIN_SCALE — the scale
 *   Lawnchair applies for 4-item previews.
 *
 * Consumed by:
 *   - foundation.e.bliss.policy.LauncherPolicy                 — folderPreview()
 *
 * Plan reference: Plans/Migration04/05-launcher-policy-strategies.md §4.2
 */
package foundation.e.bliss.policy.folder

import foundation.e.bliss.policy.FolderPreviewPolicy

/**
 * 2×2 / 44% / 4 max — the Lawnchair-style preview that Lawnchair imports configure automatically.
 */
object TwoByTwoFolderPolicy : FolderPreviewPolicy {
    override val gridCountX: Int = 2
    override val gridCountY: Int = 2
    override val itemIconScale: Float = 0.44f
    override val maxNumItemsInPreview: Int = gridCountX * gridCountY
}
