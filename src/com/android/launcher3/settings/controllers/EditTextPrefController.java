/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/EditTextPrefController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers)
 * Role:    NEW
 *
 * Tree (settings/controllers/):
 *   ├── PreferenceController.java          — interface
 *   ├── PreferenceControllerRegistry.java  — exhaustive list
 *   ├── BooleanPrefController.java         — abstract helper
 *   ├── IntFactorPrefController.java       — abstract helper
 *   ├── StringListPrefController.java      — abstract helper
 *   ├── HexColorPickerController.java      — abstract helper
 *   ├── EditTextPrefController.java        — abstract helper for EditTextPreference  ← THIS FILE
 *   └── home/, drawer/, dock/, folders/, search/, gestures/, widgets/, backup/, advanced/
 *
 * Purpose:
 *   Boilerplate base for EditTextPreference controllers that read/write a
 *   String LauncherPrefs item. Tracks the default summary so an empty
 *   value falls back to the XML-declared summary, matching the
 *   pre-Migration04 behaviour for web-suggestion url/name prefs.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.dagger.LauncherComponentProvider;

/**
 * Base for controllers that bind an {@link EditTextPreference} to a
 * String-typed {@link LauncherPrefs} item.
 */
public abstract class EditTextPrefController implements PreferenceController {

    protected abstract ConstantItem<String> prefItem();

    @Override
    public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        if (!(preference instanceof EditTextPreference)) return;
        final EditTextPreference ep = (EditTextPreference) preference;
        final LauncherPrefs prefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();
        final ConstantItem<String> item = prefItem();
        final String currentValue = prefs.get(item);
        final CharSequence defaultSummary = ep.getSummary();
        ep.setText(currentValue);
        if (currentValue != null && !currentValue.isEmpty()) {
            ep.setSummary(currentValue);
        }
        ep.setOnPreferenceChangeListener((p, newValue) -> {
            String value = newValue == null ? "" : newValue.toString();
            prefs.put(item, value);
            ep.setSummary(value.isEmpty() ? defaultSummary : value);
            return true;
        });
    }
}
