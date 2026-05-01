/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/IntFactorPrefController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers)
 * Role:    NEW
 *
 * Tree (settings/controllers/):
 *   ├── PreferenceController.java          — interface bound to a single pref key
 *   ├── PreferenceControllerRegistry.java  — exhaustive ALL_CONTROLLERS list
 *   ├── BooleanPrefController.java         — abstract helper for plain SwitchPreference
 *   ├── IntFactorPrefController.java       — abstract helper for SeekBarPreference (int) ← THIS FILE
 *   ├── StringListPrefController.java      — abstract helper for ListPreference (String)
 *   ├── home/, drawer/, dock/, folders/, search/, gestures/, widgets/, backup/, advanced/
 *
 * Purpose:
 *   Boilerplate base class for the second-largest controller cluster —
 *   SeekBarPreference (int) bound to an Integer LauncherPrefs item with a
 *   min/max range. Subclasses declare key(), prefItem(), min(), max();
 *   attach behaviour is shared.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §3, §7
 */
package com.android.launcher3.settings.controllers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SeekBarPreference;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.dagger.LauncherComponentProvider;

/**
 * Base for controllers that bind a {@link SeekBarPreference} to an
 * integer-typed {@link LauncherPrefs} item.
 */
public abstract class IntFactorPrefController implements PreferenceController {

    protected abstract ConstantItem<Integer> prefItem();

    protected abstract int min();

    protected abstract int max();

    /** Optional: subclasses override to derive a summary from the current value. */
    protected CharSequence summaryFor(int value, @NonNull Context ctx) {
        return null;
    }

    @Override
    public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        if (!(preference instanceof SeekBarPreference)) return;
        SeekBarPreference sp = (SeekBarPreference) preference;
        LauncherPrefs prefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();
        ConstantItem<Integer> item = prefItem();
        int current = prefs.get(item);
        sp.setMin(min());
        sp.setMax(max());
        sp.setValue(current);
        CharSequence initialSummary = summaryFor(current, ctx);
        if (initialSummary != null) sp.setSummary(initialSummary);
        sp.setOnPreferenceChangeListener((p, newValue) -> {
            int v = (int) newValue;
            prefs.put(item, v);
            CharSequence s = summaryFor(v, ctx);
            if (s != null) sp.setSummary(s);
            return true;
        });
    }
}
