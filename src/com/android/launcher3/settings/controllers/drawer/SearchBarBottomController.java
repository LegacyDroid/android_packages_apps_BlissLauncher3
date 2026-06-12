/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/drawer/SearchBarBottomController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.drawer)
 * Role:    NEW
 *
 * Tree (settings/controllers/drawer/):
 *   See DrawerAnimationController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Switch — pin search bar to bottom of drawer.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.drawer;

import androidx.annotation.NonNull;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.settings.controllers.BooleanPrefController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Boolean toggle bound to {@link LauncherPrefs#SEARCH_BAR_BOTTOM}. */
public final class SearchBarBottomController extends BooleanPrefController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_SEARCH_BAR_BOTTOM; }
    @Override protected ConstantItem<Boolean> prefItem() { return LauncherPrefs.SEARCH_BAR_BOTTOM; }
}
