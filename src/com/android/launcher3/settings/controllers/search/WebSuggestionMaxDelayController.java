/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/search/WebSuggestionMaxDelayController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.search)
 * Role:    NEW
 *
 * Tree (settings/controllers/search/):
 *   See SearchProviderController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Seekbar — web suggestion debounce delay (ms).
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.search;

import androidx.annotation.NonNull;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.settings.controllers.IntFactorPrefController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Integer-typed seekbar bound to {@link LauncherPrefs#WEB_SUGGESTION_MAX_DELAY}. */
public final class WebSuggestionMaxDelayController extends IntFactorPrefController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_WEB_SUGGESTION_MAX_DELAY; }
    @Override protected ConstantItem<Integer> prefItem() { return LauncherPrefs.WEB_SUGGESTION_MAX_DELAY; }
    @Override protected int min() { return 0; }
    @Override protected int max() { return 5000; }
}
