/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/drawer/DrawerLeftRightMarginFactorController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.drawer)
 * Role:    NEW
 *
 * Tree (settings/controllers/drawer/):
 *   See DrawerAnimationController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Seekbar — drawer side-margin factor.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.drawer;

import androidx.annotation.NonNull;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.settings.controllers.IntFactorPrefController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Integer-typed seekbar bound to {@link LauncherPrefs#DRAWER_LEFT_RIGHT_MARGIN_FACTOR}. */
public final class DrawerLeftRightMarginFactorController extends IntFactorPrefController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_DRAWER_LEFT_RIGHT_MARGIN_FACTOR; }
    @Override protected ConstantItem<Integer> prefItem() { return LauncherPrefs.DRAWER_LEFT_RIGHT_MARGIN_FACTOR; }
    @Override protected int min() { return 0; }
    @Override protected int max() { return 150; }
}
