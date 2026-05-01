/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/search/KeyboardAutoHideController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.search)
 * Role:    NEW
 *
 * Tree (settings/controllers/search/):
 *   See SearchProviderController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Switch — hide keyboard on drawer scroll.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.search;

import androidx.annotation.NonNull;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.settings.controllers.BooleanPrefController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Boolean toggle bound to {@link LauncherPrefs#KEYBOARD_AUTO_HIDE}. */
public final class KeyboardAutoHideController extends BooleanPrefController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_KEYBOARD_AUTO_HIDE; }
    @Override protected ConstantItem<Boolean> prefItem() { return LauncherPrefs.KEYBOARD_AUTO_HIDE; }
}
