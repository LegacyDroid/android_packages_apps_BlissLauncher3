/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/backup/BackupCreateController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.backup)
 * Role:    NEW
 *
 * Tree (settings/controllers/backup/):
 *   See LawnchairImportController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Click handler — launches a SAF CreateDocument flow with a default
 *   filename of "blisslauncher_backup.zip"; the result-callback (registered
 *   by BackupFragment) writes the actual backup zip via BackupRestoreHelper.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.backup;

import android.content.Context;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.launcher3.settings.controllers.PreferenceController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Click — launch the backup-create file picker. */
public final class BackupCreateController implements PreferenceController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_BACKUP_CREATE; }

    @Override public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        preference.setOnPreferenceClickListener(p -> {
            ActivityResultLauncher<String> launcher = BackupLaunchers.getBackupLauncher();
            if (launcher != null) launcher.launch("blisslauncher_backup.zip");
            return true;
        });
    }
}
