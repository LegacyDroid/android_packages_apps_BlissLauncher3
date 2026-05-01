/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/PreferenceController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers)
 * Role:    NEW
 *
 * Tree (settings/controllers/):
 *   ├── PreferenceController.java          — interface bound to a single pref key  ← THIS FILE
 *   ├── PreferenceControllerRegistry.java  — exhaustive ALL_CONTROLLERS list + screen filter
 *   ├── home/                              — controllers for preferences_home.xml
 *   ├── drawer/                            — controllers for preferences_drawer.xml
 *   ├── dock/                              — controllers for preferences_dock.xml
 *   ├── folders/                           — controllers for preferences_folders.xml
 *   ├── search/                            — controllers for preferences_search.xml
 *   ├── gestures/                          — controllers for preferences_gestures.xml
 *   ├── widgets/                           — controllers for preferences_widgets.xml
 *   ├── backup/                            — controllers for preferences_backup.xml
 *   └── advanced/                          — controllers for preferences_advanced.xml
 *
 * Purpose:
 *   The single contract every per-pref binding implements. Each controller
 *   owns one pref key (a BlissPrefs.PREF_* constant), wires up the
 *   Preference at attach time, optionally derives a summary, and reacts to
 *   SharedPreferences changes. The fragment is a thin shell that just
 *   filters the registry for keys present in its inflated tree and forwards
 *   lifecycle calls — no `if (key.equals(...))` chains anywhere.
 *
 * Consumed by:
 *   - com.android.launcher3.settings.controllers.PreferenceControllerRegistry
 *   - com.android.launcher3.settings.ui.* (every fragment)
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §3
 */
package com.android.launcher3.settings.controllers;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

/**
 * One pref-key, one controller. The fragment hosts the wiring; the
 * controller hosts the per-pref logic.
 */
public interface PreferenceController {

    /** The pref key this controller binds to. Must be a BlissPrefs.PREF_* constant. */
    @NonNull
    String key();

    /**
     * Called once after the fragment inflates its preference tree. The
     * controller can stash the Preference, set summary providers, attach
     * click/change listeners, etc. Default is no-op.
     */
    default void onAttach(@NonNull Preference preference, @NonNull Context ctx) {}

    /**
     * Called when SharedPreferences fires a change for this key. Default
     * is no-op; controllers that need cross-pref reactions override.
     */
    default void onSharedPrefChanged(@NonNull SharedPreferences sp, @NonNull Context ctx) {}

    /**
     * Optional: contributes a summary string. If non-null, sets the
     * Preference summary at attach time and again on each change.
     */
    @Nullable
    default CharSequence summaryFor(@NonNull SharedPreferences sp, @NonNull Context ctx) {
        return null;
    }
}
