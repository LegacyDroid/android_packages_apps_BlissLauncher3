/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/home/InfiniteScrollingController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.home)
 * Role:    NEW
 *
 * Tree (settings/controllers/home/):
 *   See AccentColorController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Switch — wrap workspace at edges (infinite scroll).
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.home;

import androidx.annotation.NonNull;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.settings.controllers.BooleanPrefController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Boolean toggle bound to {@link LauncherPrefs#INFINITE_SCROLLING}. */
public final class InfiniteScrollingController extends BooleanPrefController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_INFINITE_SCROLLING; }
    @Override protected ConstantItem<Boolean> prefItem() { return LauncherPrefs.INFINITE_SCROLLING; }
}
