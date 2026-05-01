/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/search/SearchProviderController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.search)
 * Role:    NEW
 *
 * Tree (settings/controllers/search/):
 *   See {registry-file} for the directory's full sibling listing.
 *
 * Purpose:
 *   List — web-search provider for in-drawer search.
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

/** List — web-search provider for in-drawer search. */
public final class SearchProviderController implements PreferenceController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_SEARCH_PROVIDER; }

    @Override public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        if (!(preference instanceof ListPreference)) return;
        ListPreference lp = (ListPreference) preference;
        String[] labels = new String[] {
            ctx.getString(R.string.search_provider_default),
            ctx.getString(R.string.search_provider_duckduckgo),
            ctx.getString(R.string.search_provider_qwant),
            ctx.getString(R.string.search_provider_murena),
            ctx.getString(R.string.search_provider_mojeek),
            ctx.getString(R.string.search_provider_spot),
            ctx.getString(R.string.search_provider_ecosia),
            ctx.getString(R.string.search_provider_startpage),
            ctx.getString(R.string.search_provider_brave)
        };
        String[] values = new String[] {
            "default",
            "duckduckgo",
            "qwant",
            "murena",
            "mojeek",
            "spot",
            "ecosia",
            "startpage",
            "brave"
        };
        lp.setEntries(labels);
        lp.setEntryValues(values);
        LauncherPrefs prefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();
        String current = prefs.get(LauncherPrefs.SEARCH_PROVIDER);
        lp.setValue(current);
        int idx = lp.findIndexOfValue(current);
        if (idx >= 0) lp.setSummary(labels[idx]);
        lp.setOnPreferenceChangeListener((p, newValue) -> {
            String v = (String) newValue;
            prefs.put(LauncherPrefs.SEARCH_PROVIDER, v);
            int newIdx = lp.findIndexOfValue(v);
            if (newIdx >= 0) lp.setSummary(labels[newIdx]);
            return true;
        });
    }
}
