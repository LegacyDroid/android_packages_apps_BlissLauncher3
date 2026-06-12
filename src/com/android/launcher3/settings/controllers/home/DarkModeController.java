/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/home/DarkModeController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.home)
 * Role:    NEW
 *
 * Tree (settings/controllers/home/):
 *   See {registry-file} for the directory's full sibling listing.
 *
 * Purpose:
 *   List — system / forced-dark / forced-light theme.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.home;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.dagger.LauncherComponentProvider;
import com.android.launcher3.settings.controllers.PreferenceController;

import foundation.e.bliss.preferences.BlissPrefs;

/** List — system / forced-dark / forced-light theme. */
public final class DarkModeController implements PreferenceController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_DARK_MODE; }

    @Override public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        if (!(preference instanceof ListPreference)) return;
        ListPreference lp = (ListPreference) preference;
        String[] labels = new String[] {
            ctx.getString(R.string.dark_mode_auto),
            ctx.getString(R.string.dark_mode_dark),
            ctx.getString(R.string.dark_mode_light)
        };
        String[] values = new String[] {
            "auto",
            "dark",
            "light"
        };
        lp.setEntries(labels);
        lp.setEntryValues(values);
        LauncherPrefs prefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();
        String current = prefs.get(LauncherPrefs.DARK_MODE);
        lp.setValue(current);
        int idx = lp.findIndexOfValue(current);
        if (idx >= 0) lp.setSummary(labels[idx]);
        lp.setOnPreferenceChangeListener((p, newValue) -> {
            String v = (String) newValue;
            prefs.put(LauncherPrefs.DARK_MODE, v);
            int newIdx = lp.findIndexOfValue(v);
            if (newIdx >= 0) lp.setSummary(labels[newIdx]);
            return true;
        });
    }
}
