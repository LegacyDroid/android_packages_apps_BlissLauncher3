/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/PreferenceControllerRegistry.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers)
 * Role:    NEW
 *
 * Tree (settings/controllers/):
 *   ├── PreferenceController.java          — interface bound to a single pref key
 *   ├── PreferenceControllerRegistry.java  — exhaustive ALL_CONTROLLERS list  ← THIS FILE
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
 *   The exhaustive enumeration of every PreferenceController in the build.
 *   Build-time visibility (no annotation scanning, no reflection) is what
 *   keeps R8 happy and makes "did I forget to register the new controller?"
 *   a one-line diff to inspect. forScreen() filters this list down to the
 *   controllers whose key appears in the fragment's inflated PreferenceScreen.
 *
 * Consumed by:
 *   - com.android.launcher3.settings.ui.* (every fragment) — forScreen() lookup
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §4
 */
package com.android.launcher3.settings.controllers;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.launcher3.settings.controllers.advanced.*;
import com.android.launcher3.settings.controllers.backup.*;
import com.android.launcher3.settings.controllers.dock.*;
import com.android.launcher3.settings.controllers.drawer.*;
import com.android.launcher3.settings.controllers.folders.*;
import com.android.launcher3.settings.controllers.gestures.*;
import com.android.launcher3.settings.controllers.home.*;
import com.android.launcher3.settings.controllers.search.*;
import com.android.launcher3.settings.controllers.widgets.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Build-time enumeration of every {@link PreferenceController} in the
 * launcher. Per Plan §4: "no auto-discovery; the list is enumerated
 * exhaustively". {@link #forScreen} filters the list down to the
 * controllers whose key appears in the fragment's inflated tree.
 */
public final class PreferenceControllerRegistry {

    private PreferenceControllerRegistry() {}

    /**
     * All controllers registered in the build. Add a controller here when
     * you add a new pref-key to a fragment XML; remove it when you delete
     * the key. The order is purely cosmetic — controllers are filtered
     * by key in {@link #forScreen}.
     */
    private static final List<PreferenceController> ALL_CONTROLLERS = Collections.unmodifiableList(
            buildAllControllers());

