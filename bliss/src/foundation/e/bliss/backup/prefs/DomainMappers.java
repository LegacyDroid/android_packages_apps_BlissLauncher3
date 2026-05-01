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
 * File:    bliss/src/foundation/e/bliss/backup/prefs/DomainMappers.java
 * Module:  bliss source-set  (foundation.e.bliss.backup.prefs)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/backup/prefs/):
 *   ├── BasicMappers.java                  — typed primitives
 *   ├── DomainMappers.java                 — theme/shape/hidden/folder/app-name (this file)  ← THIS FILE
 *   ├── DrawerFoldersWriter.java           — JSON-list → Room store
 *   ├── GestureMappers.java                — gesture handler JSON → Bliss handler key
 *   ├── PrefMapper.java                    — single-mapper functional interface
 *   ├── PrefMapperRegistry.java            — orchestrator
 *   ├── RecognizedKeys.java                — Lawnchair-key allow-lists
 *   └── WorkspaceGridFontPopupMappers.java — grid/font/popup-order mappers
 *
 * Purpose:
 *   Domain-specific (non-trivial) Lawnchair-pref mappers that don't fit the
 *   typed-primitive shape in BasicMappers. Each handles a single Lawnchair
 *   concept (dark mode, hotseat, themed icons, accent color, icon shape,
 *   hidden apps, folder colors, app-name overrides). Heavier mappers
 *   (gestures, workspace grid/font, popup order) live in their own files.
 *
 * Consumed by:
 *   - foundation.e.bliss.backup.prefs.PrefMapperRegistry
 *
 * Calls into:
 *   - com.android.launcher3.graphics.ThemeManager     — themed-icons toggle
 *   - com.android.launcher3.shapes.ShapesProvider     — icon-shape keys
 *   - foundation.e.bliss.preferences.AppNameOverrides — invalidate after import
 *
 * Plan reference: Plans/Migration04/02-importer-decomposition.md §4
 */
package foundation.e.bliss.backup.prefs;

import android.content.Context;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.graphics.ThemeManager;
import com.android.launcher3.shapes.ShapesProvider;

import foundation.e.bliss.utils.Logger;

import org.json.JSONObject;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Non-trivial Lawnchair → BlissLauncher pref mappers
 * (theme/shape/hidden/folder/app-name).
 */
public final class DomainMappers {

    private static final Logger LOG = Logger.tag("LawnchairImport");

    private DomainMappers() {
    }

    /**
     * Map Lawnchair's dark mode ("system"→"auto", "light"→"light", "dark"→"dark").
     */
    public static int mapDarkMode(Map<String, Object> src, LauncherPrefs prefs) {
        Object val = src.get("pref_launcherTheme");
        if (val instanceof String) {
            String theme = (String) val;
            String mapped;
            switch (theme) {
                case "light" :
                    mapped = "light";
                    break;
                case "dark" :
                    mapped = "dark";
                    break;
                default :
                    mapped = "auto";
                    break;
            }
            prefs.put(LauncherPrefs.DARK_MODE, mapped);
            return 1;
        }
        return 0;
    }

    /**
     * Map Lawnchair v12+ {@code hotseat_mode} ("disabled"/"normal"/"hidden") to
     * SHOW_DOCK boolean. "disabled"/"hidden"/"off" also forces DOCK_ICON_COUNT=0.
     */
    public static int mapHotseatMode(Map<String, Object> src, LauncherPrefs prefs) {
        Object val = src.get("hotseat_mode");
        if (!(val instanceof String))
            return 0;
        String mode = ((String) val).trim().toLowerCase(Locale.ROOT);
        boolean disabled = "disabled".equals(mode) || "hidden".equals(mode) || "off".equals(mode);
        prefs.put(LauncherPrefs.SHOW_DOCK, !disabled);
        if (disabled)
            prefs.put(LauncherPrefs.DOCK_ICON_COUNT, 0);
        return 1;
    }

    /** Apply Lawnchair's themed-icons toggle to Bliss's ThemeManager. */
    public static int mapThemedIcons(Map<String, Object> src, Context context) {
        Object val = src.get("themed_icons");
        if (val == null)
            val = src.get("pref_themedIcons");
        if (!(val instanceof Boolean))
            return 0;
        try {
            ThemeManager.INSTANCE.get(context).setMonoThemeEnabled((Boolean) val);
            return 1;
        } catch (Exception e) {
            LOG.w("Failed to apply themed_icons: " + e.getMessage());
            return 0;
        }
    }

