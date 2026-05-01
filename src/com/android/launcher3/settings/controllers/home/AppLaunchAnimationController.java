/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/home/AppLaunchAnimationController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.home)
 * Role:    NEW
 *
 * Tree (settings/controllers/home/):
 *   See {registry-file} for the directory's full sibling listing.
 *
 * Purpose:
 *   List — app-launch animation style.
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
import com.android.launcher3.settings.controllers.PreferenceController;

import foundation.e.bliss.preferences.BlissPrefs;

/** List — app-launch animation style. */
public final class AppLaunchAnimationController implements PreferenceController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_APP_LAUNCH_ANIMATION; }

    @Override public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        if (!(preference instanceof ListPreference)) return;
        ListPreference lp = (ListPreference) preference;
        String[] labels = new String[] {
            ctx.getString(R.string.launch_anim_default),
            ctx.getString(R.string.launch_anim_fade),
            ctx.getString(R.string.launch_anim_slide),
            ctx.getString(R.string.launch_anim_none)
        };
        String[] values = new String[] {
            "default",
            "fade",
            "slide_up",
            "none"
        };
        lp.setEntries(labels);
        lp.setEntryValues(values);
        LauncherPrefs prefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();
        String current = prefs.get(LauncherPrefs.APP_LAUNCH_ANIMATION);
        lp.setValue(current);
        int idx = lp.findIndexOfValue(current);
        if (idx >= 0) lp.setSummary(labels[idx]);
        lp.setOnPreferenceChangeListener((p, newValue) -> {
            String v = (String) newValue;
            prefs.put(LauncherPrefs.APP_LAUNCH_ANIMATION, v);
            int newIdx = lp.findIndexOfValue(v);
            if (newIdx >= 0) lp.setSummary(labels[newIdx]);
            return true;
        });
    }
}
