/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/src/foundation/e/bliss/policy/FolderPreviewPolicy.kt
 * Module:  bliss main source-set  (foundation.e.bliss.policy)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/policy/):
 *   ├── LauncherPolicy.kt          — singleton accessor
 *   ├── ReorderPolicy.kt           — strategy interface for cell-layout reorder
 *   ├── FolderPreviewPolicy.kt     — strategy interface (this file)             ← THIS FILE
 *   ├── IdleAppPolicy.kt           — strategy interface for idle-app auto-fill
 *   ├── reorder/                   — ReorderPolicy implementations
 *   ├── folder/                    — FolderPreviewPolicy implementations
 *   └── idle/                      — IdleAppPolicy implementations
 *
 * Purpose:
 *   Strategy describing how a closed folder icon's preview grid is laid out
 *   (3×3 default vs Lawnchair-style 2×2). Lifts the GRID_FOLDER_PREVIEW
 *   pref-gated branches out of ClippedFolderIconLayoutRule and
 *   GridFolderIconLayoutRule.
 *
 * Consumed by:
 *   - com.android.launcher3.folder.ClippedFolderIconLayoutRule — preview-item layout
 *   - foundation.e.bliss.folder.GridFolderIconLayoutRule       — grid sizing
 *
 * Plan reference: Plans/Migration04/05-launcher-policy-strategies.md §4.2
 */
package foundation.e.bliss.policy

/**
 * Constants describing a closed folder's preview grid. All implementations are `object` singletons
 * and contain no per-instance state.
 */
interface FolderPreviewPolicy {
    /** Number of preview-item columns (3 for default, 2 for Lawnchair-style). */
    val gridCountX: Int

    /** Number of preview-item rows. */
    val gridCountY: Int

    /** Per-item icon scale relative to the baseline icon size (e.g. 0.22f or 0.44f). */
    val itemIconScale: Float

    /** Maximum number of items rendered in the preview grid (gridCountX * gridCountY). */
    val maxNumItemsInPreview: Int
}
