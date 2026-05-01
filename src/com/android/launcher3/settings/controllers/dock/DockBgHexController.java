/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/dock/DockBgHexController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.dock)
 * Role:    NEW
 *
 * Tree (settings/controllers/dock/):
 *   See DockBgColorController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Hex colour picker for the dock background (legacy hex mirror).
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.dock;

import androidx.annotation.NonNull;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.settings.controllers.HexColorPickerController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Hex picker bound to LauncherPrefs.DOCK_BG_COLOR. */
public final class DockBgHexController extends HexColorPickerController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_DOCK_BG_HEX; }
    @Override protected ConstantItem<String> prefItem() { return LauncherPrefs.DOCK_BG_COLOR; }
}
