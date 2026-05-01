/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/ui/SettingsRootFragment.java
 * Module:  main source-set  (com.android.launcher3.settings.ui)
 * Role:    NEW
 *
 * Tree (settings/ui/):
 *   ├── SettingsRootFragment.java       — top-level navigation list  ← THIS FILE
 *   ├── HomeScreenFragment.java         — preferences_home.xml
 *   ├── DrawerFragment.java             — preferences_drawer.xml
 *   ├── DockFragment.java               — preferences_dock.xml
 *   ├── FoldersFragment.java            — preferences_folders.xml
 *   ├── SearchFragment.java             — preferences_search.xml
 *   ├── GesturesFragment.java           — preferences_gestures.xml
 *   ├── WidgetsFragment.java            — preferences_widgets.xml
 *   ├── BackupFragment.java             — preferences_backup.xml
 *   └── AdvancedFragment.java           — preferences_advanced.xml
 *
 * Purpose:
 *   The settings entry point — displays launcher_preferences.xml, which is
 *   now a 9-entry navigation list of `<Preference android:fragment="…">`
 *   pointing at the per-area fragments. No controllers attach here; the
 *   nav-screen entries have no associated PreferenceController.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §2, §6
 */
package com.android.launcher3.settings.ui;

import android.os.Bundle;
import android.view.View;

import androidx.preference.PreferenceFragmentCompat;

import com.android.launcher3.LauncherFiles;
import com.android.launcher3.R;

/** Settings root — a 9-entry navigation list. */
public class SettingsRootFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
        setPreferencesFromResource(R.xml.launcher_preferences, rootKey);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setTextDirection(View.TEXT_DIRECTION_LOCALE);
    }
}
