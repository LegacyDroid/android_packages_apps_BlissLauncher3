/* Copyright (C) 2026 MURENA SAS — SPDX-License-Identifier: GPL-3.0-or-later */
/*
 * File:    bliss/src/foundation/e/bliss/backup/prefs/PrefMapperRegistry.java
 * Module:  bliss source-set  (foundation.e.bliss.backup.prefs)   Role: NEW
 *
 * Tree (foundation/e/bliss/backup/prefs/):
 *   ├── BasicMappers.java                  — typed primitives
 *   ├── DomainMappers.java                 — theme/shape/hidden/folder/app-name
 *   ├── DrawerFoldersWriter.java           — JSON-list → Room store
 *   ├── GestureMappers.java                — gesture handler JSON → Bliss handler key
 *   ├── PrefMapper.java                    — functional-interface seam
 *   ├── PrefMapperRegistry.java            — orchestrator  ← THIS FILE
 *   ├── RecognizedKeys.java                — Lawnchair-key allow-lists
 *   └── WorkspaceGridFontPopupMappers.java — grid/font/popup-order mappers
 *
 * Purpose: replaces LawnchairImportHelper.applyMappedPreferences. Body
 *   preserves original mapping order; reordering risks §5 invariants.
 * Consumed by: foundation.e.bliss.backup.LawnchairImportHelper
 * Plan ref: Plans/Migration04/02-importer-decomposition.md §4.4
 */
package foundation.e.bliss.backup.prefs;

import android.content.Context;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.dagger.LauncherComponentProvider;

import foundation.e.bliss.utils.Logger;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Orchestrator that walks the original applyMappedPreferences sequence. */
public final class PrefMapperRegistry {

    private static final Logger LOG = Logger.tag("LawnchairImport");

    private PrefMapperRegistry() {
    }

    public static Set<String> recognisedKeys() {
        return RecognizedKeys.RECOGNIZED;
    }
    public static Set<String> internalKeys() {
        return RecognizedKeys.INTERNAL;
    }

