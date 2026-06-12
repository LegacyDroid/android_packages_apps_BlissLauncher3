/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/home/CustomIconShapeRadiusController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.home)
 * Role:    NEW
 *
 * Tree (settings/controllers/home/):
 *   See AccentColorController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Seekbar — custom-icon-shape corner radius.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.home;

import androidx.annotation.NonNull;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.settings.controllers.IntFactorPrefController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Integer-typed seekbar bound to {@link LauncherPrefs#CUSTOM_ICON_SHAPE_RADIUS}. */
public final class CustomIconShapeRadiusController extends IntFactorPrefController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_CUSTOM_ICON_SHAPE_RADIUS; }
    @Override protected ConstantItem<Integer> prefItem() { return LauncherPrefs.CUSTOM_ICON_SHAPE_RADIUS; }
    @Override protected int min() { return 0; }
    @Override protected int max() { return 50; }
}
