/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/advanced/SingleLayerModeController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.advanced)
 * Role:    NEW
 *
 * Tree (settings/controllers/advanced/):
 *   ├── ResetAppRenamesController.java     — clears the app-name override map
 *   ├── RestartLauncherController.java     — kills the launcher process to reload
 *   └── SingleLayerModeController.java     — single-layer (no drawer) mode toggle  ← THIS FILE
 *
 * Purpose:
 *   Switch — single-layer (no app drawer) launcher mode. Reads the current
 *   value from MultiModeController so the UI reflects the runtime state
 *   (which can drift from the SharedPreferences if migration ran on boot).
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.advanced;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.launcher3.settings.controllers.PreferenceController;

import foundation.e.bliss.multimode.MultiModeController;
import foundation.e.bliss.preferences.BlissPrefs;

/** Switch — single-layer (no drawer) launcher mode. */
public final class SingleLayerModeController implements PreferenceController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_SINGLE_LAYER_MODE; }

    @Override public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        if (!(preference instanceof SwitchPreference)) return;
        boolean isSingleLayer = MultiModeController.isSingleLayerMode();
        preference.setDefaultValue(isSingleLayer);
        ((SwitchPreference) preference).setChecked(isSingleLayer);
    }
}
