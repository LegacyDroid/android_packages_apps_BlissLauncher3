/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/home/ThemedIconsController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.home)
 * Role:    NEW
 *
 * Tree (settings/controllers/home/):
 *   See AccentColorController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Switch — themed (mono) icons. Routes the toggle through ThemeManager
 *   so the entire icon cache is rebuilt; not a plain LauncherPrefs put.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.home;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.launcher3.graphics.ThemeManager;
import com.android.launcher3.settings.controllers.PreferenceController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Switch — themed (mono) icons; goes through ThemeManager. */
public final class ThemedIconsController implements PreferenceController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_THEMED_ICONS; }

    @Override public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        if (!(preference instanceof SwitchPreference)) return;
        SwitchPreference sp = (SwitchPreference) preference;
        ThemeManager tm = ThemeManager.INSTANCE.get(ctx);
        sp.setChecked(tm.isMonoThemeEnabled());
        sp.setOnPreferenceChangeListener((p, v) -> {
            tm.setMonoThemeEnabled((boolean) v);
            return true;
        });
    }
}
