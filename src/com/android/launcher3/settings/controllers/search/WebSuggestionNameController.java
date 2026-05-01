/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/search/WebSuggestionNameController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.search)
 * Role:    NEW
 *
 * Tree (settings/controllers/search/):
 *   See SearchProviderController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   EditText — display name of the configured web suggestion provider.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.search;

import androidx.annotation.NonNull;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.settings.controllers.EditTextPrefController;

import foundation.e.bliss.preferences.BlissPrefs;

/** EditText — web suggestion provider display name. */
public final class WebSuggestionNameController extends EditTextPrefController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_WEB_SUGGESTION_NAME; }
    @Override protected ConstantItem<String> prefItem() { return LauncherPrefs.WEB_SUGGESTION_NAME; }
}
