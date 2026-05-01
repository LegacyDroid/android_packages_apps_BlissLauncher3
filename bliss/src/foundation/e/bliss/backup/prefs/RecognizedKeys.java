/*
 * Copyright (C) 2026 MURENA SAS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
/*
 * File:    bliss/src/foundation/e/bliss/backup/prefs/RecognizedKeys.java
 * Module:  bliss source-set  (foundation.e.bliss.backup.prefs)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/backup/prefs/):
 *   ├── BasicMappers.java          — typed primitives
 *   ├── DomainMappers.java         — domain-specific mappers
 *   ├── DrawerFoldersWriter.java   — JSON-list → Room store
 *   ├── PrefMapper.java            — single-mapper functional interface
 *   ├── PrefMapperRegistry.java    — orchestrator
 *   └── RecognizedKeys.java        — Lawnchair-key allow-lists  ← THIS FILE
 *
 * Purpose:
 *   Holds the Lawnchair-source-key allow-lists that PrefMapperRegistry
 *   exposes via recognisedKeys() / internalKeys(). Lifted out of the
 *   registry to keep that file under 300 lines post-Migration04 phase 02.
 *
 * Consumed by:
 *   - foundation.e.bliss.backup.prefs.PrefMapperRegistry
 *
 * Plan reference: Plans/Migration04/02-importer-decomposition.md §4.4
 */
package foundation.e.bliss.backup.prefs;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Allow-lists of Lawnchair source keys recognised / known-internal by the
 * importer.
 */
final class RecognizedKeys {

    private RecognizedKeys() {
    }

    /**
     * Lawnchair source keys recognised by this registry. Used to compute the
     * "skipped" list reported back to the user.
     */
    static final Set<String> RECOGNIZED = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            // booleans
            "show_icon_labels_on_home_screen", "pref_showHomeLabels", "show_icon_labels_in_drawer",
            "pref_allAppsIconLabels", "show_icon_labels_on_home_screen_folder", "pref_show_hotseat",
            "enable_label_dock", "pref_wallpaperScrolling", "pref_infiniteScrolling", "show_status_bar",
            "pref_showStatusBar", "force_widget_resize", "rounded_widgets", "widget_unlimited_size",
            "hide_app_drawer_search_bar", "pref_hideAppSearchBar", "enable_fuzzy_search", "pref_useFuzzySearch",
            "enable_smartspace", "pref_enableMinusOne", "show_notification_count", "notification_dots_show_count",
            "pref_notificationCount", "themed_icons", "pref_themedIcons", "auto_show_keyboard_in_drawer",
            "pref_autoShowKeyboard",
            // ints
            "pref_hotseatColumns", "drawer_columns", "pref_allAppsColumns", "folder_columns", "pref_folderColumns",
            "max_search_result_count", "pref_maxSearchResultCount", "pref_hotseatBGTransparency", "pref_wallpaperBlur",
            // floats
            "home_icon_size_factor", "pref_iconSizeFactor", "drawer_icon_size_factor", "pref_allAppsIconSizeFactor",
            "home_icon_label_size_factor", "pref_textSizeFactor", "drawer_icon_label_size_factor",
            "pref_allAppsTextSizeFactor", "pref_drawerOpacity", "folder_background_opacity",
            "pref_folderPreviewBgOpacity", "folder_preview_background_opacity", "pref_hotseatQsbCornerRadius",
            // strings
            "pref_iconPackPackage", "icon_pack_package", "pref_launcherTheme",
            // gestures
            "double_tap_gesture_handler", "swipe_down_gesture_handler",
            // special
            "hidden_apps", "hidden-app-set", "hotseat_mode", "accent_color", "pref_accentColor", "icon_shape",
            "pref_iconShape", "pref_workspaceColumns", "pref_workspaceRows", "pref_workspaceFont", "workspace_font",
            "launcher_popup_order", "pref_appNameMap", "hotseat_bottom_factor", "page_indicator_height_factor",
            "pref_add_icon_to_home", "addIconToHome", "lock_home_screen", "pref_lockHomeScreen", "prefs_wrapAdaptive",
            "pref_wrapAdaptiveIcons", "wallpaper_depth_effect", "pref_wallpaperDepthEffect", "allow_widget_overlap",
            "pref_allowWidgetOverlap", "app_drawer_haptic_feedback", "pref_appDrawerHapticFeedback",
            "hidden_apps_in_search", "pref_hiddenAppsInSearch", "notification_dot_text_color",
            "pref_notificationDotTextColor", "workspace_text_color", "pref_workspaceTextColor",
            "drawer_cell_height_factor", "drawer_left_right_factor", "show_scrollbar", "pref_showScrollbar",
            "drawer_themed_icons", "tint_icon_pack_backgrounds", "show_suggested_apps_in_drawer",
            "pref_showSuggestedAppsInDrawer", "dock_search_bar", "pref_dockSearchBar",
            "hotseat_bg_horizontal_inset_left", "hotseat_bg_horizontal_inset_right", "hotseat_bg_vertical_inset_top",
            "hotseat_bg_vertical_inset_bottom", "pref_searchResultPeople", "pref_searchResultFiles",
            "pref_searchResultAudio", "pref_searchResultVisualMedia", "pref_searchResultSettingsEntry",
            "pref_searchResultStartPageSuggestion", "pref_webSuggestionProviderUrl", "pref_webSuggestionProviderName",
            "pref_maxWebSuggestionDelay", "pref_maxWebSuggestionResultCount", "pref_drawerFoldersEnabled",
            "pref_drawerFolders", "home_icon_label_folder_size_factor", "pref_hotseatBG", "smartspace_show_time",
            "smartspace_show_date", "enable_smartspace_now_playing", "smartspace_time_format", "24_hour_format",
            "dark_status_bar", "closing_app_overlay", "all_apps_remember_position", "folder_color", "folder_colors")));

    /**
     * Lawnchair internal keys (not user-facing). Filtered out of the "skipped" log.
     */
    static final Set<String> INTERNAL = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("version",
            "migration_src_device_type", "migration_src_workspace_size", "migration_src_hotseat_count",
            "migration_src_db_file", "legacy_popup_options_migrated", "pref_allapps_bulk_icon_loading")));
}
