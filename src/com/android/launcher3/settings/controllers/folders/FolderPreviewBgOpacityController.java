/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/folders/FolderPreviewBgOpacityController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.folders)
 * Role:    NEW
 *
 * Tree (settings/controllers/folders/):
 *   See FolderBadgesController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Seekbar — folder-preview background opacity.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.folders;

import androidx.annotation.NonNull;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.settings.controllers.IntFactorPrefController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Integer-typed seekbar bound to {@link LauncherPrefs#FOLDER_PREVIEW_BG_OPACITY}. */
public final class FolderPreviewBgOpacityController extends IntFactorPrefController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_FOLDER_PREVIEW_BG_OPACITY; }
    @Override protected ConstantItem<Integer> prefItem() { return LauncherPrefs.FOLDER_PREVIEW_BG_OPACITY; }
    @Override protected int min() { return 0; }
    @Override protected int max() { return 100; }
}
