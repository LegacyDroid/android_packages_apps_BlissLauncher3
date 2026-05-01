/*
 * Copyright (C) 2025 MURENA SAS
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
 *
 */
/*
 * File:    bliss-prefs/src/main/kotlin/foundation/e/bliss/preferences/BlissPrefs.kt
 * Module:  :bliss-prefs  (foundation.e.bliss.preferences)
 * Role:    RELOCATED
 *
 * Tree (foundation/e/bliss/preferences/):
 *   ├── BlissPrefs.kt           — registry of every Bliss pref-key string  ← THIS FILE
 *   └── BlissPrefSchema.kt      — reflection-derived ALL_KEYS / INTERNAL_KEYS sets + XML self-test
 *
 * Purpose:
 *   Single source of truth for every "pref_*" SharedPreferences key the
 *   launcher reads or writes. Migration04 §03 hoists the last raw-string
 *   key sites (LauncherPrefs.kt + SettingsActivity.java) into this object
 *   so a key string is declared exactly once. Keys are grouped into named
 *   `// region`s so the file scans cleanly at 119+ entries; the grouping
 *   is purely cosmetic and informs the future per-region SettingsFragment
 *   split (Plan §04). Migration04 §09 relocates this file from the
 *   `bliss/` source-set into the leaf `:bliss-prefs` Gradle module so
 *   the registry is physically isolated from any Android-Context retention.
 *
 * Consumed by:
 *   - com.android.launcher3.LauncherPrefs                 — every backedUpItem(BlissPrefs.PREF_…)
 *   - com.android.launcher3.settings.SettingsActivity     — findPreference / equals call sites
 *   - foundation.e.bliss.preferences.BlissPrefSchema      — reflection over declaredFields
 *   - foundation.e.bliss.* (numerous read sites)          — see grep PREF_ for full list
 *
 * Plan reference: Plans/Migration04/03-prefs-registry.md §3, §5, §6
 *                 Plans/Migration04/09-build-and-rollout.md §2.1
 */
package foundation.e.bliss.preferences

object BlissPrefs {
    // region — Home & layout
    const val PREF_SINGLE_LAYER_MODE = "pref_single_layer"
    const val PREF_PRESERVE_LAYOUT_GAPS = "pref_preserve_layout_gaps"
    const val PREF_GRID_FOLDER_PREVIEW = "pref_grid_folder_preview"
    const val PREF_INFINITE_SCROLLING = "pref_infinite_scrolling"
    const val PREF_GRID_SIZE = "pref_grid_size"
    const val PREF_ICON_BADGING = "pref_icon_badging"
    const val PREF_SHOW_HOME_LABELS = "pref_show_home_labels"
    const val PREF_HOME_LABEL_COLOR_MODE = "pref_home_label_color_mode"
    const val PREF_HOME_LABEL_SIZE_FACTOR = "pref_home_label_size_factor"
    const val PREF_HOME_POPUP_ORDER = "pref_home_popup_order"
    const val PREF_ADD_ICON_TO_HOME = "pref_add_icon_to_home"
    const val PREF_PAGE_INDICATOR_HEIGHT_FACTOR = "pref_page_indicator_height_factor"
    const val PREF_WORKSPACE_TOP_PADDING_FACTOR = "pref_workspace_top_padding_factor"
    const val PREF_WORKSPACE_BOTTOM_PADDING_FACTOR = "pref_workspace_bottom_padding_factor"
    const val PREF_WORKSPACE_LOCK = "pref_workspace_lock"
    const val PREF_PAGE_TRANSITION = "pref_page_transition"
    const val PREF_WALLPAPER_SCROLLING = "pref_wallpaper_scrolling"
    const val PREF_WALLPAPER_DEPTH_EFFECT = "pref_wallpaper_depth_effect"
    const val PREF_DARK_MODE = "pref_dark_mode"
    const val PREF_DARK_STATUS_BAR = "pref_dark_status_bar"
    const val PREF_SHOW_STATUS_BAR = "pref_show_status_bar"
    const val PREF_ALLOW_WIDGET_OVERLAP = "pref_allow_widget_overlap"
    const val PREF_FORCE_WIDGET_RESIZE = "pref_force_widget_resize"
    const val PREF_WIDGET_ROUNDED_CORNERS = "pref_widget_rounded_corners"
    const val PREF_WIDGET_UNLIMITED_SIZE = "pref_widget_unlimited_size"
    const val PREF_WIDGET_PADDING = "pref_widget_padding"
    // endregion

