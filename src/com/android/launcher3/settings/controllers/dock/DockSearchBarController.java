/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/dock/DockSearchBarController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.dock)
 * Role:    NEW
 *
 * Tree (settings/controllers/dock/):
 *   See DockBgColorController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Switch — show search bar in dock.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.dock;

import androidx.annotation.NonNull;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.settings.controllers.BooleanPrefController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Boolean toggle bound to {@link LauncherPrefs#DOCK_SEARCH_BAR}. */
public final class DockSearchBarController extends BooleanPrefController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_DOCK_SEARCH_BAR; }
    @Override protected ConstantItem<Boolean> prefItem() { return LauncherPrefs.DOCK_SEARCH_BAR; }
}
