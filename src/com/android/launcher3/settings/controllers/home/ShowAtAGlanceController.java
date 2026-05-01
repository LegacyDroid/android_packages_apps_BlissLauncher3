/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/home/ShowAtAGlanceController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.home)
 * Role:    NEW
 *
 * Tree (settings/controllers/home/):
 *   See AccentColorController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Switch — show At-a-Glance smartspace widget.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.home;

import androidx.annotation.NonNull;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.settings.controllers.BooleanPrefController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Boolean toggle bound to {@link LauncherPrefs#SHOW_AT_A_GLANCE}. */
public final class ShowAtAGlanceController extends BooleanPrefController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_SHOW_AT_A_GLANCE; }
    @Override protected ConstantItem<Boolean> prefItem() { return LauncherPrefs.SHOW_AT_A_GLANCE; }
}
