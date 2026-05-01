/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/folders/FolderBadgesController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.folders)
 * Role:    NEW
 *
 * Tree (settings/controllers/folders/):
 *   See FolderBadgesController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Switch — show notification badges on folder previews.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.folders;

import androidx.annotation.NonNull;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.settings.controllers.BooleanPrefController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Boolean toggle bound to {@link LauncherPrefs#FOLDER_BADGES}. */
public final class FolderBadgesController extends BooleanPrefController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_FOLDER_BADGES; }
    @Override protected ConstantItem<Boolean> prefItem() { return LauncherPrefs.FOLDER_BADGES; }
}
