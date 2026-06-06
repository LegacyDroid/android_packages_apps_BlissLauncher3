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
 *   Toggling single-layer mode changes the launcher's whole structure — app
 *   drawer vs. no drawer, the QSB first page, the hotseat search field — which
 *   a live model reload (MultiModeController -> forceReload) cannot rebuild;
 *   those views are wired up in Launcher.onCreate(). To make the toggle apply
 *   immediately (instead of leaving a stale home until the user taps the
 *   separate "Restart launcher" action), persist the new value synchronously
 *   and restart the launcher process cleanly — the same mechanism as
 *   {@link RestartLauncherController}.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.advanced;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.dagger.LauncherComponentProvider;
import com.android.launcher3.settings.controllers.PreferenceController;

import foundation.e.bliss.multimode.MultiModeController;
import foundation.e.bliss.preferences.BlissPrefs;

/** Switch — single-layer (no drawer) launcher mode. */
public final class SingleLayerModeController implements PreferenceController {

    /**
     * Delay before the restart so the switch animation completes (visual
     * confirmation) and the synchronous pref write is safely on disk.
     */
    private static final long RESTART_DELAY_MS = 350L;

    @NonNull @Override public String key() { return BlissPrefs.PREF_SINGLE_LAYER_MODE; }

    @Override public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        if (!(preference instanceof SwitchPreference)) return;
        boolean isSingleLayer = MultiModeController.isSingleLayerMode();
        preference.setDefaultValue(isSingleLayer);
        ((SwitchPreference) preference).setChecked(isSingleLayer);

        preference.setOnPreferenceChangeListener((p, newValue) -> {
            final boolean enable = (boolean) newValue;
            // Persist synchronously: the process is about to be killed and an
            // async apply() would not be guaranteed to survive the kill.
            LauncherComponentProvider.get(ctx).getLauncherPrefs()
                    .putSync(LauncherPrefs.IS_SINGLE_LAYER_ENABLED.to(enable));
            // Restart cleanly so the new mode fully applies and the home
            // re-renders without requiring a separate "Restart launcher" tap.
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> Process.killProcess(Process.myPid()), RESTART_DELAY_MS);
            // Return true so the switch reflects the new state during the brief
            // window before the restart.
            return true;
        });
    }
}
