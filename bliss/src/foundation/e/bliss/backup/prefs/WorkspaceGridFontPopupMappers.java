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
 * File:    bliss/src/foundation/e/bliss/backup/prefs/WorkspaceGridFontPopupMappers.java
 * Module:  bliss source-set  (foundation.e.bliss.backup.prefs)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/backup/prefs/):
 *   ├── BasicMappers.java                  — typed primitives
 *   ├── DomainMappers.java                 — theme/shape/hidden-apps/folder-colors/app-name
 *   ├── DrawerFoldersWriter.java           — JSON-list → Room store
 *   ├── GestureMappers.java                — gesture handler JSON → Bliss handler key
 *   ├── PrefMapper.java                    — single-mapper functional interface
 *   ├── PrefMapperRegistry.java            — orchestrator
 *   ├── RecognizedKeys.java                — Lawnchair-key allow-lists
 *   └── WorkspaceGridFontPopupMappers.java — grid/font/popup-order mappers (this file) ← THIS FILE
 *
 * Purpose:
 *   Holds three of the heavier domain-specific mappers — workspace grid,
 *   workspace font, and home long-press popup order. Lifted out of
 *   DomainMappers to keep that file under the 300-line ceiling.
 *
 *   mapWorkspaceGrid is special: it pre-writes DeviceGridState before the
 *   GRID_NAME pref switch so GridSizeMigrationDBController skips migration
 *   and the freshly-imported favorites table survives.
 *
 * Consumed by:
 *   - foundation.e.bliss.backup.prefs.PrefMapperRegistry
 *
 * Calls into:
 *   - com.android.launcher3.model.DeviceGridState
 *
 * Plan reference: Plans/Migration04/02-importer-decomposition.md §4 (lifted from lines 1361–1512)
 */
package foundation.e.bliss.backup.prefs;

import android.content.Context;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.model.DeviceGridState;

import foundation.e.bliss.utils.Logger;

import org.json.JSONObject;

import java.util.Locale;
import java.util.Map;

/** Heavier domain-specific mappers: workspace grid / font / popup order. */
public final class WorkspaceGridFontPopupMappers {

    private static final Logger LOG = Logger.tag("LawnchairImport");

    /**
     * Available BlissLauncher grid names (must match res/xml/device_profiles.xml).
     */
    private static final String[] GRID_NAMES = {"2_by_2", "3_by_3", "4_by_5", "5_by_5", "4_by_6", "5_by_7", "6_by_6"};

    private WorkspaceGridFontPopupMappers() {
    }

    /**
     * Map Lawnchair's workspace columns × rows to the closest BlissLauncher grid;
     * pre-writes DeviceGridState src prefs so migration is suppressed.
     */
    public static int mapWorkspaceGrid(Map<String, Object> src, LauncherPrefs prefs, Context context) {
        Object colsVal = src.get("pref_workspaceColumns");
        Object rowsVal = src.get("pref_workspaceRows");
        if (!(colsVal instanceof Integer) || !(rowsVal instanceof Integer))
            return 0;
        int cols = (Integer) colsVal;
        int rows = (Integer) rowsVal;
        if (cols < 2 || rows < 2)
            return 0;

        String exact = cols + "_by_" + rows;
        String chosen = null;
        for (String name : GRID_NAMES) {
            if (name.equals(exact)) {
                chosen = name;
                break;
            }
        }
        if (chosen == null) {
            int bestScore = Integer.MAX_VALUE;
            for (String name : GRID_NAMES) {
                String[] parts = name.split("_by_");
                int gc = Integer.parseInt(parts[0]);
                int gr = Integer.parseInt(parts[1]);
                int score = Math.abs(gc - cols) * 10 + Math.abs(gr - rows);
                if (score < bestScore) {
                    bestScore = score;
                    chosen = name;
                }
            }
        }
        if (chosen == null)
            return 0;

        try {
            String[] parts = chosen.split("_by_");
            int targetCols = Integer.parseInt(parts[0]);
            int targetRows = Integer.parseInt(parts[1]);
            String targetDbFile = "launcher_" + chosen + ".db";
            int targetHotseat = targetCols;
            DeviceGridState target = new DeviceGridState(targetCols, targetRows, targetHotseat, /* deviceType */ 0,
                    targetDbFile, /* gridType */ 0);
            target.writeToPrefs(context);
            LOG.i("Pre-set DeviceGridState src to " + targetDbFile + " before grid switch (suppresses migration)");
        } catch (Throwable t) {
            LOG.w("Could not pre-set DeviceGridState: " + t.getMessage());
        }

        prefs.putSync(LauncherPrefs.GRID_NAME.to(chosen));
        LOG.i("Mapped workspace grid " + cols + "x" + rows + " -> " + chosen);
        return 1;
    }

    /** Lawnchair JSON font descriptor → BlissLauncher FONT_FAMILY enum. */
    public static int mapWorkspaceFont(Map<String, Object> src, LauncherPrefs prefs) {
        Object val = src.get("pref_workspaceFont");
        if (val == null)
            val = src.get("workspace_font");
        if (!(val instanceof String))
            return 0;
        String raw = (String) val;
        String family;
        try {
            JSONObject obj = new JSONObject(raw);
            family = obj.optString("family", "");
        } catch (Exception e) {
            family = raw;
        }
        if (family.isEmpty())
            return 0;
        String lower = family.toLowerCase(Locale.ROOT);
        String mapped;
        if (lower.contains("monospace") || lower.contains("source code") || lower.contains("cousine")
                || lower.contains("mono")) {
            mapped = "monospace";
        } else if (lower.contains("serif") && !lower.contains("sans")) {
            mapped = "serif";
        } else if (lower.contains("cursive") || lower.contains("script") || lower.contains("hand")) {
            mapped = "cursive";
        } else if (lower.contains("system") || lower.contains("default")) {
            mapped = "default";
        } else {
            mapped = "sans-serif";
        }
        prefs.put(LauncherPrefs.FONT_FAMILY, mapped);
        LOG.i("Mapped workspace font \"" + family + "\" -> " + mapped);
        return 1;
    }

    /**
     * Lawnchair pipe-separated launcher_popup_order → comma-separated
     * HOME_POPUP_ORDER.
     */
    public static int mapPopupOrder(Map<String, Object> src, LauncherPrefs prefs) {
        Object val = src.get("launcher_popup_order");
        if (!(val instanceof String))
            return 0;
        String raw = (String) val;
        StringBuilder out = new StringBuilder();
        for (String tokenRaw : raw.split("\\|")) {
            String token = tokenRaw.trim();
            if (token.isEmpty() || !token.startsWith("+"))
                continue;
            String mapped = mapPopupToken(token.substring(1));
            if (mapped == null)
                continue;
            if (out.length() > 0)
                out.append(',');
            out.append(mapped);
        }
        if (out.length() == 0)
            return 0;
        prefs.put(LauncherPrefs.HOME_POPUP_ORDER, out.toString());
        LOG.i("Mapped popup_order \"" + raw + "\" -> " + out);
        return 1;
    }

    private static String mapPopupToken(String lawnchairToken) {
        switch (lawnchairToken) {
            case "wallpaper" :
                return "wallpaper";
            case "widgets" :
                return "widgets";
            case "edit_mode" :
                return "edit_mode";
            case "home_settings" :
                return "settings";
            case "lock" :
                return "lock";
            case "sys_settings" :
                return "sys_settings";
            case "carousel" :
            default :
                return null;
        }
    }
}
