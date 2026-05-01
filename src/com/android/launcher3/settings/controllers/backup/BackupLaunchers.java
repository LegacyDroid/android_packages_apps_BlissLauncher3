/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/backup/BackupLaunchers.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.backup)
 * Role:    NEW
 *
 * Tree (settings/controllers/backup/):
 *   ├── BackupCreateController.java       — pref_backup_create click → CreateDocument launcher
 *   ├── BackupLaunchers.java              — process-scoped ActivityResultLauncher holders ← THIS FILE
 *   ├── BackupRestoreController.java      — pref_backup_restore click → OpenDocument launcher
 *   └── LawnchairImportController.java    — pref_lawnchair_import click → OpenDocument launcher
 *
 * Purpose:
 *   ActivityResultLaunchers must be registered at fragment-creation time
 *   (not at click time). The hosting BackupFragment registers them and
 *   stashes them here so the click-time controllers (which are stateless
 *   process-scoped singletons) can launch them. Cleared in onDestroyView.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.backup;

import androidx.activity.result.ActivityResultLauncher;

/**
 * Process-scoped holder for the three backup-related
 * {@link ActivityResultLauncher}s that the BackupFragment registers and
 * the corresponding controllers consume on click.
 */
public final class BackupLaunchers {

    private BackupLaunchers() {}

    private static volatile ActivityResultLauncher<String> sBackupLauncher;
    private static volatile ActivityResultLauncher<String[]> sRestoreLauncher;
    private static volatile ActivityResultLauncher<String[]> sLawnchairImportLauncher;

    public static void setBackupLauncher(ActivityResultLauncher<String> l) { sBackupLauncher = l; }
    public static void setRestoreLauncher(ActivityResultLauncher<String[]> l) { sRestoreLauncher = l; }
    public static void setLawnchairImportLauncher(ActivityResultLauncher<String[]> l) {
        sLawnchairImportLauncher = l;
    }

    public static ActivityResultLauncher<String> getBackupLauncher() { return sBackupLauncher; }
    public static ActivityResultLauncher<String[]> getRestoreLauncher() { return sRestoreLauncher; }
    public static ActivityResultLauncher<String[]> getLawnchairImportLauncher() {
        return sLawnchairImportLauncher;
    }

    public static void clear() {
        sBackupLauncher = null;
        sRestoreLauncher = null;
        sLawnchairImportLauncher = null;
    }
}