    /** Apply every recognised Lawnchair pref; preserves legacy mapping order. */
    public static int applyAll(Context context, Map<String, Object> src) {
        LauncherPrefs prefs = LauncherComponentProvider.get(context).getLauncherPrefs();
        int count = 0;
        count += applyBooleanPrefs(src, prefs);
        count += applyStringPrefsBlock1(src, prefs);
        count += applyClampedFloatPrefs(src, prefs);
        count += applyDrawerVisualPrefs(src, prefs);
        count += applyHotseatInsetPrefs(src, prefs);
        count += applySearchResultPrefs(src, prefs);
        count += applyDrawerFoldersBlock(context, src, prefs);
        count += applyMoreBooleanPrefs(src, prefs);
        count += applyIntPrefs(src, prefs);
        count += applyFloatToIntPercentPrefs(src, prefs);
        count += applyOpacityPrefs(src, prefs);
        count += applyIconPackAndDarkMode(src, prefs);
        count += applyGestureAndHiddenApps(src, prefs);
        count += applySmartspaceAndHotseatMode(src, prefs);
        count += applyNotifAndKeyboard(src, prefs);
        count += applyThemeAccentShape(context, src, prefs);
        applyTrailingForcedPrefs(src, prefs);
        count += applyWorkspaceGridFontPopup(context, src, prefs);
        count += applyTrailingFloatPrefs(src, prefs);
        count += applySmartspaceSubToggles(src, prefs);
        count += applyMiscFinalPrefs(src, prefs);
        return count;
    }
    private static int applyBooleanPrefs(Map<String, Object> src, LauncherPrefs prefs) {
        int c = 0;
        if (BasicMappers.mapBoolean(src, prefs, "show_icon_labels_on_home_screen", LauncherPrefs.SHOW_HOME_LABELS))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "pref_showHomeLabels", LauncherPrefs.SHOW_HOME_LABELS))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "pref_add_icon_to_home", LauncherPrefs.ADD_ICON_TO_HOME))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "addIconToHome", LauncherPrefs.ADD_ICON_TO_HOME))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "lock_home_screen", LauncherPrefs.WORKSPACE_LOCK))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "pref_lockHomeScreen", LauncherPrefs.WORKSPACE_LOCK))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "prefs_wrapAdaptive", LauncherPrefs.WRAP_ADAPTIVE_ICONS))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "pref_wrapAdaptiveIcons", LauncherPrefs.WRAP_ADAPTIVE_ICONS))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "wallpaper_depth_effect", LauncherPrefs.WALLPAPER_DEPTH_EFFECT))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "pref_wallpaperDepthEffect", LauncherPrefs.WALLPAPER_DEPTH_EFFECT))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "allow_widget_overlap", LauncherPrefs.ALLOW_WIDGET_OVERLAP))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "pref_allowWidgetOverlap", LauncherPrefs.ALLOW_WIDGET_OVERLAP))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "app_drawer_haptic_feedback", LauncherPrefs.DRAWER_HAPTIC))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "pref_appDrawerHapticFeedback", LauncherPrefs.DRAWER_HAPTIC))
            c++;
        return c;
    }
    private static int applyStringPrefsBlock1(Map<String, Object> src, LauncherPrefs prefs) {
        int c = 0;
        if (BasicMappers.mapString(src, prefs, "hidden_apps_in_search", LauncherPrefs.HIDDEN_APPS_IN_SEARCH))
            c++;
        if (BasicMappers.mapString(src, prefs, "pref_hiddenAppsInSearch", LauncherPrefs.HIDDEN_APPS_IN_SEARCH))
            c++;
        if (BasicMappers.mapString(src, prefs, "notification_dot_text_color", LauncherPrefs.DOT_TEXT_COLOR))
            c++;
        if (BasicMappers.mapString(src, prefs, "pref_notificationDotTextColor", LauncherPrefs.DOT_TEXT_COLOR))
            c++;
        if (BasicMappers.mapString(src, prefs, "workspace_text_color", LauncherPrefs.HOME_LABEL_COLOR_MODE))
            c++;
        if (BasicMappers.mapString(src, prefs, "pref_workspaceTextColor", LauncherPrefs.HOME_LABEL_COLOR_MODE))
            c++;
        return c;
    }
    private static int applyClampedFloatPrefs(Map<String, Object> src, LauncherPrefs prefs) {
        int c = 0;
        c += BasicMappers.mapFloatToIntPercentClamped(src, prefs, "drawer_cell_height_factor",
                LauncherPrefs.DRAWER_CELL_HEIGHT_FACTOR, 150);
        c += BasicMappers.mapFloatToIntPercentClamped(src, prefs, "drawer_left_right_factor",
                LauncherPrefs.DRAWER_LEFT_RIGHT_MARGIN_FACTOR, 150);
        return c;
    }
    private static int applyDrawerVisualPrefs(Map<String, Object> src, LauncherPrefs prefs) {
        int c = 0;
        if (BasicMappers.mapBoolean(src, prefs, "show_scrollbar", LauncherPrefs.DRAWER_SHOW_SCROLLBAR))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "pref_showScrollbar", LauncherPrefs.DRAWER_SHOW_SCROLLBAR))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "drawer_themed_icons", LauncherPrefs.DRAWER_THEMED_ICONS))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "tint_icon_pack_backgrounds", LauncherPrefs.TINT_ICON_PACK_BG))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "show_suggested_apps_in_drawer", LauncherPrefs.SHOW_SUGGESTED_APPS))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "pref_showSuggestedAppsInDrawer", LauncherPrefs.SHOW_SUGGESTED_APPS))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "dock_search_bar", LauncherPrefs.DOCK_SEARCH_BAR))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "pref_dockSearchBar", LauncherPrefs.DOCK_SEARCH_BAR))
            c++;
        return c;
    }
    private static int applyHotseatInsetPrefs(Map<String, Object> src, LauncherPrefs prefs) {
        int c = 0;
        if (BasicMappers.mapInt(src, prefs, "hotseat_bg_horizontal_inset_left", LauncherPrefs.HOTSEAT_INSET_LEFT))
            c++;
        if (BasicMappers.mapInt(src, prefs, "hotseat_bg_horizontal_inset_right", LauncherPrefs.HOTSEAT_INSET_RIGHT))
            c++;
        if (BasicMappers.mapInt(src, prefs, "hotseat_bg_vertical_inset_top", LauncherPrefs.HOTSEAT_INSET_TOP))
            c++;
        if (BasicMappers.mapInt(src, prefs, "hotseat_bg_vertical_inset_bottom", LauncherPrefs.HOTSEAT_INSET_BOTTOM))
            c++;
        return c;
    }
    private static int applySearchResultPrefs(Map<String, Object> src, LauncherPrefs prefs) {
        int c = 0;
        if (BasicMappers.mapBoolean(src, prefs, "pref_searchResultPeople", LauncherPrefs.SEARCH_PEOPLE))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "pref_searchResultFiles", LauncherPrefs.SEARCH_FILES))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "pref_searchResultAudio", LauncherPrefs.SEARCH_AUDIO))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "pref_searchResultVisualMedia", LauncherPrefs.SEARCH_VISUAL_MEDIA))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "pref_searchResultSettingsEntry", LauncherPrefs.SEARCH_SETTINGS))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "pref_searchResultStartPageSuggestion",
                LauncherPrefs.SEARCH_STARTPAGE_SUGGESTION))
            c++;
        if (BasicMappers.mapString(src, prefs, "pref_webSuggestionProviderUrl", LauncherPrefs.WEB_SUGGESTION_URL))
            c++;
        if (BasicMappers.mapString(src, prefs, "pref_webSuggestionProviderName", LauncherPrefs.WEB_SUGGESTION_NAME))
            c++;
        if (BasicMappers.mapInt(src, prefs, "pref_maxWebSuggestionDelay", LauncherPrefs.WEB_SUGGESTION_MAX_DELAY))
            c++;
        if (BasicMappers.mapInt(src, prefs, "pref_maxWebSuggestionResultCount", LauncherPrefs.WEB_SUGGESTION_MAX_COUNT))
            c++;
        return c;
    }
    private static int applyDrawerFoldersBlock(Context context, Map<String, Object> src, LauncherPrefs prefs) {
        int c = 0;
        if (BasicMappers.mapBoolean(src, prefs, "pref_drawerFoldersEnabled", LauncherPrefs.DRAWER_FOLDERS_ENABLED))
            c++;
        if (BasicMappers.mapString(src, prefs, "pref_drawerFolders", LauncherPrefs.DRAWER_FOLDERS_MAP))
            c++;
        try {
            String foldersJson = prefs.get(LauncherPrefs.DRAWER_FOLDERS_MAP);
            if (foldersJson != null && !foldersJson.isEmpty() && !"{}".equals(foldersJson)) {
                DrawerFoldersWriter.write(context, foldersJson);
            }
        } catch (Throwable t) {
            LOG.w("Lawnchair import: writeDrawerFoldersToRoom failed", t);
        }
        return c;
    }
    private static int applyMoreBooleanPrefs(Map<String, Object> src, LauncherPrefs prefs) {
        int c = 0;
        c += hit(BasicMappers.mapBoolean(src, prefs, "show_icon_labels_in_drawer", LauncherPrefs.SHOW_DRAWER_LABELS));
        c += hit(BasicMappers.mapBoolean(src, prefs, "pref_allAppsIconLabels", LauncherPrefs.SHOW_DRAWER_LABELS));
        c += hit(BasicMappers.mapBoolean(src, prefs, "show_icon_labels_on_home_screen_folder",
                LauncherPrefs.SHOW_FOLDER_LABELS));
        c += hit(BasicMappers.mapBoolean(src, prefs, "pref_show_hotseat", LauncherPrefs.SHOW_DOCK));
        c += hit(BasicMappers.mapBoolean(src, prefs, "enable_label_dock", LauncherPrefs.DOCK_LABELS));
        c += hit(BasicMappers.mapBoolean(src, prefs, "pref_wallpaperScrolling", LauncherPrefs.WALLPAPER_SCROLLING));
        c += hit(BasicMappers.mapBoolean(src, prefs, "pref_infiniteScrolling", LauncherPrefs.INFINITE_SCROLLING));
        c += hit(BasicMappers.mapBoolean(src, prefs, "show_status_bar", LauncherPrefs.SHOW_STATUS_BAR));
        c += hit(BasicMappers.mapBoolean(src, prefs, "pref_showStatusBar", LauncherPrefs.SHOW_STATUS_BAR));
        c += hit(BasicMappers.mapBoolean(src, prefs, "force_widget_resize", LauncherPrefs.FORCE_WIDGET_RESIZE));
        c += hit(BasicMappers.mapBoolean(src, prefs, "rounded_widgets", LauncherPrefs.WIDGET_ROUNDED_CORNERS));
        c += hit(BasicMappers.mapBoolean(src, prefs, "widget_unlimited_size", LauncherPrefs.WIDGET_UNLIMITED_SIZE));
        c += hit(BasicMappers.mapBoolean(src, prefs, "hide_app_drawer_search_bar", LauncherPrefs.HIDE_DRAWER_SEARCH));
        c += hit(BasicMappers.mapBoolean(src, prefs, "pref_hideAppSearchBar", LauncherPrefs.HIDE_DRAWER_SEARCH));
        c += hit(BasicMappers.mapBoolean(src, prefs, "enable_fuzzy_search", LauncherPrefs.FUZZY_SEARCH));
        c += hit(BasicMappers.mapBoolean(src, prefs, "pref_useFuzzySearch", LauncherPrefs.FUZZY_SEARCH));
        return c;
    }

    private static int hit(boolean mapped) {
        return mapped ? 1 : 0;
    }
    private static int applyIntPrefs(Map<String, Object> src, LauncherPrefs prefs) {
        int c = 0;
        if (BasicMappers.mapInt(src, prefs, "pref_hotseatColumns", LauncherPrefs.DOCK_ICON_COUNT))
            c++;
        if (BasicMappers.mapInt(src, prefs, "drawer_columns", LauncherPrefs.DRAWER_COLUMNS))
            c++;
        if (BasicMappers.mapInt(src, prefs, "pref_allAppsColumns", LauncherPrefs.DRAWER_COLUMNS))
            c++;
        if (BasicMappers.mapInt(src, prefs, "folder_columns", LauncherPrefs.FOLDER_COLUMNS))
            c++;
        if (BasicMappers.mapInt(src, prefs, "pref_folderColumns", LauncherPrefs.FOLDER_COLUMNS))
            c++;
        if (BasicMappers.mapInt(src, prefs, "max_search_result_count", LauncherPrefs.SEARCH_RESULT_COUNT))
            c++;
        if (BasicMappers.mapInt(src, prefs, "pref_maxSearchResultCount", LauncherPrefs.SEARCH_RESULT_COUNT))
            c++;
        return c;
    }
    private static int applyFloatToIntPercentPrefs(Map<String, Object> src, LauncherPrefs prefs) {
        int c = 0;
        c += BasicMappers.mapFloatToIntPercent(src, prefs, new String[]{"home_icon_size_factor", "pref_iconSizeFactor"},
                LauncherPrefs.ICON_SIZE_FACTOR);
        c += BasicMappers.mapFloatToIntPercent(src, prefs,
                new String[]{"drawer_icon_size_factor", "pref_allAppsIconSizeFactor"},
                LauncherPrefs.DRAWER_ICON_SIZE_FACTOR);
        c += BasicMappers.mapFloatToIntPercent(src, prefs,
                new String[]{"home_icon_label_size_factor", "pref_textSizeFactor"},
                LauncherPrefs.HOME_LABEL_SIZE_FACTOR);
        c += BasicMappers.mapFloatToIntPercent(src, prefs,
                new String[]{"drawer_icon_label_size_factor", "pref_allAppsTextSizeFactor"},
                LauncherPrefs.DRAWER_LABEL_SIZE_FACTOR);
        return c;
    }
    private static int applyOpacityPrefs(Map<String, Object> src, LauncherPrefs prefs) {
        int c = 0;
        c += BasicMappers.mapFloatToIntOpacity(src, prefs, "pref_drawerOpacity", LauncherPrefs.DRAWER_OPACITY);
        c += BasicMappers.mapFloatToIntOpacity(src, prefs, "folder_background_opacity",
                LauncherPrefs.FOLDER_BG_OPACITY);
        c += BasicMappers.mapFloatToIntOpacity(src, prefs, "folder_preview_background_opacity",
                LauncherPrefs.FOLDER_PREVIEW_BG_OPACITY);
        c += BasicMappers.mapFloatToIntOpacity(src, prefs, "pref_folderPreviewBgOpacity",
                LauncherPrefs.FOLDER_PREVIEW_BG_OPACITY);
        if (BasicMappers.mapInt(src, prefs, "pref_hotseatBGTransparency", LauncherPrefs.DOCK_BG_OPACITY))
            c++;
        Object v = src.get("pref_hotseatBG");
        if (v instanceof Boolean b) {
            prefs.put(LauncherPrefs.DOCK_BG_OPACITY, b.booleanValue() ? 100 : 0);
            c++;
        }
        if (BasicMappers.mapInt(src, prefs, "pref_wallpaperBlur", LauncherPrefs.BLUR_INTENSITY))
            c++;
        c += BasicMappers.mapFloatToIntOpacity(src, prefs, "pref_hotseatQsbCornerRadius",
                LauncherPrefs.SEARCH_BAR_RADIUS);
        return c;
    }
    private static int applyIconPackAndDarkMode(Map<String, Object> src, LauncherPrefs prefs) {
        int c = 0;
        if (BasicMappers.mapString(src, prefs, "pref_iconPackPackage", LauncherPrefs.ICON_PACK))
            c++;
        c += DomainMappers.mapDarkMode(src, prefs);
        return c;
    }
    private static int applyGestureAndHiddenApps(Map<String, Object> src, LauncherPrefs prefs) {
        int c = 0;
        c += GestureMappers.mapGesture(src, prefs, "double_tap_gesture_handler", LauncherPrefs.GESTURE_DOUBLE_TAP);
        c += GestureMappers.mapGesture(src, prefs, "swipe_down_gesture_handler", LauncherPrefs.GESTURE_SWIPE_DOWN);
        c += DomainMappers.mapHiddenApps(src, prefs);
        return c;
    }
    private static int applySmartspaceAndHotseatMode(Map<String, Object> src, LauncherPrefs prefs) {
        int c = 0;
        if (BasicMappers.mapBoolean(src, prefs, "enable_smartspace", LauncherPrefs.SHOW_AT_A_GLANCE))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "pref_enableMinusOne", LauncherPrefs.SHOW_AT_A_GLANCE))
            c++;
        c += DomainMappers.mapHotseatMode(src, prefs);
        return c;
    }
    private static int applyNotifAndKeyboard(Map<String, Object> src, LauncherPrefs prefs) {
        int c = 0;
        if (BasicMappers.mapBoolean(src, prefs, "show_notification_count", LauncherPrefs.IS_NOTIF_COUNT_ENABLED))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "notification_dots_show_count", LauncherPrefs.IS_NOTIF_COUNT_ENABLED))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "pref_notificationCount", LauncherPrefs.IS_NOTIF_COUNT_ENABLED))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "auto_show_keyboard_in_drawer", LauncherPrefs.AUTO_SHOW_KEYBOARD))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "pref_autoShowKeyboard", LauncherPrefs.AUTO_SHOW_KEYBOARD))
            c++;
        return c;
    }
    private static int applyThemeAccentShape(Context context, Map<String, Object> src, LauncherPrefs prefs) {
        int c = 0;
        c += DomainMappers.mapThemedIcons(src, context);
        c += DomainMappers.mapAccentColor(src, prefs);
        if (BasicMappers.mapString(src, prefs, "icon_pack_package", LauncherPrefs.ICON_PACK))
            c++;
        c += DomainMappers.mapIconShape(src, prefs);
        return c;
    }
    /**
     * Force WORKSPACE_LOCK off unless src set it true; skip first-run wizard;
     * PRESERVE_LAYOUT_GAPS / GRID_FOLDER_PREVIEW moved to PostImportLayoutFix tail.
     */
    private static void applyTrailingForcedPrefs(Map<String, Object> src, LauncherPrefs prefs) {
        Object lockSrc = src.get("lock_home_screen");
        if (lockSrc == null)
            lockSrc = src.get("pref_lockHomeScreen");
        if (!(lockSrc instanceof Boolean lockBool) || !lockBool.booleanValue()) {
            prefs.put(LauncherPrefs.WORKSPACE_LOCK, false);
        }
        prefs.put(LauncherPrefs.FIRST_RUN_LAYOUT_CHOICE_DONE, true);
        prefs.put(LauncherPrefs.IS_SINGLE_LAYER_ENABLED, false);
        prefs.put(LauncherPrefs.IS_SINGLE_LAYER_ENABLED, false);
    }
    private static int applyWorkspaceGridFontPopup(Context context, Map<String, Object> src, LauncherPrefs prefs) {
        int c = 0;
        c += WorkspaceGridFontPopupMappers.mapWorkspaceGrid(src, prefs, context);
        c += WorkspaceGridFontPopupMappers.mapWorkspaceFont(src, prefs);
        c += WorkspaceGridFontPopupMappers.mapPopupOrder(src, prefs);
        c += DomainMappers.mapAppNameMap(src, prefs);
        return c;
    }
    private static int applyTrailingFloatPrefs(Map<String, Object> src, LauncherPrefs prefs) {
        int c = 0;
        c += BasicMappers.mapFloatToIntPercentClamped(src, prefs, "hotseat_bottom_factor",
                LauncherPrefs.HOTSEAT_BOTTOM_FACTOR, 170);
        c += BasicMappers.mapFloatToIntPercentClamped(src, prefs, "page_indicator_height_factor",
                LauncherPrefs.PAGE_INDICATOR_HEIGHT_FACTOR, 100);
        c += BasicMappers.mapFloatToIntPercent(src, prefs, new String[]{"home_icon_label_folder_size_factor"},
                LauncherPrefs.FOLDER_LABEL_SIZE_FACTOR);
        return c;
    }
    private static int applySmartspaceSubToggles(Map<String, Object> src, LauncherPrefs prefs) {
        int c = 0;
        if (BasicMappers.mapBoolean(src, prefs, "smartspace_show_time", LauncherPrefs.SMARTSPACE_SHOW_TIME))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "smartspace_show_date", LauncherPrefs.SMARTSPACE_SHOW_DATE))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "enable_smartspace_now_playing", LauncherPrefs.SMARTSPACE_NOW_PLAYING))
            c++;
        Object v = src.get("smartspace_time_format");
        if (v instanceof String str) {
            String s = str.toLowerCase(Locale.ROOT);
            String norm;
            if (s.contains("24"))
                norm = "24";
            else if (s.contains("12"))
                norm = "12";
            else
                norm = "system";
            prefs.put(LauncherPrefs.SMARTSPACE_TIME_FORMAT, norm);
            c++;
        }
        return c;
    }
    private static int applyMiscFinalPrefs(Map<String, Object> src, LauncherPrefs prefs) {
        int c = 0;
        if (BasicMappers.mapBoolean(src, prefs, "24_hour_format", LauncherPrefs.USE_24H_FORMAT))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "dark_status_bar", LauncherPrefs.DARK_STATUS_BAR))
            c++;
        if (BasicMappers.mapString(src, prefs, "closing_app_overlay", LauncherPrefs.CLOSING_APP_OVERLAY))
            c++;
        if (BasicMappers.mapBoolean(src, prefs, "all_apps_remember_position", LauncherPrefs.REMEMBER_DRAWER_POSITION))
            c++;
        c += DomainMappers.mapFolderColors(src, prefs);
        return c;
    }
}
