/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/HexColorPickerController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers)
 * Role:    NEW
 *
 * Tree (settings/controllers/):
 *   ├── PreferenceController.java          — interface
 *   ├── PreferenceControllerRegistry.java  — exhaustive list
 *   ├── BooleanPrefController.java         — abstract helper
 *   ├── IntFactorPrefController.java       — abstract helper
 *   ├── StringListPrefController.java      — abstract helper
 *   ├── HexColorPickerController.java      — abstract helper for hex pickers ← THIS FILE
 *   └── home/, drawer/, dock/, folders/, search/, gestures/, widgets/, backup/, advanced/
 *
 * Purpose:
 *   Boilerplate base for the three "tap to pick a hex colour" Preferences
 *   (accent_hex / drawer_bg_hex / dock_bg_hex). Subclasses declare key()
 *   and prefItem(); attach launches BlissColorPickerView in an AlertDialog.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.dagger.LauncherComponentProvider;

import foundation.e.bliss.preferences.colorpicker.BlissColorPickerView;

/** Base for hex-colour picker preferences. */
public abstract class HexColorPickerController implements PreferenceController {

    /** The {@link LauncherPrefs} item this controller writes through. */
    protected abstract ConstantItem<String> prefItem();

    @Override
    public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        preference.setOnPreferenceClickListener(p -> {
            LauncherPrefs prefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();
            ConstantItem<String> item = prefItem();
            String current = prefs.get(item);
            BlissColorPickerView picker = new BlissColorPickerView(ctx, null);
            int initialColor = 0xFF000000;
            if (current != null && current.startsWith("#")) {
                try {
                    initialColor = Color.parseColor(current);
                } catch (Throwable ignored) {
                    // Persisted value pre-dates the current colour format; fall back to default.
                }
            }
            picker.setColor(initialColor);
            int hPad = (int) (16 * ctx.getResources().getDisplayMetrics().density);
            picker.setPadding(hPad, hPad, hPad, 0);
            new AlertDialog.Builder(ctx)
                    .setTitle(preference.getTitle())
                    .setView(picker)
                    .setPositiveButton(android.R.string.ok, (d, w) -> {
                        int color = picker.getColor();
                        String hex = String.format("#%08x", color);
                        prefs.put(item, hex);
                        BlissColorPickerView.rememberRecentColor(ctx, color);
                    })
                    .setNeutralButton(R.string.reset_to_default, (d, w) ->
                            prefs.put(item, "default"))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        });
    }
}
