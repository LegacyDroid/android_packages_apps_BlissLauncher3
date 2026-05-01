/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/dock/DockIconCountController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.dock)
 * Role:    NEW
 *
 * Tree (settings/controllers/dock/):
 *   See DockBgColorController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   List — dock icon count override (default | 3..6). Stores int via
 *   LauncherPrefs.DOCK_ICON_COUNT.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.dock;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.dagger.LauncherComponentProvider;
import com.android.launcher3.settings.controllers.PreferenceController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Dock icon count: default | 3 | 4 | 5 | 6 (stored as int). */
public final class DockIconCountController implements PreferenceController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_DOCK_ICON_COUNT; }

    @Override public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        if (!(preference instanceof ListPreference)) return;
        ListPreference lp = (ListPreference) preference;
        String[] labels = { ctx.getString(R.string.dock_icon_count_default),
                "3", "4", "5", "6" };
        String[] values = { "-1", "3", "4", "5", "6" };
        lp.setEntries(labels);
        lp.setEntryValues(values);
        LauncherPrefs prefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();
        int current = prefs.get(LauncherPrefs.DOCK_ICON_COUNT);
        lp.setValue(String.valueOf(current));
        int idx = lp.findIndexOfValue(String.valueOf(current));
        if (idx >= 0) lp.setSummary(labels[idx]);
        else lp.setSummary(labels[0]);
        lp.setOnPreferenceChangeListener((p, v) -> {
            int n = Integer.parseInt((String) v);
            prefs.put(LauncherPrefs.DOCK_ICON_COUNT, n);
            int newIdx = lp.findIndexOfValue((String) v);
            if (newIdx >= 0) lp.setSummary(labels[newIdx]);
            return true;
        });
    }
}
