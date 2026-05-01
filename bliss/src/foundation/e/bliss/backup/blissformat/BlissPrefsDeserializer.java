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
 * File:    bliss/src/foundation/e/bliss/backup/blissformat/BlissPrefsDeserializer.java
 * Module:  bliss source-set  (foundation.e.bliss.backup.blissformat)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/backup/blissformat/):
 *   ├── BlissBackupZip.java            — ZIP envelope read/write
 *   ├── BlissPrefsSerializer.java      — LauncherPrefs → prefs.json
 *   └── BlissPrefsDeserializer.java    — prefs.json → LauncherPrefs (this file)  ← THIS FILE
 *
 * Purpose:
 *   Owns the read-half of the Bliss backup pref schema. Walks each known
 *   key in the JSON object and writes the corresponding LauncherPrefs
 *   entry, validating the gesture-handler string keys against the
 *   GestureHandler allow-list (and "app:<ComponentName>" form). Keys not
 *   present in the JSON are left untouched. Behaviour-preserving with the
 *   pre-Migration05 monolithic restoreFromBackup; a forthcoming round-trip
 *   test pins this guarantee.
 *
 * Consumed by:
 *   - foundation.e.bliss.backup.BackupRestoreHelper  — façade orchestrator
 *
 * Plan reference: Plans/Migration05/02-backup-restore-decomp.md (speculative)
 */
package foundation.e.bliss.backup.blissformat;

import android.content.ComponentName;

import com.android.launcher3.LauncherPrefs;

import foundation.e.bliss.gestures.GestureHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Deserialises the prefs.json payload of a .bliss backup into LauncherPrefs.
 */
public final class BlissPrefsDeserializer {

    private static final Set<String> VALID_GESTURE_HANDLERS;
    static {
        Set<String> set = new HashSet<>(Arrays.asList(GestureHandler.HANDLER_NONE, GestureHandler.HANDLER_SLEEP,
                GestureHandler.HANDLER_NOTIFICATIONS, GestureHandler.HANDLER_QUICK_SETTINGS,
                GestureHandler.HANDLER_APP_DRAWER, GestureHandler.HANDLER_APP_SEARCH, GestureHandler.HANDLER_ASSISTANT,
                GestureHandler.HANDLER_RECENTS));
        VALID_GESTURE_HANDLERS = Collections.unmodifiableSet(set);
    }

    private BlissPrefsDeserializer() {
    }

