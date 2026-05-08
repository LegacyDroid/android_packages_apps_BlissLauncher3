/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/src/foundation/e/bliss/policy/IdleAppPolicy.kt
 * Module:  bliss main source-set  (foundation.e.bliss.policy)
 *
 * Tree (foundation/e/bliss/policy/):
 *   ├── LauncherPolicy.kt          — singleton accessor
 *   ├── ReorderPolicy.kt           — strategy interface for cell-layout reorder
 *   ├── FolderPreviewPolicy.kt     — strategy interface for folder previews
 *   ├── IdleAppPolicy.kt           — strategy interface (this file)             ← THIS FILE
 *   ├── reorder/                   — ReorderPolicy implementations
 *   ├── folder/                    — FolderPreviewPolicy implementations
 *   └── idle/                      — IdleAppPolicy implementations
 *
 * Purpose:
 *   Strategy describing whether new-app placement should auto-fill empty
 *   workspace cells (default Bliss behaviour) or honour the user's
 *   intentional gaps. The actual decision point is
 *   com.android.launcher3.model.WorkspaceItemSpaceFinder — when this policy
 *   says false, the finder switches from AOSP first-fit to "place new item
 *   AFTER the last occupied cell" so gaps survive a model reload.
 *   VerifyIdleAppTask deliberately does NOT consult this policy: gating it
 *   would block newly installed apps from reaching the home screen entirely
 *   (regression caught during audit — see VerifyIdleAppTask's
 *   touchpoint block).
 *
 * Consumed by:
 *   - foundation.e.bliss.policy.LauncherPolicy                 — idleApp()
 *   - com.android.launcher3.model.WorkspaceItemSpaceFinder     — placement gate
 */
package foundation.e.bliss.policy

/**
 * Decides whether installed apps that are absent from the workspace should be auto-filled into
 * empty cells (the default Bliss behaviour) or left alone so the user's intentional gaps survive.
 */
fun interface IdleAppPolicy {
    /**
     * @return true when [foundation.e.bliss.multimode.VerifyIdleAppTask] (or any future caller)
     *   should auto-fill empty cells with installed apps; false to leave gaps alone.
     */
    fun shouldAutoFillIdle(): Boolean
}
