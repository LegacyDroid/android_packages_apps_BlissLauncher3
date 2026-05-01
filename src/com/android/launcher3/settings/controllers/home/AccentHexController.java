/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/home/AccentHexController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.home)
 * Role:    NEW
 *
 * Tree (settings/controllers/home/):
 *   See AccentColorController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Hex colour picker for the accent colour (legacy hex mirror of the
 *   structured ACCENT_COLOR pref).
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.home;

import androidx.annotation.NonNull;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.settings.controllers.HexColorPickerController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Hex picker bound to LauncherPrefs.ACCENT_COLOR. */
public final class AccentHexController extends HexColorPickerController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_ACCENT_HEX; }
    @Override protected ConstantItem<String> prefItem() { return LauncherPrefs.ACCENT_COLOR; }
}
