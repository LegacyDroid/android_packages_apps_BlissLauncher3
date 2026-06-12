/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/advanced/RestartLauncherController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.advanced)
 * Role:    NEW
 *
 * Tree (settings/controllers/advanced/):
 *   See ResetAppRenamesController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Click — kill the launcher process so Android restarts it cleanly.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.advanced;

import android.content.Context;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.launcher3.settings.controllers.PreferenceController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Click — Process.killProcess(myPid()) so the launcher restarts. */
public final class RestartLauncherController implements PreferenceController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_RESTART_LAUNCHER; }

    @Override public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        preference.setOnPreferenceClickListener(p -> {
            Process.killProcess(Process.myPid());
            return true;
        });
    }
}
