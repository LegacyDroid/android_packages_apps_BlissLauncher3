/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/home/PageIndicatorHeightFactorController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.home)
 * Role:    NEW
 *
 * Tree (settings/controllers/home/):
 *   See AccentColorController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Seekbar — page-indicator height factor.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.home;

import androidx.annotation.NonNull;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.settings.controllers.IntFactorPrefController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Integer-typed seekbar bound to {@link LauncherPrefs#PAGE_INDICATOR_HEIGHT_FACTOR}. */
public final class PageIndicatorHeightFactorController extends IntFactorPrefController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_PAGE_INDICATOR_HEIGHT_FACTOR; }
    @Override protected ConstantItem<Integer> prefItem() { return LauncherPrefs.PAGE_INDICATOR_HEIGHT_FACTOR; }
    @Override protected int min() { return 0; }
    @Override protected int max() { return 100; }
}