    /** ARGB int → "#RRGGBB" hex string for ACCENT_COLOR. */
    public static int mapAccentColor(Map<String, Object> src, LauncherPrefs prefs) {
        Object val = src.get("accent_color");
        if (val == null)
            val = src.get("pref_accentColor");
        if (val == null)
            return 0;
        int argb;
        if (val instanceof Integer) {
            argb = (Integer) val;
        } else if (val instanceof Long) {
            argb = (int) ((long) ((Long) val));
        } else if (val instanceof String) {
            String s = ((String) val).trim();
            try {
                argb = (int) Long.parseLong(s.startsWith("#") ? s.substring(1) : s, 16);
            } catch (NumberFormatException e) {
                return 0;
            }
        } else {
            return 0;
        }
        if (argb == 0)
            return 0;
        String hex = String.format("#%06X", argb & 0xFFFFFF);
        prefs.put(LauncherPrefs.ACCENT_COLOR, hex);
        return 1;
    }

    /**
     * Map Lawnchair icon-shape string to a ShapesProvider key + ThemeManager pref.
     */
    public static int mapIconShape(Map<String, Object> src, LauncherPrefs prefs) {
        Object val = src.get("icon_shape");
        if (val == null)
            val = src.get("pref_iconShape");
        if (!(val instanceof String))
            return 0;
        String shape = ((String) val).trim().toLowerCase(Locale.ROOT);
        String mapped;
        switch (shape) {
            case "circle" :
                mapped = ShapesProvider.CIRCLE_KEY;
                break;
            case "square" :
            case "rounded_square" :
            case "roundedsquare" :
                mapped = ShapesProvider.SQUARE_KEY;
                break;
            case "squircle" :
            case "cylinder" :
            case "teardrop" :
            case "cookie" :
            case "four_sided_cookie" :
                mapped = ShapesProvider.FOUR_SIDED_COOKIE_KEY;
                break;
            case "seven_sided_cookie" :
                mapped = ShapesProvider.SEVEN_SIDED_COOKIE_KEY;
                break;
            case "arch" :
                mapped = ShapesProvider.ARCH_KEY;
                break;
            default :
                LOG.i("Unknown Lawnchair icon_shape \"" + shape + "\"; skipping");
                return 0;
        }
        prefs.put(ThemeManager.PREF_ICON_SHAPE, mapped);
        return 1;
    }

    /** Map Lawnchair hidden apps set to BlissLauncher3. */
    @SuppressWarnings("unchecked")
    public static int mapHiddenApps(Map<String, Object> src, LauncherPrefs prefs) {
        Object val = src.get("hidden_apps");
        if (val == null)
            val = src.get("hidden-app-set");
        if (val instanceof Set) {
            Set<String> apps = (Set<String>) val;
            if (!apps.isEmpty()) {
                prefs.put(LauncherPrefs.HIDDEN_APPS, apps);
                return 1;
            }
        }
        return 0;
    }

    /** Map Lawnchair's folder_color (single hex global, or JSON map per-folder). */
    public static int mapFolderColors(Map<String, Object> src, LauncherPrefs prefs) {
        Object val = src.get("folder_color");
        if (val == null)
            val = src.get("folder_colors");
        if (!(val instanceof String))
            return 0;
        String raw = ((String) val).trim();
        if (raw.isEmpty() || "default".equalsIgnoreCase(raw))
            return 0;

        if (raw.startsWith("#") || raw.matches("[0-9A-Fa-f]{6,8}")) {
            String hex = raw.startsWith("#") ? raw : "#" + raw;
            prefs.put(LauncherPrefs.FOLDER_BG_COLOR, hex);
            return 1;
        }
        prefs.put(LauncherPrefs.FOLDER_COLOR_OVERRIDES, raw);
        return 1;
    }

    /**
     * Validate-and-passthrough Lawnchair's pref_appNameMap JSON to
     * APP_NAME_OVERRIDES.
     */
    public static int mapAppNameMap(Map<String, Object> src, LauncherPrefs prefs) {
        Object val = src.get("pref_appNameMap");
        if (!(val instanceof String))
            return 0;
        String raw = (String) val;
        if (raw.trim().isEmpty())
            return 0;
        try {
            new JSONObject(raw);
        } catch (Exception e) {
            LOG.w("pref_appNameMap is not valid JSON; skipping");
            return 0;
        }
        prefs.put(LauncherPrefs.APP_NAME_OVERRIDES, raw);
        try {
            foundation.e.bliss.preferences.AppNameOverrides.invalidate();
        } catch (Throwable ignored) {
        }
        LOG.i("Imported pref_appNameMap (" + raw.length() + " chars)");
        return 1;
    }
}
