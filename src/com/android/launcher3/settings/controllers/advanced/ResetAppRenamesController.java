/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/advanced/ResetAppRenamesController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.advanced)
 * Role:    NEW
 *
 * Tree (settings/controllers/advanced/):
 *   ├── ResetAppRenamesController.java     — clears the app-name override map  ← THIS FILE
 *   ├── RestartLauncherController.java     — kills the launcher process to reload
 *   └── SingleLayerModeController.java     — single-layer (no drawer) mode toggle
 *
 * Purpose:
 *   Click — clears the LauncherPrefs.APP_NAME_OVERRIDES JSON map so every
 *   user-renamed app reverts to its launchable label.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.advanced;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.dagger.LauncherComponentProvider;
import com.android.launcher3.settings.controllers.PreferenceController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Click — wipe the JSON map of user-renamed app labels. */
public final class ResetAppRenamesController implements PreferenceController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_RESET_APP_RENAMES; }

    @Override public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        preference.setOnPreferenceClickListener(p -> {
            LauncherPrefs prefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();
            prefs.put(LauncherPrefs.APP_NAME_OVERRIDES, "{}");
            Toast.makeText(ctx, R.string.reset_app_renames_confirm,
                    Toast.LENGTH_SHORT).show();
            return true;
        });
    }
}
