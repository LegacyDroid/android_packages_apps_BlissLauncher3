/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/home/HomeLabelColorModeController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.home)
 * Role:    NEW
 *
 * Tree (settings/controllers/home/):
 *   See AccentColorController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   List bound to LauncherPrefs.HOME_LABEL_COLOR_MODE. Entries come from
 *   the XML resource arrays — controller just persists the selected value.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.home;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.dagger.LauncherComponentProvider;
import com.android.launcher3.settings.controllers.PreferenceController;

import foundation.e.bliss.preferences.BlissPrefs;

/** ListPref backed by LauncherPrefs.HOME_LABEL_COLOR_MODE; XML provides entries. */
public final class HomeLabelColorModeController implements PreferenceController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_HOME_LABEL_COLOR_MODE; }

    @Override public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        if (!(preference instanceof ListPreference)) return;
        ListPreference lp = (ListPreference) preference;
        LauncherPrefs prefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();
        lp.setValue(prefs.get(LauncherPrefs.HOME_LABEL_COLOR_MODE));
        lp.setOnPreferenceChangeListener((p, v) -> {
            prefs.put(LauncherPrefs.HOME_LABEL_COLOR_MODE, (String) v);
            return true;
        });
    }
}