    private static List<PreferenceController> buildAllControllers() {
        List<PreferenceController> list = new ArrayList<>();
        // Home screen + general appearance + smartspace + fonts
        list.add(new GridSizeController());
        list.add(new IconSizeController());
        list.add(new ShowHomeLabelsController());
        list.add(new HomeLabelSizeFactorController());
        list.add(new HomeLabelColorModeController());
        list.add(new PreserveLayoutGapsController());
        list.add(new WorkspaceTopPaddingFactorController());
        list.add(new WorkspaceBottomPaddingFactorController());
        list.add(new PageIndicatorHeightFactorController());
        list.add(new ShowAtAGlanceController());
        list.add(new WallpaperScrollingController());
        list.add(new InfiniteScrollingController());
        list.add(new ShowStatusBarController());
        list.add(new DarkStatusBarController());
        list.add(new SmartspaceShowTimeController());
        list.add(new SmartspaceShowDateController());
        list.add(new SmartspaceNowPlayingController());
        list.add(new Use24hFormatController());
        list.add(new BlurIntensityController());
        list.add(new PageTransitionController());
        list.add(new AppLaunchAnimationController());
        list.add(new DarkModeController());
        list.add(new AccentColorController());
        list.add(new AccentHexController());
        list.add(new ThemedIconsController());
        list.add(new WallpaperDepthEffectController());
        list.add(new AllowWidgetOverlapController());
        list.add(new IconPackController());
        list.add(new IconShapeController());
        list.add(new CustomIconShapeRadiusController());
        list.add(new DotColorController());
        list.add(new DotTextColorController());
        list.add(new FontFamilyController());
        list.add(new FontWeightController());

        // Drawer
        list.add(new DrawerIconSizeFactorController());
        list.add(new ShowDrawerLabelsController());
        list.add(new DrawerLabelSizeFactorController());
        list.add(new HiddenAppsController());
        list.add(new DrawerFoldersManageController());
        list.add(new DrawerColumnsController());
        list.add(new EnableTwoLineToggleController());
        list.add(new HiddenAppsInSearchController());
        list.add(new DrawerHapticController());
        list.add(new DrawerCellHeightFactorController());
        list.add(new DrawerLeftRightMarginFactorController());
        list.add(new DrawerShowScrollbarController());
        list.add(new DrawerThemedIconsController());
        list.add(new ShowSuggestedAppsController());
        list.add(new DrawerBgColorController());
        list.add(new DrawerBgHexController());
        list.add(new DrawerOpacityController());
        list.add(new DrawerSortOrderController());
        list.add(new DrawerListViewController());
        list.add(new DrawerAnimationController());
        list.add(new HideDrawerSearchController());
        list.add(new SearchBarBottomController());
        list.add(new RememberDrawerPositionController());

        // Dock
        list.add(new ShowDockController());
        list.add(new DockIconCountController());
        list.add(new DockLabelsController());
        list.add(new DockBgHexController());
        list.add(new DockBgColorController());
        list.add(new DockBgOpacityController());
        list.add(new DockCornerRadiusController());
        list.add(new HotseatBottomFactorController());
        list.add(new DockSearchBarController());
        list.add(new HotseatInsetLeftController());
        list.add(new HotseatInsetRightController());
        list.add(new HotseatInsetTopController());
        list.add(new HotseatInsetBottomController());

        // Folders
        list.add(new ShowFolderLabelsController());
        list.add(new FolderColumnsController());
        list.add(new FolderBgColorController());
        list.add(new FolderBgOpacityController());
        list.add(new FolderPreviewBgOpacityController());
        list.add(new FolderBadgesController());
        list.add(new FolderSpringAnimController());

        // Search
        list.add(new SearchProviderController());
        list.add(new SearchResultCountController());
        list.add(new FuzzySearchController());
        list.add(new ShowCalculatorController());
        list.add(new SearchPeopleController());
        list.add(new SearchFilesController());
        list.add(new SearchAudioController());
        list.add(new SearchVisualMediaController());
        list.add(new SearchSettingsController());
        list.add(new SearchStartpageSuggestionController());
        list.add(new SearchBarColorController());
        list.add(new SearchBarRadiusController());
        list.add(new ContactSearchController());
        list.add(new ShortcutSearchController());
        list.add(new WebSuggestionUrlController());
        list.add(new WebSuggestionNameController());
        list.add(new WebSuggestionMaxDelayController());
        list.add(new WebSuggestionMaxCountController());
        list.add(new AutoShowKeyboardController());
        list.add(new KeyboardAutoHideController());

        // Gestures
        list.add(new GestureDoubleTapController());
        list.add(new GestureSwipeDownController());
        list.add(new GestureEdgeLeftController());
        list.add(new GestureEdgeRightController());

        // Widgets
        list.add(new WidgetRoundedCornersController());
        list.add(new ForceWidgetResizeController());
        list.add(new WidgetUnlimitedSizeController());
        list.add(new WidgetPaddingController());

        // Backup
        list.add(new BackupCreateController());
        list.add(new BackupRestoreController());
        list.add(new LawnchairImportController());

        // Advanced
        list.add(new SingleLayerModeController());
        list.add(new RestartLauncherController());
        list.add(new ResetAppRenamesController());

        return list;
    }

    /** Total number of controllers registered. Reported in Plan §9 done-criteria. */
    public static int size() {
        return ALL_CONTROLLERS.size();
    }

    /** Returns the controllers that bind to keys present in {@code screen}. */
    public static List<PreferenceController> forScreen(PreferenceScreen screen) {
        Set<String> presentKeys = walkKeys(screen);
        List<PreferenceController> matches = new ArrayList<>();
        for (PreferenceController c : ALL_CONTROLLERS) {
            if (presentKeys.contains(c.key())) {
                matches.add(c);
            }
        }
        return Collections.unmodifiableList(matches);
    }

    private static Set<String> walkKeys(PreferenceGroup g) {
        Set<String> keys = new HashSet<>();
        collectKeys(g, keys);
        return keys;
    }

    private static void collectKeys(PreferenceGroup g, Set<String> out) {
        for (int i = 0; i < g.getPreferenceCount(); i++) {
            Preference p = g.getPreference(i);
            String k = p.getKey();
            if (k != null) {
                out.add(k);
            }
            if (p instanceof PreferenceGroup) {
                collectKeys((PreferenceGroup) p, out);
            }
        }
    }
}
