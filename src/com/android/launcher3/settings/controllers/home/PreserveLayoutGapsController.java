/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/home/PreserveLayoutGapsController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.home)
 * Role:    NEW
 *
 * Tree (settings/controllers/home/):
 *   ├── AccentColorController.java               — accent colour list
 *   ├── AccentHexController.java                 — accent hex picker
 *   ├── AllowWidgetOverlapController.java        — switch
 *   ├── AppLaunchAnimationController.java        — list
 *   ├── BlurIntensityController.java             — seekbar
 *   ├── CustomIconShapeRadiusController.java     — seekbar
 *   ├── DarkModeController.java                  — list
 *   ├── DarkStatusBarController.java             — switch
 *   ├── DotColorController.java                  — list
 *   ├── DotTextColorController.java              — list
 *   ├── FontFamilyController.java                — list
 *   ├── FontWeightController.java                — list
 *   ├── GridSizeController.java                  — list (custom: idp grid)
 *   ├── HomeLabelColorModeController.java        — list
 *   ├── HomeLabelSizeFactorController.java       — seekbar
 *   ├── IconPackController.java                  — list (queries pm)
 *   ├── IconShapeController.java                 — list (writes ThemeManager)
 *   ├── IconSizeController.java                  — seekbar (with summary)
 *   ├── InfiniteScrollingController.java         — switch
 *   ├── PageIndicatorHeightFactorController.java — seekbar
 *   ├── PageTransitionController.java            — list
 *   ├── PreserveLayoutGapsController.java        — registry-only marker  ← THIS FILE
 *   ├── ShowAtAGlanceController.java             — switch
 *   ├── ShowHomeLabelsController.java            — switch (writes SHOW_HOME_LABELS)
 *   ├── ShowStatusBarController.java             — switch
 *   ├── SmartspaceNowPlayingController.java      — switch
 *   ├── SmartspaceShowDateController.java        — switch
 *   ├── SmartspaceShowTimeController.java        — switch
 *   ├── ThemedIconsController.java               — switch (writes ThemeManager)
 *   ├── Use24hFormatController.java              — switch
 *   ├── WallpaperDepthEffectController.java      — switch
 *   ├── WallpaperScrollingController.java        — switch
 *   ├── WorkspaceBottomPaddingFactorController.java — seekbar
 *   └── WorkspaceTopPaddingFactorController.java — seekbar
 *
 * Purpose:
 *   Marker controller for the "preserve layout gaps" SwitchPreference.
 *   No attach logic — the XML SwitchPreference is android:persistent="true"
 *   and the consumer-side policy (ReorderAlgorithm / VerifyIdleAppTask) reads
 *   the SharedPreferences directly. This controller exists solely so the
 *   registry can recognise the key as a deliberate registration target.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7.1
 */
package com.android.launcher3.settings.controllers.home;

import androidx.annotation.NonNull;

import com.android.launcher3.settings.controllers.PreferenceController;

import foundation.e.bliss.preferences.BlissPrefs;

/** Trivial marker — the SwitchPreference XML and the consumer policy do everything. */
public final class PreserveLayoutGapsController implements PreferenceController {
    @NonNull
    @Override
    public String key() {
        return BlissPrefs.PREF_PRESERVE_LAYOUT_GAPS;
    }
}