    // region — Dock / hotseat
    const val PREF_SHOW_DOCK = "pref_show_dock"
    const val PREF_DOCK_ICON_COUNT = "pref_dock_icon_count"
    const val PREF_DOCK_LABELS = "pref_dock_labels"
    const val PREF_DOCK_BG_COLOR = "pref_dock_bg_color"
    const val PREF_DOCK_BG_OPACITY = "pref_dock_bg_opacity"
    const val PREF_DOCK_CORNER_RADIUS = "pref_dock_corner_radius"
    const val PREF_DOCK_SEARCH_BAR = "pref_dock_search_bar"
    const val PREF_HOTSEAT_INSET_LEFT = "pref_hotseat_inset_left"
    const val PREF_HOTSEAT_INSET_RIGHT = "pref_hotseat_inset_right"
    const val PREF_HOTSEAT_INSET_TOP = "pref_hotseat_inset_top"
    const val PREF_HOTSEAT_INSET_BOTTOM = "pref_hotseat_inset_bottom"
    const val PREF_HOTSEAT_BOTTOM_FACTOR = "pref_hotseat_bottom_factor"
    // endregion

    // region — Drawer
    const val PREF_DRAWER_COLUMNS = "pref_drawer_columns"
    const val PREF_DRAWER_OPACITY = "pref_drawer_opacity"
    const val PREF_DRAWER_BG_COLOR = "pref_drawer_bg_color"
    const val PREF_DRAWER_HAPTIC = "pref_drawer_haptic"
    const val PREF_DRAWER_CELL_HEIGHT_FACTOR = "pref_drawer_cell_height_factor"
    const val PREF_DRAWER_LEFT_RIGHT_MARGIN_FACTOR = "pref_drawer_left_right_margin_factor"
    const val PREF_DRAWER_SHOW_SCROLLBAR = "pref_drawer_show_scrollbar"
    const val PREF_DRAWER_THEMED_ICONS = "pref_drawer_themed_icons"
    const val PREF_DRAWER_ICON_SIZE_FACTOR = "pref_drawer_icon_size_factor"
    const val PREF_DRAWER_LABEL_SIZE_FACTOR = "pref_drawer_label_size_factor"
    const val PREF_DRAWER_SORT_ORDER = "pref_drawer_sort_order"
    const val PREF_DRAWER_LIST_VIEW = "pref_drawer_list_view"
    const val PREF_DRAWER_ANIMATION = "pref_drawer_animation"
    const val PREF_REMEMBER_DRAWER_POSITION = "pref_remember_drawer_position"
    const val PREF_HIDE_DRAWER_SEARCH = "pref_hide_drawer_search"
    const val PREF_SHOW_DRAWER_LABELS = "pref_show_drawer_labels"
    const val PREF_ENABLE_TWO_LINE_TOGGLE = "pref_enable_two_line_toggle"
    const val PREF_WORK_TAB_BG_COLOR = "pref_work_tab_bg_color"
    // endregion

    // region — Folders
    const val PREF_SHOW_FOLDER_LABELS = "pref_show_folder_labels"
    const val PREF_FOLDER_COLUMNS = "pref_folder_columns"
    const val PREF_FOLDER_BG_COLOR = "pref_folder_bg_color"
    const val PREF_FOLDER_BG_OPACITY = "pref_folder_bg_opacity"
    const val PREF_FOLDER_PREVIEW_BG_OPACITY = "pref_folder_preview_bg_opacity"
    const val PREF_FOLDER_LABEL_SIZE_FACTOR = "pref_folder_label_size_factor"
    const val PREF_FOLDER_BADGES = "pref_folder_badges"
    const val PREF_FOLDER_SPRING_ANIM = "pref_folder_spring_anim"
    const val PREF_DRAWER_FOLDERS_ENABLED = "pref_drawer_folders_enabled"
    const val PREF_DRAWER_FOLDERS_MANAGE = "pref_drawer_folders_manage"
    // endregion

    // region — Icons
    const val PREF_ICON_PACK = "pref_icon_pack"
    const val PREF_ICON_SIZE_FACTOR = "pref_icon_size_factor"
    const val PREF_NOTIF_COUNT = "pref_notif_count"
    const val PREF_WRAP_ADAPTIVE_ICONS = "pref_wrap_adaptive_icons"
    const val PREF_TINT_ICON_PACK_BG = "pref_tint_icon_pack_bg"
    const val PREF_DOT_TEXT_COLOR = "pref_dot_text_color"
    const val PREF_DOT_COLOR = "pref_dot_color"
    const val PREF_CUSTOM_ICON_SHAPE_RADIUS = "pref_custom_icon_shape_radius"
    const val PREF_BLUR_INTENSITY = "pref_blur_intensity"
    const val PREF_APP_LAUNCH_ANIMATION = "pref_app_launch_animation"
    const val PREF_CLOSING_APP_OVERLAY = "pref_closing_app_overlay"
    const val PREF_ICON_SHAPE = "pref_icon_shape"
    const val PREF_THEMED_ICONS = "pref_themed_icons"
    // endregion

