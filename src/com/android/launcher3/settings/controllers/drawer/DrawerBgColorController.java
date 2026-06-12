/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/drawer/DrawerBgColorController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.drawer)
 * Role:    NEW
 *
 * Tree (settings/controllers/drawer/):
 *   See {registry-file} for the directory's full sibling listing.
 *
 * Purpose:
 *   List — drawer background colour preset.
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

/** List — drawer background colour preset. */
public final class DrawerBgColorController implements PreferenceController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_DRAWER_BG_COLOR; }

    @Override public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        if (!(preference instanceof ListPreference)) return;
        ListPreference lp = (ListPreference) preference;
        String[] labels = new String[] {
            ctx.getString(R.string.drawer_bg_default),
            ctx.getString(R.string.drawer_bg_black),
            ctx.getString(R.string.drawer_bg_white),
            ctx.getString(R.string.drawer_bg_dark_gray)
        };
        String[] values = new String[] {
            "default",
            "black",
            "white",
            "dark_gray"
        };
        lp.setEntries(labels);
        lp.setEntryValues(values);
        LauncherPrefs prefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();
        String current = prefs.get(LauncherPrefs.DRAWER_BG_COLOR);
        lp.setValue(current);
        int idx = lp.findIndexOfValue(current);
        if (idx >= 0) lp.setSummary(labels[idx]);
        lp.setOnPreferenceChangeListener((p, newValue) -> {
            String v = (String) newValue;
            prefs.put(LauncherPrefs.DRAWER_BG_COLOR, v);
            int newIdx = lp.findIndexOfValue(v);
            if (newIdx >= 0) lp.setSummary(labels[newIdx]);
            return true;
        });
    }
}
