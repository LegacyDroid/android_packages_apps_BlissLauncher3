/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/search/SearchBarColorController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.search)
 * Role:    NEW
 *
 * Tree (settings/controllers/search/):
 *   See {registry-file} for the directory's full sibling listing.
 *
 * Purpose:
 *   List — search-bar colour preset.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.search;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.dagger.LauncherComponentProvider;
import com.android.launcher3.settings.controllers.PreferenceController;

import foundation.e.bliss.preferences.BlissPrefs;

/** List — search-bar colour preset. */
public final class SearchBarColorController implements PreferenceController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_SEARCH_BAR_COLOR; }

    @Override public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        if (!(preference instanceof ListPreference)) return;
        ListPreference lp = (ListPreference) preference;
        String[] labels = new String[] {
            ctx.getString(R.string.search_bar_color_default),
            ctx.getString(R.string.search_bar_color_black),
            ctx.getString(R.string.search_bar_color_white),
            ctx.getString(R.string.search_bar_color_dark_gray),
            ctx.getString(R.string.search_bar_color_accent)
        };
        String[] values = new String[] {
            "default",
            "black",
            "white",
            "dark_gray",
            "accent"
        };
        lp.setEntries(labels);
        lp.setEntryValues(values);
        LauncherPrefs prefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();
        String current = prefs.get(LauncherPrefs.SEARCH_BAR_COLOR);
        lp.setValue(current);
        int idx = lp.findIndexOfValue(current);
        if (idx >= 0) lp.setSummary(labels[idx]);
        lp.setOnPreferenceChangeListener((p, newValue) -> {
            String v = (String) newValue;
            prefs.put(LauncherPrefs.SEARCH_BAR_COLOR, v);
            int newIdx = lp.findIndexOfValue(v);
            if (newIdx >= 0) lp.setSummary(labels[newIdx]);
            return true;
        });
    }
}
