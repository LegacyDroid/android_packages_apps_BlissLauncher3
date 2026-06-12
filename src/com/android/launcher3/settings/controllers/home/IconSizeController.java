/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/home/IconSizeController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.home)
 * Role:    NEW
 *
 * Tree (settings/controllers/home/):
 *   See AccentColorController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Seekbar — home icon size factor. Overrides summaryFor() to display the
 *   current factor as a percentage in the Preference summary, matching the
 *   pre-Migration04 user-facing string.
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

/** Seekbar — home icon size factor. */
public final class IconSizeController extends IntFactorPrefController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_ICON_SIZE_FACTOR; }
    @Override protected ConstantItem<Integer> prefItem() { return LauncherPrefs.ICON_SIZE_FACTOR; }
    @Override protected int min() { return 50; }
    @Override protected int max() { return 150; }
    @Override protected CharSequence summaryFor(int value, @NonNull Context ctx) {
        return ctx.getString(R.string.icon_size_summary, value);
    }
}
