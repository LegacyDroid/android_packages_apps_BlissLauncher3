/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/home/GridSizeController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.home)
 * Role:    NEW
 *
 * Tree (settings/controllers/home/):
 *   See AccentColorController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Phone-compatible grid-size list. Reads/writes via
 *   InvariantDeviceProfile.setCurrentGrid which triggers a layout reload —
 *   not a plain LauncherPrefs put.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.home;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.dagger.LauncherComponentProvider;
import com.android.launcher3.settings.controllers.PreferenceController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Phone-compatible workspace grid sizes. */
public final class GridSizeController implements PreferenceController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_GRID_SIZE; }

    @Override public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        if (!(preference instanceof ListPreference)) return;
        ListPreference lp = (ListPreference) preference;
        InvariantDeviceProfile idp = InvariantDeviceProfile.INSTANCE.get(ctx);
        String[] gridNames = {"2_by_2", "3_by_3", "4_by_5", "4_by_6"};
        String[] gridLabels = {
                ctx.getString(R.string.grid_2_by_2),
                ctx.getString(R.string.grid_3_by_3),
                ctx.getString(R.string.grid_4_by_5),
                ctx.getString(R.string.grid_4_by_6)
        };
        lp.setEntries(gridLabels);
        lp.setEntryValues(gridNames);

        LauncherPrefs prefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();
        String currentGridName = prefs.get(LauncherPrefs.GRID_NAME);
        if (currentGridName == null) {
            String match = idp.numColumns + "_by_" + idp.numRows;
            for (String name : gridNames) {
                if (name.equals(match)) { currentGridName = name; break; }
            }
        }
        if (currentGridName != null) {
            lp.setValue(currentGridName);
            int idx = lp.findIndexOfValue(currentGridName);
            if (idx >= 0) lp.setSummary(gridLabels[idx]);
        }
        if (lp.getSummary() == null || lp.getSummary().length() == 0) {
            lp.setSummary(idp.numColumns + " \u00d7 " + idp.numRows);
        }
        lp.setOnPreferenceChangeListener((p, newValue) -> {
            String newGrid = (String) newValue;
            InvariantDeviceProfile.INSTANCE.get(ctx).setCurrentGrid(ctx, newGrid);
            int idx = lp.findIndexOfValue(newGrid);
            if (idx >= 0) lp.setSummary(gridLabels[idx]);
            return true;
        });
    }
}
