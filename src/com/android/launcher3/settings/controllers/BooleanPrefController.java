/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/BooleanPrefController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers)
 * Role:    NEW
 *
 * Tree (settings/controllers/):
 *   ├── PreferenceController.java          — interface bound to a single pref key
 *   ├── PreferenceControllerRegistry.java  — exhaustive ALL_CONTROLLERS list
 *   ├── BooleanPrefController.java         — abstract helper for plain SwitchPreference  ← THIS FILE
 *   ├── IntFactorPrefController.java       — abstract helper for SeekBarPreference (int)
 *   ├── StringListPrefController.java      — abstract helper for ListPreference (String)
 *   ├── home/                              — controllers for preferences_home.xml
 *   ├── drawer/                            — controllers for preferences_drawer.xml
 *   ├── dock/                              — controllers for preferences_dock.xml
 *   ├── folders/                           — controllers for preferences_folders.xml
 *   ├── search/                            — controllers for preferences_search.xml
 *   ├── gestures/                          — controllers for preferences_gestures.xml
 *   ├── widgets/                           — controllers for preferences_widgets.xml
 *   ├── backup/                            — controllers for preferences_backup.xml
 *   └── advanced/                          — controllers for preferences_advanced.xml
 *
 * Purpose:
 *   Boilerplate base class for the largest cluster of controllers — those
 *   binding a SwitchPreference to a Boolean LauncherPrefs.ConstantItem.
 *   Subclasses declare key() and prefItem(); attach behaviour is shared.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §3, §7
 */
package com.android.launcher3.settings.controllers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.dagger.LauncherComponentProvider;

/**
 * Base for controllers that bind a {@link SwitchPreference} to a
 * boolean-typed {@link LauncherPrefs} item with no extra logic.
 */
public abstract class BooleanPrefController implements PreferenceController {

    /** The {@link LauncherPrefs} item this controller writes through. */
    protected abstract ConstantItem<Boolean> prefItem();

    @Override
    public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        if (!(preference instanceof SwitchPreference)) return;
        SwitchPreference sp = (SwitchPreference) preference;
        LauncherPrefs prefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();
        ConstantItem<Boolean> item = prefItem();
        sp.setChecked(prefs.get(item));
        sp.setOnPreferenceChangeListener((p, newValue) -> {
            prefs.put(item, (boolean) newValue);
            return true;
        });
    }
}
