/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/ui/AreaFragmentBase.java
 * Module:  main source-set  (com.android.launcher3.settings.ui)
 * Role:    NEW
 *
 * Tree (settings/ui/):
 *   ├── SettingsRootFragment.java       — top-level navigation list
 *   ├── AreaFragmentBase.java           — shared per-area fragment behaviour  ← THIS FILE
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
 *   All nine area fragments share the same skeleton: inflate one
 *   preferences_<area>.xml, query the registry for controllers whose key
 *   appears in the inflated tree, attach each one, and forward
 *   SharedPreferences change events. Subclasses only declare the XML res-id.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §5
 */
package com.android.launcher3.settings.ui;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.android.launcher3.BuildConfig;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.settings.controllers.PreferenceController;
import com.android.launcher3.settings.controllers.PreferenceControllerRegistry;

import foundation.e.bliss.preferences.BlissPrefSchema;

import java.util.List;

/** Base for the 9 area fragments. Subclasses only override {@link #xmlRes()}. */
public abstract class AreaFragmentBase extends PreferenceFragmentCompat
        implements OnSharedPreferenceChangeListener {

    private List<PreferenceController> mControllers;

    /** Resource id of the {@code preferences_<area>.xml} for this fragment. */
    protected abstract int xmlRes();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
        setPreferencesFromResource(xmlRes(), rootKey);

        if (BuildConfig.DEBUG) {
            BlissPrefSchema.assertAllPrefKeysAreRegistered(getPreferenceScreen());
        }

        Context ctx = requireContext();
        mControllers = PreferenceControllerRegistry.forScreen(getPreferenceScreen());
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        for (PreferenceController c : mControllers) {
            Preference p = findPreference(c.key());
            if (p == null) continue;
            c.onAttach(p, ctx);
            CharSequence s = c.summaryFor(sp, ctx);
            if (s != null) p.setSummary(s);
        }
        sp.registerOnSharedPreferenceChangeListener(this);

        if (getActivity() != null && !TextUtils.isEmpty(getPreferenceScreen().getTitle())) {
            getActivity().setTitle(getPreferenceScreen().getTitle());
        }
    }

    @Override
    public void onSharedPreferenceChanged(@NonNull SharedPreferences sp, String key) {
        if (mControllers == null || key == null) return;
        Context ctx = requireContext();
        for (PreferenceController c : mControllers) {
            if (!c.key().equals(key)) continue;
            c.onSharedPrefChanged(sp, ctx);
            Preference p = findPreference(c.key());
            CharSequence s = c.summaryFor(sp, ctx);
            if (p != null && s != null) p.setSummary(s);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setTextDirection(View.TEXT_DIRECTION_LOCALE);
    }

    @Override
    public void onDestroyView() {
        Context ctx = getContext();
        if (ctx != null) {
            PreferenceManager.getDefaultSharedPreferences(ctx)
                    .unregisterOnSharedPreferenceChangeListener(this);
        }
        super.onDestroyView();
    }
}
