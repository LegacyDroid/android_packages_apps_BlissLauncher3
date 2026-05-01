/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/drawer/DrawerLabelSizeFactorController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.drawer)
 * Role:    NEW
 *
 * Tree (settings/controllers/drawer/):
 *   See DrawerAnimationController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Seekbar — drawer label size factor.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.drawer;

import androidx.annotation.NonNull;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.settings.controllers.IntFactorPrefController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Integer-typed seekbar bound to {@link LauncherPrefs#DRAWER_LABEL_SIZE_FACTOR}. */
public final class DrawerLabelSizeFactorController extends IntFactorPrefController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_DRAWER_LABEL_SIZE_FACTOR; }
    @Override protected ConstantItem<Integer> prefItem() { return LauncherPrefs.DRAWER_LABEL_SIZE_FACTOR; }
    @Override protected int min() { return 50; }
    @Override protected int max() { return 150; }
}
