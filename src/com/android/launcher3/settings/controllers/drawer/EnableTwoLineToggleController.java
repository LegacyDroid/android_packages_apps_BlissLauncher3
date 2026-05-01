/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/drawer/EnableTwoLineToggleController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.drawer)
 * Role:    NEW
 *
 * Tree (settings/controllers/drawer/):
 *   See DrawerAnimationController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Switch — allow two-line drawer labels.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.drawer;

import androidx.annotation.NonNull;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.settings.controllers.BooleanPrefController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Boolean toggle bound to {@link LauncherPrefs#ENABLE_TWOLINE_ALLAPPS_TOGGLE}. */
public final class EnableTwoLineToggleController extends BooleanPrefController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_ENABLE_TWO_LINE_TOGGLE; }
    @Override protected ConstantItem<Boolean> prefItem() { return LauncherPrefs.ENABLE_TWOLINE_ALLAPPS_TOGGLE; }
}
