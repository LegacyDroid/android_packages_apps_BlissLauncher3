/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/gestures/GestureEdgeLeftController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.gestures)
 * Role:    NEW
 *
 * Tree (settings/controllers/gestures/):
 *   See GestureControllerBase.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Left-edge swipe gesture.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.gestures;

import androidx.annotation.NonNull;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;

import foundation.e.bliss.preferences.BlissPrefs;

/** Left-edge swipe gesture. */
public final class GestureEdgeLeftController extends GestureControllerBase {
    @NonNull @Override public String key() { return BlissPrefs.PREF_GESTURE_EDGE_LEFT; }
    @Override protected ConstantItem<String> prefItem() { return LauncherPrefs.GESTURE_EDGE_LEFT; }
}
