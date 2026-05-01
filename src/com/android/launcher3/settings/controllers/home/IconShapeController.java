/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/home/IconShapeController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.home)
 * Role:    NEW
 *
 * Tree (settings/controllers/home/):
 *   See AccentColorController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   List — icon shape preset. Reads/writes via ThemeManager.PREF_ICON_SHAPE
 *   (a LauncherPrefs item) so the icon-mask path picks up the change.
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
import com.android.launcher3.graphics.ThemeManager;
import com.android.launcher3.settings.controllers.PreferenceController;

import foundation.e.bliss.preferences.BlissPrefs;

/** List — icon-shape preset (system / circle / square / clover / flower / arch / custom). */
public final class IconShapeController implements PreferenceController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_ICON_SHAPE; }

    @Override public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        if (!(preference instanceof ListPreference)) return;
        ListPreference lp = (ListPreference) preference;
        String[] labels = {
                ctx.getString(R.string.icon_shape_system),
                ctx.getString(R.string.icon_shape_circle),
                ctx.getString(R.string.icon_shape_square),
                ctx.getString(R.string.icon_shape_clover),
                ctx.getString(R.string.icon_shape_flower),
                ctx.getString(R.string.icon_shape_arch),
                ctx.getString(R.string.icon_shape_custom),
        };
        String[] values = {
                "", "circle", "square", "four_sided_cookie", "seven_sided_cookie", "arch", "custom"
        };
        lp.setEntries(labels);
        lp.setEntryValues(values);
        LauncherPrefs prefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();
        String current = prefs.get(ThemeManager.PREF_ICON_SHAPE);
        lp.setValue(current);
        int idx = lp.findIndexOfValue(current);
        if (idx >= 0) lp.setSummary(labels[idx]);
        lp.setOnPreferenceChangeListener((p, v) -> {
            String value = (String) v;
            prefs.put(ThemeManager.PREF_ICON_SHAPE, value);
            int newIdx = lp.findIndexOfValue(value);
            if (newIdx >= 0) lp.setSummary(labels[newIdx]);
            return true;
        });
    }
}
