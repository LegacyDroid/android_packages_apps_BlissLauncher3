/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/StringListPrefController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers)
 * Role:    NEW
 *
 * Tree (settings/controllers/):
 *   ├── PreferenceController.java          — interface bound to a single pref key
 *   ├── PreferenceControllerRegistry.java  — exhaustive ALL_CONTROLLERS list
 *   ├── BooleanPrefController.java         — abstract helper for plain SwitchPreference
 *   ├── IntFactorPrefController.java       — abstract helper for SeekBarPreference (int)
 *   ├── StringListPrefController.java      — abstract helper for ListPreference (String) ← THIS FILE
 *   ├── home/, drawer/, dock/, folders/, search/, gestures/, widgets/, backup/, advanced/
 *
 * Purpose:
 *   Boilerplate base class for ListPreference controllers that map a fixed
 *   set of label/value pairs to a String LauncherPrefs item. Subclasses
 *   provide entries(), entryValues(), and prefItem(); attach behaviour is
 *   shared. Summary tracks the selected entry's label.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §3, §7
 */
package com.android.launcher3.settings.controllers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.dagger.LauncherComponentProvider;

/**
 * Base for controllers that bind a {@link ListPreference} to a
 * string-typed {@link LauncherPrefs} item with a fixed entry table.
 */
public abstract class StringListPrefController implements PreferenceController {

    protected abstract ConstantItem<String> prefItem();

    protected abstract CharSequence[] entries(@NonNull Context ctx);

    protected abstract CharSequence[] entryValues(@NonNull Context ctx);

    @Override
    public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        if (!(preference instanceof ListPreference)) return;
        ListPreference lp = (ListPreference) preference;
        CharSequence[] labels = entries(ctx);
        CharSequence[] values = entryValues(ctx);
        lp.setEntries(labels);
        lp.setEntryValues(values);
        LauncherPrefs prefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();
        ConstantItem<String> item = prefItem();
        String current = prefs.get(item);
        lp.setValue(current);
        int idx = lp.findIndexOfValue(current);
        if (idx >= 0) lp.setSummary(labels[idx]);
        lp.setOnPreferenceChangeListener((p, newValue) -> {
            String v = (String) newValue;
            prefs.put(item, v);
            int newIdx = lp.findIndexOfValue(v);
            if (newIdx >= 0) lp.setSummary(labels[newIdx]);
            return true;
        });
    }
}
