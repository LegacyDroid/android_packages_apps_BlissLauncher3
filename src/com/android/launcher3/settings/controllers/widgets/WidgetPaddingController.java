/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/widgets/WidgetPaddingController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.widgets)
 * Role:    NEW
 *
 * Tree (settings/controllers/widgets/):
 *   See WidgetRoundedCornersController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Seekbar — widget internal padding.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.widgets;

import androidx.annotation.NonNull;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.settings.controllers.IntFactorPrefController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Integer-typed seekbar bound to {@link LauncherPrefs#WIDGET_PADDING}. */
public final class WidgetPaddingController extends IntFactorPrefController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_WIDGET_PADDING; }
    @Override protected ConstantItem<Integer> prefItem() { return LauncherPrefs.WIDGET_PADDING; }
    @Override protected int min() { return 0; }
    @Override protected int max() { return 32; }
}
