/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/dock/DockBgOpacityController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.dock)
 * Role:    NEW
 *
 * Tree (settings/controllers/dock/):
 *   See DockBgColorController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Seekbar — dock background opacity.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.dock;

import androidx.annotation.NonNull;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.settings.controllers.IntFactorPrefController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Integer-typed seekbar bound to {@link LauncherPrefs#DOCK_BG_OPACITY}. */
public final class DockBgOpacityController extends IntFactorPrefController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_DOCK_BG_OPACITY; }
    @Override protected ConstantItem<Integer> prefItem() { return LauncherPrefs.DOCK_BG_OPACITY; }
    @Override protected int min() { return 0; }
    @Override protected int max() { return 100; }
}
