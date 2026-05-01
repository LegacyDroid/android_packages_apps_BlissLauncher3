/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/search/SearchResultCountController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.search)
 * Role:    NEW
 *
 * Tree (settings/controllers/search/):
 *   See SearchProviderController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Seekbar — max results per search.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.search;

import androidx.annotation.NonNull;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.settings.controllers.IntFactorPrefController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Integer-typed seekbar bound to {@link LauncherPrefs#SEARCH_RESULT_COUNT}. */
public final class SearchResultCountController extends IntFactorPrefController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_SEARCH_RESULT_COUNT; }
    @Override protected ConstantItem<Integer> prefItem() { return LauncherPrefs.SEARCH_RESULT_COUNT; }
    @Override protected int min() { return 3; }
    @Override protected int max() { return 20; }
}
