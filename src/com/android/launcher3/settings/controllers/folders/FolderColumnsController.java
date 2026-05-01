/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/folders/FolderColumnsController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.folders)
 * Role:    NEW
 *
 * Tree (settings/controllers/folders/):
 *   See {registry-file} for the directory's full sibling listing.
 *
 * Purpose:
 *   Folder columns: default | 2..5.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.folders;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.dagger.LauncherComponentProvider;
import com.android.launcher3.settings.controllers.PreferenceController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Folder columns: default | 2..5. */
public final class FolderColumnsController implements PreferenceController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_FOLDER_COLUMNS; }

    @Override public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        if (!(preference instanceof ListPreference)) return;
        ListPreference lp = (ListPreference) preference;
        String[] labels = { ctx.getString(R.string.folder_columns_default), "2 \u00d7 2", "3 \u00d7 3", "4 \u00d7 4", "5 \u00d7 5" };
        String[] values = { "-1", "2", "3", "4", "5" };
        lp.setEntries(labels);
        lp.setEntryValues(values);
        LauncherPrefs prefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();
        int current = prefs.get(LauncherPrefs.FOLDER_COLUMNS);
        lp.setValue(String.valueOf(current));
        int idx = lp.findIndexOfValue(String.valueOf(current));
        if (idx >= 0) lp.setSummary(labels[idx]);
        lp.setOnPreferenceChangeListener((p, v) -> {
            int n = Integer.parseInt((String) v);
            prefs.put(LauncherPrefs.FOLDER_COLUMNS, n);
            int newIdx = lp.findIndexOfValue((String) v);
            if (newIdx >= 0) lp.setSummary(labels[newIdx]);
            return true;
        });
    }
}
