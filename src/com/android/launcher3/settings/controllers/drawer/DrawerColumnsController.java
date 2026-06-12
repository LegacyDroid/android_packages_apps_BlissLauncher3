/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/drawer/DrawerColumnsController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.drawer)
 * Role:    NEW
 *
 * Tree (settings/controllers/drawer/):
 *   See {registry-file} for the directory's full sibling listing.
 *
 * Purpose:
 *   Drawer columns: default | 3..8.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.drawer;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.dagger.LauncherComponentProvider;
import com.android.launcher3.settings.controllers.PreferenceController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Drawer columns: default | 3..8. */
public final class DrawerColumnsController implements PreferenceController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_DRAWER_COLUMNS; }

    @Override public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        if (!(preference instanceof ListPreference)) return;
        ListPreference lp = (ListPreference) preference;
        String[] labels = { ctx.getString(R.string.drawer_columns_default), "3", "4", "5", "6", "7", "8" };
        String[] values = { "-1", "3", "4", "5", "6", "7", "8" };
        lp.setEntries(labels);
        lp.setEntryValues(values);
        LauncherPrefs prefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();
        int current = prefs.get(LauncherPrefs.DRAWER_COLUMNS);
        lp.setValue(String.valueOf(current));
        int idx = lp.findIndexOfValue(String.valueOf(current));
        if (idx >= 0) lp.setSummary(labels[idx]);
        lp.setOnPreferenceChangeListener((p, v) -> {
            int n = Integer.parseInt((String) v);
            prefs.put(LauncherPrefs.DRAWER_COLUMNS, n);
            int newIdx = lp.findIndexOfValue((String) v);
            if (newIdx >= 0) lp.setSummary(labels[newIdx]);
            return true;
        });
    }
}
