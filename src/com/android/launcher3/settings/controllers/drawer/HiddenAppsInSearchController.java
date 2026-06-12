/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/drawer/HiddenAppsInSearchController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.drawer)
 * Role:    NEW
 *
 * Tree (settings/controllers/drawer/):
 *   See DrawerAnimationController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   List bound to LauncherPrefs.HIDDEN_APPS_IN_SEARCH; XML provides entries.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.drawer;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.dagger.LauncherComponentProvider;
import com.android.launcher3.settings.controllers.PreferenceController;

import foundation.e.bliss.preferences.BlissPrefs;

/** ListPref bound to LauncherPrefs.HIDDEN_APPS_IN_SEARCH; XML provides entries. */
public final class HiddenAppsInSearchController implements PreferenceController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_HIDDEN_APPS_IN_SEARCH; }

    @Override public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        if (!(preference instanceof ListPreference)) return;
        ListPreference lp = (ListPreference) preference;
        LauncherPrefs prefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();
        lp.setValue(prefs.get(LauncherPrefs.HIDDEN_APPS_IN_SEARCH));
        lp.setOnPreferenceChangeListener((p, v) -> {
            prefs.put(LauncherPrefs.HIDDEN_APPS_IN_SEARCH, (String) v);
            return true;
        });
    }
}
