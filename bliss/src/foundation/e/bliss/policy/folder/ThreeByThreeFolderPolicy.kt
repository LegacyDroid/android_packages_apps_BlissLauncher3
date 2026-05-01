/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/src/foundation/e/bliss/policy/folder/ThreeByThreeFolderPolicy.kt
 * Module:  bliss main source-set  (foundation.e.bliss.policy.folder)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/policy/folder/):
 *   ├── ThreeByThreeFolderPolicy.kt    — Bliss default 3×3 grid (this file)    ← THIS FILE
 *   └── TwoByTwoFolderPolicy.kt        — Lawnchair-style 2×2 grid
 *
 * Purpose:
 *   Bliss-default closed-folder preview policy: 3×3 grid with 22% icon
 *   scale. Selected by LauncherPolicy.folderPreview() when
 *   GRID_FOLDER_PREVIEW is false. The numbers here mirror the
 *   `bliss/res/values/config.xml` integers (`grid_folder_icon_rows`,
 *   `grid_folder_icon_columns`, `grid_folder_app_icon_size_percentage`)
 *   that the prior implementation read from resources — kept inline here
 *   so the policy is self-contained for unit testing.
 *
 * Consumed by:
 *   - foundation.e.bliss.policy.LauncherPolicy                 — folderPreview()
 *
 * Plan reference: Plans/Migration04/05-launcher-policy-strategies.md §4.2
 */
package foundation.e.bliss.policy.folder

import foundation.e.bliss.policy.FolderPreviewPolicy

/** 3×3 / 22% — the BlissLauncher default that ships in `bliss/res/values/config.xml`. */
object ThreeByThreeFolderPolicy : FolderPreviewPolicy {
    override val gridCountX: Int = 3
    override val gridCountY: Int = 3
    override val itemIconScale: Float = 0.22f
    override val maxNumItemsInPreview: Int = gridCountX * gridCountY
}
