/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/home/DotTextColorController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.home)
 * Role:    NEW
 *
 * Tree (settings/controllers/home/):
 *   See AccentColorController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   List backed by LauncherPrefs.DOT_TEXT_COLOR; entries come from XML
 *   resource arrays.
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

/** ListPref backed by LauncherPrefs.DOT_TEXT_COLOR; XML provides entries. */
public final class DotTextColorController implements PreferenceController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_DOT_TEXT_COLOR; }

    @Override public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        if (!(preference instanceof ListPreference)) return;
        ListPreference lp = (ListPreference) preference;
        LauncherPrefs prefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();
        lp.setValue(prefs.get(LauncherPrefs.DOT_TEXT_COLOR));
        lp.setOnPreferenceChangeListener((p, v) -> {
            prefs.put(LauncherPrefs.DOT_TEXT_COLOR, (String) v);
            return true;
        });
    }
}
