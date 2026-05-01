/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/backup/BackupRestoreController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.backup)
 * Role:    NEW
 *
 * Tree (settings/controllers/backup/):
 *   See LawnchairImportController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Click handler — launches a SAF OpenDocument restricted to application/zip;
 *   the result-callback (registered by BackupFragment) feeds the stream to
 *   BackupRestoreHelper.restoreFromBackup and triggers a model reload.
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

/** Click — launch the restore-from-backup file picker. */
public final class BackupRestoreController implements PreferenceController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_BACKUP_RESTORE; }

    @Override public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        preference.setOnPreferenceClickListener(p -> {
            ActivityResultLauncher<String[]> launcher = BackupLaunchers.getRestoreLauncher();
            if (launcher != null) launcher.launch(new String[] { "application/zip" });
            return true;
        });
    }
}
