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
 * File:    bliss/src/foundation/e/bliss/backup/blissformat/BlissPrefsSerializer.java
 * Module:  bliss source-set  (foundation.e.bliss.backup.blissformat)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/backup/blissformat/):
 *   ├── BlissBackupZip.java            — ZIP envelope read/write
 *   ├── BlissPrefsSerializer.java      — LauncherPrefs → prefs.json (this file)  ← THIS FILE
 *   └── BlissPrefsDeserializer.java    — prefs.json → LauncherPrefs
 *
 * Purpose:
 *   Owns the write-half of the Bliss backup pref schema. Reads every
 *   user-tunable value from LauncherPrefs and emits a stable JSON object.
 *   Output ordering, key names and value types are byte-identical to the
 *   pre-Migration05 monolithic BackupRestoreHelper.createBackup so existing
 *   .bliss backups stay restorable; a forthcoming round-trip test pins this
 *   guarantee.
 *
 * Consumed by:
 *   - foundation.e.bliss.backup.BackupRestoreHelper  — façade orchestrator
 *
 * Plan reference: Plans/Migration05/02-backup-restore-decomp.md (speculative)
 */
package foundation.e.bliss.backup.blissformat;

import com.android.launcher3.LauncherPrefs;

import org.json.JSONException;
import org.json.JSONObject;

/** Serialises LauncherPrefs into the prefs.json payload of a .bliss backup. */
public final class BlissPrefsSerializer {

    private BlissPrefsSerializer() {
    }

