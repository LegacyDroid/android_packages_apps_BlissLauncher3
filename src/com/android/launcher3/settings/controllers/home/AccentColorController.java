/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/home/AccentColorController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.home)
 * Role:    NEW
 *
 * Tree (settings/controllers/home/):
 *   See {registry-file} for the directory's full sibling listing.
 *
 * Purpose:
 *   List — accent colour preset.
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

/** List — accent colour preset. */
public final class AccentColorController implements PreferenceController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_ACCENT_COLOR; }

    @Override public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        if (!(preference instanceof ListPreference)) return;
        ListPreference lp = (ListPreference) preference;
        String[] labels = new String[] {
            ctx.getString(R.string.accent_system),
            ctx.getString(R.string.accent_blue),
            ctx.getString(R.string.accent_green),
            ctx.getString(R.string.accent_red),
            ctx.getString(R.string.accent_purple),
            ctx.getString(R.string.accent_orange),
            ctx.getString(R.string.accent_pink),
            ctx.getString(R.string.accent_teal)
        };
        String[] values = new String[] {
            "system",
            "blue",
            "green",
            "red",
            "purple",
            "orange",
            "pink",
            "teal"
        };
        lp.setEntries(labels);
        lp.setEntryValues(values);
        LauncherPrefs prefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();
        String current = prefs.get(LauncherPrefs.ACCENT_COLOR);
        lp.setValue(current);
        int idx = lp.findIndexOfValue(current);
        if (idx >= 0) lp.setSummary(labels[idx]);
        lp.setOnPreferenceChangeListener((p, newValue) -> {
            String v = (String) newValue;
            prefs.put(LauncherPrefs.ACCENT_COLOR, v);
            int newIdx = lp.findIndexOfValue(v);
            if (newIdx >= 0) lp.setSummary(labels[newIdx]);
            return true;
        });
    }
}
