/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/widgets/ForceWidgetResizeController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.widgets)
 * Role:    NEW
 *
 * Tree (settings/controllers/widgets/):
 *   See WidgetRoundedCornersController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Switch — bypass widget min-size constraints on resize.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.widgets;

import androidx.annotation.NonNull;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.settings.controllers.BooleanPrefController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Boolean toggle bound to {@link LauncherPrefs#FORCE_WIDGET_RESIZE}. */
public final class ForceWidgetResizeController extends BooleanPrefController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_FORCE_WIDGET_RESIZE; }
    @Override protected ConstantItem<Boolean> prefItem() { return LauncherPrefs.FORCE_WIDGET_RESIZE; }
}
