/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/backup/LawnchairImportController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.backup)
 * Role:    NEW
 *
 * Tree (settings/controllers/backup/):
 *   ├── BackupCreateController.java       — pref_backup_create click → CreateDocument launcher
 *   ├── BackupLaunchers.java              — process-scoped ActivityResultLauncher holders
 *   ├── BackupRestoreController.java      — pref_backup_restore click → OpenDocument launcher
 *   └── LawnchairImportController.java    — pref_lawnchair_import click → OpenDocument launcher  ← THIS FILE
 *
 * Purpose:
 *   Click handler — launches a SAF OpenDocument with mime "*\/*"; the
 *   BackupFragment-registered callback opens the stream and runs
 *   LawnchairImportHelper.importFromLawnchair on the model executor.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7.2
 */
package com.android.launcher3.settings.controllers.backup;

import android.content.Context;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.launcher3.settings.controllers.PreferenceController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Click — launch the Lawnchair-import file picker. */
public final class LawnchairImportController implements PreferenceController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_LAWNCHAIR_IMPORT; }

    @Override public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        preference.setOnPreferenceClickListener(p -> {
            ActivityResultLauncher<String[]> launcher =
                    BackupLaunchers.getLawnchairImportLauncher();
            if (launcher != null) launcher.launch(new String[] { "*/*" });
            return true;
        });
    }
}