    // region — Search backends
    const val PREF_SEARCH_RESULT_COUNT = "pref_search_result_count"
    const val PREF_SEARCH_PROVIDER = "pref_search_provider"
    const val PREF_SEARCH_PEOPLE = "pref_search_people"
    const val PREF_SEARCH_FILES = "pref_search_files"
    const val PREF_SEARCH_AUDIO = "pref_search_audio"
    const val PREF_SEARCH_VISUAL_MEDIA = "pref_search_visual_media"
    const val PREF_SEARCH_SETTINGS = "pref_search_settings"
    const val PREF_SEARCH_STARTPAGE_SUGGESTION = "pref_search_startpage_suggestion"
    const val PREF_WEB_SUGGESTION_URL = "pref_web_suggestion_url"
    const val PREF_WEB_SUGGESTION_NAME = "pref_web_suggestion_name"
    const val PREF_WEB_SUGGESTION_MAX_DELAY = "pref_web_suggestion_max_delay"
    const val PREF_WEB_SUGGESTION_MAX_COUNT = "pref_web_suggestion_max_count"
    const val PREF_HIDDEN_APPS_IN_SEARCH = "pref_hidden_apps_in_search"
    const val PREF_AUTO_SHOW_KEYBOARD = "pref_auto_show_keyboard"
    const val PREF_FUZZY_SEARCH = "pref_fuzzy_search"
    const val PREF_SHOW_CALCULATOR = "pref_show_calculator"
    const val PREF_SHOW_SUGGESTED_APPS = "pref_show_suggested_apps"
    const val PREF_CONTACT_SEARCH = "pref_contact_search"
    const val PREF_SHORTCUT_SEARCH = "pref_shortcut_search"
    const val PREF_SEARCH_BAR_BOTTOM = "pref_search_bar_bottom"
    const val PREF_SEARCH_BAR_COLOR = "pref_search_bar_color"
    const val PREF_SEARCH_BAR_RADIUS = "pref_search_bar_radius"
    const val PREF_KEYBOARD_AUTO_HIDE = "pref_keyboard_auto_hide"
    // endregion

    // region — Smartspace
    const val PREF_SHOW_AT_A_GLANCE = "pref_show_at_a_glance"
    const val PREF_SMARTSPACE_SHOW_TIME = "pref_smartspace_show_time"
    const val PREF_SMARTSPACE_SHOW_DATE = "pref_smartspace_show_date"
    const val PREF_SMARTSPACE_NOW_PLAYING = "pref_smartspace_now_playing"
    const val PREF_SMARTSPACE_TIME_FORMAT = "pref_smartspace_time_format"
    const val PREF_USE_24H_FORMAT = "pref_use_24h_format"
    // endregion

    // region — Gestures
    const val PREF_GESTURE_DOUBLE_TAP = "pref_gesture_double_tap"
    const val PREF_GESTURE_SWIPE_DOWN = "pref_gesture_swipe_down"
    const val PREF_GESTURE_EDGE_LEFT = "pref_gesture_edge_left"
    const val PREF_GESTURE_EDGE_RIGHT = "pref_gesture_edge_right"
    // endregion

    // region — Theme & color
    const val PREF_ACCENT_COLOR = "pref_accent_color"
    const val PREF_FONT_FAMILY = "pref_font_family"
    const val PREF_FONT_WEIGHT = "pref_font_weight"
    // Legacy hex-string mirrors of the structured color prefs (kept for SettingsActivity wiring)
    const val PREF_DOCK_BG_HEX = "pref_dock_bg_hex"
    const val PREF_ACCENT_HEX = "pref_accent_hex"
    const val PREF_DRAWER_BG_HEX = "pref_drawer_bg_hex"
    // endregion

    // region — Action triggers (no stored value; just findPreference targets)
    const val PREF_BACKUP_CREATE = "pref_backup_create"
    const val PREF_BACKUP_RESTORE = "pref_backup_restore"
    const val PREF_LAWNCHAIR_IMPORT = "pref_lawnchair_import"
    const val PREF_RESET_APP_RENAMES = "pref_reset_app_renames"
    const val PREF_RESTART_LAUNCHER = "pref_restart_launcher"
    const val PREF_DEVELOPER_OPTIONS = "pref_developer_options"
    const val PREF_FIXED_LANDSCAPE_MODE = "pref_fixed_landscape_mode"
    // endregion

    // region — Internal / migration markers (not user-facing)
    const val PREF_HIDDEN_APPS = "pref_hidden_apps"
    const val PREF_DRAWER_FOLDERS_MAP = "pref_drawer_folders_map"
    const val PREF_DRAWER_FOLDERS_MIGRATED_V1 = "pref_drawer_folders_migrated_v1"
    const val PREF_FIRST_RUN_LAYOUT_CHOICE_DONE = "pref_first_run_layout_choice_done"
    const val PREF_FIRST_RUN_STEPS_DONE = "pref_first_run_steps_done"
    const val PREF_HOTSEAT_FACTOR_V1_MIGRATED = "pref_hotseat_factor_v1_migrated"
    const val PREF_CLOSING_OVERLAY_V1_MIGRATED = "pref_closing_overlay_v1_migrated"
    const val PREF_APP_NAME_OVERRIDES = "pref_app_name_overrides"
    const val PREF_FOLDER_COLOR_OVERRIDES = "pref_folder_color_overrides"
    const val PREF_RECENT_COLORS = "pref_recent_colors"
    // endregion
}