    /**
     * Build the prefs.json string. Returns {@code null} if any pref read or JSON
     * write throws — the caller treats that as a failed backup.
     */
    public static String toJson(LauncherPrefs prefs) {
        JSONObject json = new JSONObject();
        try {
            // Boolean preferences
            json.put("show_home_labels", prefs.get(LauncherPrefs.SHOW_HOME_LABELS));
            json.put("show_drawer_labels", prefs.get(LauncherPrefs.SHOW_DRAWER_LABELS));
            json.put("show_dock", prefs.get(LauncherPrefs.SHOW_DOCK));
            json.put("show_folder_labels", prefs.get(LauncherPrefs.SHOW_FOLDER_LABELS));
            json.put("wallpaper_scrolling", prefs.get(LauncherPrefs.WALLPAPER_SCROLLING));
            json.put("infinite_scrolling", prefs.get(LauncherPrefs.INFINITE_SCROLLING));
            json.put("show_status_bar", prefs.get(LauncherPrefs.SHOW_STATUS_BAR));
            json.put("force_widget_resize", prefs.get(LauncherPrefs.FORCE_WIDGET_RESIZE));
            json.put("widget_rounded_corners", prefs.get(LauncherPrefs.WIDGET_ROUNDED_CORNERS));
            json.put("widget_unlimited_size", prefs.get(LauncherPrefs.WIDGET_UNLIMITED_SIZE));
            json.put("show_at_a_glance", prefs.get(LauncherPrefs.SHOW_AT_A_GLANCE));
            json.put("hide_drawer_search", prefs.get(LauncherPrefs.HIDE_DRAWER_SEARCH));
            json.put("auto_show_keyboard", prefs.get(LauncherPrefs.AUTO_SHOW_KEYBOARD));
            json.put("fuzzy_search", prefs.get(LauncherPrefs.FUZZY_SEARCH));
            json.put("show_calculator", prefs.get(LauncherPrefs.SHOW_CALCULATOR));
            json.put("drawer_list_view", prefs.get(LauncherPrefs.DRAWER_LIST_VIEW));
            json.put("contact_search", prefs.get(LauncherPrefs.CONTACT_SEARCH));
            json.put("shortcut_search", prefs.get(LauncherPrefs.SHORTCUT_SEARCH));
            json.put("search_bar_bottom", prefs.get(LauncherPrefs.SEARCH_BAR_BOTTOM));
            json.put("dock_labels", prefs.get(LauncherPrefs.DOCK_LABELS));
            json.put("keyboard_auto_hide", prefs.get(LauncherPrefs.KEYBOARD_AUTO_HIDE));
            json.put("folder_badges", prefs.get(LauncherPrefs.FOLDER_BADGES));

            // Integer preferences
            json.put("icon_size_factor", prefs.get(LauncherPrefs.ICON_SIZE_FACTOR));
            json.put("drawer_icon_size_factor", prefs.get(LauncherPrefs.DRAWER_ICON_SIZE_FACTOR));
            json.put("home_label_size_factor", prefs.get(LauncherPrefs.HOME_LABEL_SIZE_FACTOR));
            json.put("drawer_label_size_factor", prefs.get(LauncherPrefs.DRAWER_LABEL_SIZE_FACTOR));
            json.put("dock_icon_count", prefs.get(LauncherPrefs.DOCK_ICON_COUNT));
            json.put("blur_intensity", prefs.get(LauncherPrefs.BLUR_INTENSITY));
            json.put("drawer_columns", prefs.get(LauncherPrefs.DRAWER_COLUMNS));
            json.put("folder_columns", prefs.get(LauncherPrefs.FOLDER_COLUMNS));
            json.put("drawer_opacity", prefs.get(LauncherPrefs.DRAWER_OPACITY));
            json.put("folder_bg_opacity", prefs.get(LauncherPrefs.FOLDER_BG_OPACITY));
            json.put("folder_preview_bg_opacity", prefs.get(LauncherPrefs.FOLDER_PREVIEW_BG_OPACITY));
            json.put("search_result_count", prefs.get(LauncherPrefs.SEARCH_RESULT_COUNT));
            json.put("dock_bg_opacity", prefs.get(LauncherPrefs.DOCK_BG_OPACITY));
            json.put("search_bar_radius", prefs.get(LauncherPrefs.SEARCH_BAR_RADIUS));

            // String preferences
            json.put("gesture_double_tap", prefs.get(LauncherPrefs.GESTURE_DOUBLE_TAP));
            json.put("gesture_swipe_down", prefs.get(LauncherPrefs.GESTURE_SWIPE_DOWN));
            json.put("drawer_bg_color", prefs.get(LauncherPrefs.DRAWER_BG_COLOR));
            json.put("folder_bg_color", prefs.get(LauncherPrefs.FOLDER_BG_COLOR));
            json.put("search_provider", prefs.get(LauncherPrefs.SEARCH_PROVIDER));
            json.put("icon_pack", prefs.get(LauncherPrefs.ICON_PACK));
            json.put("font_family", prefs.get(LauncherPrefs.FONT_FAMILY));
            json.put("accent_color", prefs.get(LauncherPrefs.ACCENT_COLOR));
            json.put("drawer_sort_order", prefs.get(LauncherPrefs.DRAWER_SORT_ORDER));
            json.put("dock_bg_color", prefs.get(LauncherPrefs.DOCK_BG_COLOR));
            json.put("search_bar_color", prefs.get(LauncherPrefs.SEARCH_BAR_COLOR));
            json.put("dot_color", prefs.get(LauncherPrefs.DOT_COLOR));
            json.put("page_transition", prefs.get(LauncherPrefs.PAGE_TRANSITION));
            json.put("dark_mode", prefs.get(LauncherPrefs.DARK_MODE));
            json.put("gesture_edge_left", prefs.get(LauncherPrefs.GESTURE_EDGE_LEFT));
            json.put("gesture_edge_right", prefs.get(LauncherPrefs.GESTURE_EDGE_RIGHT));

            // Integer preferences (additional)
            json.put("widget_padding", prefs.get(LauncherPrefs.WIDGET_PADDING));
            json.put("dock_corner_radius", prefs.get(LauncherPrefs.DOCK_CORNER_RADIUS));

            // String preferences (additional)
            json.put("app_launch_animation", prefs.get(LauncherPrefs.APP_LAUNCH_ANIMATION));
            json.put("font_weight", prefs.get(LauncherPrefs.FONT_WEIGHT));
            json.put("drawer_animation", prefs.get(LauncherPrefs.DRAWER_ANIMATION));
        } catch (JSONException | RuntimeException e) {
            return null;
        }
        return json.toString();
    }
}
