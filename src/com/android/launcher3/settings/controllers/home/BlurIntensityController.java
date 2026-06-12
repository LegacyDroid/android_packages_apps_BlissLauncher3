/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/home/BlurIntensityController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.home)
 * Role:    NEW
 *
 * Tree (settings/controllers/home/):
 *   See AccentColorController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Seekbar — wallpaper-blur intensity.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.home;

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.settings.controllers.IntFactorPrefController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Integer-typed seekbar bound to {@link LauncherPrefs#BLUR_INTENSITY}. */
public final class BlurIntensityController extends IntFactorPrefController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_BLUR_INTENSITY; }
    @Override protected ConstantItem<Integer> prefItem() { return LauncherPrefs.BLUR_INTENSITY; }
    @Override protected int min() { return 0; }
    @Override protected int max() { return 200; }
    @Override protected CharSequence summaryFor(int value, @NonNull Context ctx) {
        return ctx.getString(R.string.blur_intensity_summary, value);
    }
}