    /** Returns true on success, false on parse error. */
    public static boolean fromJson(String prefsJson, LauncherPrefs prefs) {
        if (prefsJson == null)
            return false;
        JSONObject json;
        try {
            json = new JSONObject(prefsJson);
        } catch (JSONException e) {
            return false;
        }
        try {
            applyBooleans(json, prefs);
            applyIntegers(json, prefs);
            applyStrings(json, prefs);
            applyTrailingExtras(json, prefs);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     * A handler value is valid if it is a known constant or "app:<ComponentName>".
     */
    public static boolean isValidGestureHandler(String value) {
        if (value == null)
            return false;
        if (VALID_GESTURE_HANDLERS.contains(value))
            return true;
        if (value.startsWith(GestureHandler.HANDLER_APP_PREFIX)) {
            String component = value.substring(GestureHandler.HANDLER_APP_PREFIX.length());
            return ComponentName.unflattenFromString(component) != null;
        }
        return false;
    }

    private static void applyBooleans(JSONObject json, LauncherPrefs prefs) throws JSONException {
        if (json.has("show_home_labels"))
            prefs.put(LauncherPrefs.SHOW_HOME_LABELS, json.getBoolean("show_home_labels"));
        if (json.has("show_drawer_labels"))
            prefs.put(LauncherPrefs.SHOW_DRAWER_LABELS, json.getBoolean("show_drawer_labels"));
        if (json.has("show_dock"))
            prefs.put(LauncherPrefs.SHOW_DOCK, json.getBoolean("show_dock"));
        if (json.has("show_folder_labels"))
            prefs.put(LauncherPrefs.SHOW_FOLDER_LABELS, json.getBoolean("show_folder_labels"));
        if (json.has("wallpaper_scrolling"))
            prefs.put(LauncherPrefs.WALLPAPER_SCROLLING, json.getBoolean("wallpaper_scrolling"));
        if (json.has("infinite_scrolling"))
            prefs.put(LauncherPrefs.INFINITE_SCROLLING, json.getBoolean("infinite_scrolling"));
        if (json.has("show_status_bar"))
            prefs.put(LauncherPrefs.SHOW_STATUS_BAR, json.getBoolean("show_status_bar"));
        if (json.has("force_widget_resize"))
            prefs.put(LauncherPrefs.FORCE_WIDGET_RESIZE, json.getBoolean("force_widget_resize"));
        if (json.has("widget_rounded_corners"))
            prefs.put(LauncherPrefs.WIDGET_ROUNDED_CORNERS, json.getBoolean("widget_rounded_corners"));
        if (json.has("widget_unlimited_size"))
            prefs.put(LauncherPrefs.WIDGET_UNLIMITED_SIZE, json.getBoolean("widget_unlimited_size"));
        if (json.has("show_at_a_glance"))
            prefs.put(LauncherPrefs.SHOW_AT_A_GLANCE, json.getBoolean("show_at_a_glance"));
        if (json.has("hide_drawer_search"))
            prefs.put(LauncherPrefs.HIDE_DRAWER_SEARCH, json.getBoolean("hide_drawer_search"));
        if (json.has("auto_show_keyboard"))
            prefs.put(LauncherPrefs.AUTO_SHOW_KEYBOARD, json.getBoolean("auto_show_keyboard"));
        if (json.has("fuzzy_search"))
            prefs.put(LauncherPrefs.FUZZY_SEARCH, json.getBoolean("fuzzy_search"));
        if (json.has("show_calculator"))
            prefs.put(LauncherPrefs.SHOW_CALCULATOR, json.getBoolean("show_calculator"));
        if (json.has("drawer_list_view"))
            prefs.put(LauncherPrefs.DRAWER_LIST_VIEW, json.getBoolean("drawer_list_view"));
        if (json.has("contact_search"))
            prefs.put(LauncherPrefs.CONTACT_SEARCH, json.getBoolean("contact_search"));
        if (json.has("shortcut_search"))
            prefs.put(LauncherPrefs.SHORTCUT_SEARCH, json.getBoolean("shortcut_search"));
        if (json.has("search_bar_bottom"))
            prefs.put(LauncherPrefs.SEARCH_BAR_BOTTOM, json.getBoolean("search_bar_bottom"));
        if (json.has("dock_labels"))
            prefs.put(LauncherPrefs.DOCK_LABELS, json.getBoolean("dock_labels"));
        if (json.has("keyboard_auto_hide"))
            prefs.put(LauncherPrefs.KEYBOARD_AUTO_HIDE, json.getBoolean("keyboard_auto_hide"));
        if (json.has("folder_badges"))
            prefs.put(LauncherPrefs.FOLDER_BADGES, json.getBoolean("folder_badges"));
    }

    private static void applyIntegers(JSONObject json, LauncherPrefs prefs) throws JSONException {
        if (json.has("icon_size_factor"))
            prefs.put(LauncherPrefs.ICON_SIZE_FACTOR, json.getInt("icon_size_factor"));
        if (json.has("drawer_icon_size_factor"))
            prefs.put(LauncherPrefs.DRAWER_ICON_SIZE_FACTOR, json.getInt("drawer_icon_size_factor"));
        if (json.has("home_label_size_factor"))
            prefs.put(LauncherPrefs.HOME_LABEL_SIZE_FACTOR, json.getInt("home_label_size_factor"));
        if (json.has("drawer_label_size_factor"))
            prefs.put(LauncherPrefs.DRAWER_LABEL_SIZE_FACTOR, json.getInt("drawer_label_size_factor"));
        if (json.has("dock_icon_count"))
            prefs.put(LauncherPrefs.DOCK_ICON_COUNT, json.getInt("dock_icon_count"));
        if (json.has("blur_intensity"))
            prefs.put(LauncherPrefs.BLUR_INTENSITY, json.getInt("blur_intensity"));
        if (json.has("drawer_columns"))
            prefs.put(LauncherPrefs.DRAWER_COLUMNS, json.getInt("drawer_columns"));
        if (json.has("folder_columns"))
            prefs.put(LauncherPrefs.FOLDER_COLUMNS, json.getInt("folder_columns"));
        if (json.has("drawer_opacity"))
            prefs.put(LauncherPrefs.DRAWER_OPACITY, json.getInt("drawer_opacity"));
        if (json.has("folder_bg_opacity"))
            prefs.put(LauncherPrefs.FOLDER_BG_OPACITY, json.getInt("folder_bg_opacity"));
        if (json.has("folder_preview_bg_opacity"))
            prefs.put(LauncherPrefs.FOLDER_PREVIEW_BG_OPACITY, json.getInt("folder_preview_bg_opacity"));
        if (json.has("search_result_count"))
            prefs.put(LauncherPrefs.SEARCH_RESULT_COUNT, json.getInt("search_result_count"));
        if (json.has("dock_bg_opacity"))
            prefs.put(LauncherPrefs.DOCK_BG_OPACITY, json.getInt("dock_bg_opacity"));
        if (json.has("search_bar_radius"))
            prefs.put(LauncherPrefs.SEARCH_BAR_RADIUS, json.getInt("search_bar_radius"));
    }

    private static void applyStrings(JSONObject json, LauncherPrefs prefs) throws JSONException {
        if (json.has("gesture_double_tap")) {
            String val = json.getString("gesture_double_tap");
            if (isValidGestureHandler(val))
                prefs.put(LauncherPrefs.GESTURE_DOUBLE_TAP, val);
        }
        if (json.has("gesture_swipe_down")) {
            String val = json.getString("gesture_swipe_down");
            if (isValidGestureHandler(val))
                prefs.put(LauncherPrefs.GESTURE_SWIPE_DOWN, val);
        }
        if (json.has("drawer_bg_color"))
            prefs.put(LauncherPrefs.DRAWER_BG_COLOR, json.getString("drawer_bg_color"));
        if (json.has("folder_bg_color"))
            prefs.put(LauncherPrefs.FOLDER_BG_COLOR, json.getString("folder_bg_color"));
        if (json.has("search_provider"))
            prefs.put(LauncherPrefs.SEARCH_PROVIDER, json.getString("search_provider"));
        if (json.has("icon_pack"))
            prefs.put(LauncherPrefs.ICON_PACK, json.getString("icon_pack"));
        if (json.has("font_family"))
            prefs.put(LauncherPrefs.FONT_FAMILY, json.getString("font_family"));
        if (json.has("accent_color"))
            prefs.put(LauncherPrefs.ACCENT_COLOR, json.getString("accent_color"));
        if (json.has("drawer_sort_order"))
            prefs.put(LauncherPrefs.DRAWER_SORT_ORDER, json.getString("drawer_sort_order"));
        if (json.has("dock_bg_color"))
            prefs.put(LauncherPrefs.DOCK_BG_COLOR, json.getString("dock_bg_color"));
        if (json.has("search_bar_color"))
            prefs.put(LauncherPrefs.SEARCH_BAR_COLOR, json.getString("search_bar_color"));
        if (json.has("dot_color"))
            prefs.put(LauncherPrefs.DOT_COLOR, json.getString("dot_color"));
        if (json.has("page_transition"))
            prefs.put(LauncherPrefs.PAGE_TRANSITION, json.getString("page_transition"));
        if (json.has("dark_mode"))
            prefs.put(LauncherPrefs.DARK_MODE, json.getString("dark_mode"));
        if (json.has("gesture_edge_left")) {
            String val = json.getString("gesture_edge_left");
            if (isValidGestureHandler(val))
                prefs.put(LauncherPrefs.GESTURE_EDGE_LEFT, val);
        }
        if (json.has("gesture_edge_right")) {
            String val = json.getString("gesture_edge_right");
            if (isValidGestureHandler(val))
                prefs.put(LauncherPrefs.GESTURE_EDGE_RIGHT, val);
        }
    }

    private static void applyTrailingExtras(JSONObject json, LauncherPrefs prefs) throws JSONException {
        if (json.has("widget_padding"))
            prefs.put(LauncherPrefs.WIDGET_PADDING, json.getInt("widget_padding"));
        if (json.has("dock_corner_radius"))
            prefs.put(LauncherPrefs.DOCK_CORNER_RADIUS, json.getInt("dock_corner_radius"));
        if (json.has("app_launch_animation"))
            prefs.put(LauncherPrefs.APP_LAUNCH_ANIMATION, json.getString("app_launch_animation"));
        if (json.has("font_weight"))
            prefs.put(LauncherPrefs.FONT_WEIGHT, json.getString("font_weight"));
        if (json.has("drawer_animation"))
            prefs.put(LauncherPrefs.DRAWER_ANIMATION, json.getString("drawer_animation"));
    }
}
