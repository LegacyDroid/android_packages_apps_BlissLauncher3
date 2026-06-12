/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/gestures/GestureDoubleTapController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.gestures)
 * Role:    NEW
 *
 * Tree (settings/controllers/gestures/):
 *   See GestureControllerBase.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Double-tap on workspace.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.gestures;

import androidx.annotation.NonNull;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;

import foundation.e.bliss.preferences.BlissPrefs;

/** Double-tap on workspace. */
public final class GestureDoubleTapController extends GestureControllerBase {
    @NonNull @Override public String key() { return BlissPrefs.PREF_GESTURE_DOUBLE_TAP; }
    @Override protected ConstantItem<String> prefItem() { return LauncherPrefs.GESTURE_DOUBLE_TAP; }
}
