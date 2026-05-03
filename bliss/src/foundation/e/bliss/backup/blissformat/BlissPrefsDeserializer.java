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

import com.android.launcher3.ConstantItem;
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
        putBool(json, "show_home_labels", prefs, LauncherPrefs.SHOW_HOME_LABELS);
        putBool(json, "show_drawer_labels", prefs, LauncherPrefs.SHOW_DRAWER_LABELS);
        putBool(json, "show_dock", prefs, LauncherPrefs.SHOW_DOCK);
        putBool(json, "show_folder_labels", prefs, LauncherPrefs.SHOW_FOLDER_LABELS);
        putBool(json, "wallpaper_scrolling", prefs, LauncherPrefs.WALLPAPER_SCROLLING);
        putBool(json, "infinite_scrolling", prefs, LauncherPrefs.INFINITE_SCROLLING);
        putBool(json, "show_status_bar", prefs, LauncherPrefs.SHOW_STATUS_BAR);
        putBool(json, "force_widget_resize", prefs, LauncherPrefs.FORCE_WIDGET_RESIZE);
        putBool(json, "widget_rounded_corners", prefs, LauncherPrefs.WIDGET_ROUNDED_CORNERS);
        putBool(json, "widget_unlimited_size", prefs, LauncherPrefs.WIDGET_UNLIMITED_SIZE);
        putBool(json, "show_at_a_glance", prefs, LauncherPrefs.SHOW_AT_A_GLANCE);
        putBool(json, "hide_drawer_search", prefs, LauncherPrefs.HIDE_DRAWER_SEARCH);
        putBool(json, "auto_show_keyboard", prefs, LauncherPrefs.AUTO_SHOW_KEYBOARD);
        putBool(json, "fuzzy_search", prefs, LauncherPrefs.FUZZY_SEARCH);
        putBool(json, "show_calculator", prefs, LauncherPrefs.SHOW_CALCULATOR);
        putBool(json, "drawer_list_view", prefs, LauncherPrefs.DRAWER_LIST_VIEW);
        putBool(json, "contact_search", prefs, LauncherPrefs.CONTACT_SEARCH);
        putBool(json, "shortcut_search", prefs, LauncherPrefs.SHORTCUT_SEARCH);
        putBool(json, "search_bar_bottom", prefs, LauncherPrefs.SEARCH_BAR_BOTTOM);
        putBool(json, "dock_labels", prefs, LauncherPrefs.DOCK_LABELS);
        putBool(json, "keyboard_auto_hide", prefs, LauncherPrefs.KEYBOARD_AUTO_HIDE);
        putBool(json, "folder_badges", prefs, LauncherPrefs.FOLDER_BADGES);
    }

    private static void applyIntegers(JSONObject json, LauncherPrefs prefs) throws JSONException {
        putInt(json, "icon_size_factor", prefs, LauncherPrefs.ICON_SIZE_FACTOR);
        putInt(json, "drawer_icon_size_factor", prefs, LauncherPrefs.DRAWER_ICON_SIZE_FACTOR);
        putInt(json, "home_label_size_factor", prefs, LauncherPrefs.HOME_LABEL_SIZE_FACTOR);
        putInt(json, "drawer_label_size_factor", prefs, LauncherPrefs.DRAWER_LABEL_SIZE_FACTOR);
        putInt(json, "dock_icon_count", prefs, LauncherPrefs.DOCK_ICON_COUNT);
        putInt(json, "blur_intensity", prefs, LauncherPrefs.BLUR_INTENSITY);
        putInt(json, "drawer_columns", prefs, LauncherPrefs.DRAWER_COLUMNS);
        putInt(json, "folder_columns", prefs, LauncherPrefs.FOLDER_COLUMNS);
        putInt(json, "drawer_opacity", prefs, LauncherPrefs.DRAWER_OPACITY);
        putInt(json, "folder_bg_opacity", prefs, LauncherPrefs.FOLDER_BG_OPACITY);
        putInt(json, "folder_preview_bg_opacity", prefs, LauncherPrefs.FOLDER_PREVIEW_BG_OPACITY);
        putInt(json, "search_result_count", prefs, LauncherPrefs.SEARCH_RESULT_COUNT);
        putInt(json, "dock_bg_opacity", prefs, LauncherPrefs.DOCK_BG_OPACITY);
        putInt(json, "search_bar_radius", prefs, LauncherPrefs.SEARCH_BAR_RADIUS);
    }

    private static void applyStrings(JSONObject json, LauncherPrefs prefs) throws JSONException {
        putGesture(json, "gesture_double_tap", prefs, LauncherPrefs.GESTURE_DOUBLE_TAP);
        putGesture(json, "gesture_swipe_down", prefs, LauncherPrefs.GESTURE_SWIPE_DOWN);
        putString(json, "drawer_bg_color", prefs, LauncherPrefs.DRAWER_BG_COLOR);
        putString(json, "folder_bg_color", prefs, LauncherPrefs.FOLDER_BG_COLOR);
        putString(json, "search_provider", prefs, LauncherPrefs.SEARCH_PROVIDER);
        putString(json, "icon_pack", prefs, LauncherPrefs.ICON_PACK);
        putString(json, "font_family", prefs, LauncherPrefs.FONT_FAMILY);
        putString(json, "accent_color", prefs, LauncherPrefs.ACCENT_COLOR);
        putString(json, "drawer_sort_order", prefs, LauncherPrefs.DRAWER_SORT_ORDER);
        putString(json, "dock_bg_color", prefs, LauncherPrefs.DOCK_BG_COLOR);
        putString(json, "search_bar_color", prefs, LauncherPrefs.SEARCH_BAR_COLOR);
        putString(json, "dot_color", prefs, LauncherPrefs.DOT_COLOR);
        putString(json, "page_transition", prefs, LauncherPrefs.PAGE_TRANSITION);
        putString(json, "dark_mode", prefs, LauncherPrefs.DARK_MODE);
        putGesture(json, "gesture_edge_left", prefs, LauncherPrefs.GESTURE_EDGE_LEFT);
        putGesture(json, "gesture_edge_right", prefs, LauncherPrefs.GESTURE_EDGE_RIGHT);
    }

    private static void applyTrailingExtras(JSONObject json, LauncherPrefs prefs) throws JSONException {
        putInt(json, "widget_padding", prefs, LauncherPrefs.WIDGET_PADDING);
        putInt(json, "dock_corner_radius", prefs, LauncherPrefs.DOCK_CORNER_RADIUS);
        putString(json, "app_launch_animation", prefs, LauncherPrefs.APP_LAUNCH_ANIMATION);
        putString(json, "font_weight", prefs, LauncherPrefs.FONT_WEIGHT);
        putString(json, "drawer_animation", prefs, LauncherPrefs.DRAWER_ANIMATION);
    }

    private static void putBool(JSONObject json, String key, LauncherPrefs prefs, ConstantItem<Boolean> prefKey)
            throws JSONException {
        if (json.has(key))
            prefs.put(prefKey, json.getBoolean(key));
    }

    private static void putInt(JSONObject json, String key, LauncherPrefs prefs, ConstantItem<Integer> prefKey)
            throws JSONException {
        if (json.has(key))
            prefs.put(prefKey, json.getInt(key));
    }

    private static void putString(JSONObject json, String key, LauncherPrefs prefs, ConstantItem<String> prefKey)
            throws JSONException {
        if (json.has(key))
            prefs.put(prefKey, json.getString(key));
    }

    private static void putGesture(JSONObject json, String key, LauncherPrefs prefs, ConstantItem<String> prefKey)
            throws JSONException {
        if (json.has(key)) {
            String val = json.getString(key);
            if (isValidGestureHandler(val))
                prefs.put(prefKey, val);
        }
    }
}
