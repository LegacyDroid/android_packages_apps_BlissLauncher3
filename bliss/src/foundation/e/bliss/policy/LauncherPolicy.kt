/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/src/foundation/e/bliss/policy/LauncherPolicy.kt
 * Module:  bliss main source-set  (foundation.e.bliss.policy)
 *
 * Tree (foundation/e/bliss/policy/):
 *   ├── LauncherPolicy.kt          — singleton accessor (this file)            ← THIS FILE
 *   ├── ReorderPolicy.kt           — strategy interface for cell-layout reorder
 *   ├── FolderPreviewPolicy.kt     — strategy interface for closed-folder preview grid
 *   ├── IdleAppPolicy.kt           — strategy interface for VerifyIdleAppTask auto-fill
 *   ├── reorder/                   — ReorderPolicy implementations (Default + PreserveGaps)
 *   ├── folder/                    — FolderPreviewPolicy implementations (3x3 + 2x2)
 *   └── idle/                      — IdleAppPolicy implementations (AutoFill + HonorGaps)
 *
 * Purpose:
 *   Resolves Bliss-side strategy implementations from preferences. Lifts
 *   pref-gated branches out of upstream-aligned AOSP files (ReorderAlgorithm,
 *   ClippedFolderIconLayoutRule, VerifyIdleAppTask) and centralises the
 *   pref-to-policy mapping here so future AOSP rebases stay mechanical.
 *   Each policy is an `object` singleton; pref reads are cheap (in-memory
 *   SharedPreferences lookup) so no caching is required at this layer.
 *
 * Consumed by:
 *   - com.android.launcher3.celllayout.ReorderAlgorithm        — reorder()
 *   - com.android.launcher3.folder.ClippedFolderIconLayoutRule — folderPreview()
 *   - foundation.e.bliss.folder.GridFolderIconLayoutRule       — folderPreview()
 *
 * Calls into:
 *   - com.android.launcher3.LauncherPrefs                      — pref reads
 *
 * Module boundary:
 *   Folder preview and idle policy implementations are candidates for a
 *   policy-core module. LauncherPolicy stays app-owned as long as it reads
 *   LauncherPrefs directly, and reorder policy stays app-owned until a stable
 *   cell-layout data contract replaces CellLayout/ItemConfiguration imports.
 */
package foundation.e.bliss.policy

import android.content.Context
import com.android.launcher3.LauncherPrefs
import foundation.e.bliss.policy.folder.ThreeByThreeFolderPolicy
import foundation.e.bliss.policy.folder.TwoByTwoFolderPolicy
import foundation.e.bliss.policy.idle.AutoFillIdleAppPolicy
import foundation.e.bliss.policy.idle.HonorGapsIdleAppPolicy
import foundation.e.bliss.policy.reorder.DefaultReorderPolicy
import foundation.e.bliss.policy.reorder.PreserveGapsReorderPolicy

/**
 * Resolves Bliss-side strategy implementations from preferences. Each policy is a stateless
 * `object` singleton; the [Context] passed in is used only for the pref lookup and is not retained.
 *
 * Pref changes take effect on next call (no caching here). If a future profile shows pref reads
 * dominating, add a per-Activity cache invalidated by an `OnSharedPreferenceChangeListener`
 * (Migration05+).
 */
object LauncherPolicy {

    @JvmStatic
    fun reorder(ctx: Context): ReorderPolicy =
        if (prefs(ctx).get(LauncherPrefs.PRESERVE_LAYOUT_GAPS)) PreserveGapsReorderPolicy
        else DefaultReorderPolicy

    @JvmStatic
    fun folderPreview(ctx: Context): FolderPreviewPolicy =
        if (prefs(ctx).get(LauncherPrefs.GRID_FOLDER_PREVIEW)) TwoByTwoFolderPolicy
        else ThreeByThreeFolderPolicy

    @JvmStatic
    fun idleApp(ctx: Context): IdleAppPolicy =
        if (prefs(ctx).get(LauncherPrefs.PRESERVE_LAYOUT_GAPS)) HonorGapsIdleAppPolicy
        else AutoFillIdleAppPolicy

    private fun prefs(ctx: Context): LauncherPrefs = LauncherPrefs.get(ctx)
}
