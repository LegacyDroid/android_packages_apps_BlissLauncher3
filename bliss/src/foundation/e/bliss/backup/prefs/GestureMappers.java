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
 * File:    bliss/src/foundation/e/bliss/backup/prefs/GestureMappers.java
 * Module:  bliss source-set  (foundation.e.bliss.backup.prefs)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/backup/prefs/):
 *   ├── BasicMappers.java                  — typed primitives
 *   ├── DomainMappers.java                 — theme/shape/hidden-apps/folder-colors/app-name
 *   ├── DrawerFoldersWriter.java           — JSON-list → Room store
 *   ├── GestureMappers.java                — gesture handler JSON → Bliss handler key  ← THIS FILE
 *   ├── PrefMapper.java                    — single-mapper functional interface
 *   ├── PrefMapperRegistry.java            — orchestrator
 *   ├── RecognizedKeys.java                — Lawnchair-key allow-lists
 *   └── WorkspaceGridFontPopupMappers.java — grid/font/popup-order mappers
 *
 * Purpose:
 *   Lifts the Lawnchair gesture-handler JSON parsing out of DomainMappers
 *   so that file stays under the Migration04 300-line ceiling. Gesture
 *   handlers are stored in Lawnchair as JSON ({"type":"sleep"}); we map
 *   each known type to BlissLauncher's flat handler-name string.
 *
 * Consumed by:
 *   - foundation.e.bliss.backup.prefs.PrefMapperRegistry
 *
 * Plan reference: Plans/Migration04/02-importer-decomposition.md §4 (lifted from lines 1197–1245)
 */
package foundation.e.bliss.backup.prefs;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;

import org.json.JSONObject;

import java.util.Map;

/** Lawnchair gesture-handler JSON → BlissLauncher handler-name string. */
public final class GestureMappers {

    private GestureMappers() {
    }

    public static int mapGesture(Map<String, Object> src, LauncherPrefs prefs, String srcKey,
            ConstantItem<String> destItem) {
        Object val = src.get(srcKey);
        if (!(val instanceof String))
            return 0;
        String json = (String) val;
        try {
            JSONObject obj = new JSONObject(json);
            String type = obj.optString("type", "noOp");
            String mapped = mapGestureType(type);
            if (mapped != null) {
                prefs.put(destItem, mapped);
                return 1;
            }
        } catch (Exception e) {
            // Not valid JSON — try using the raw string as a handler name
            String mapped = mapGestureType(json);
            if (mapped != null) {
                prefs.put(destItem, mapped);
                return 1;
            }
        }
        return 0;
    }

    static String mapGestureType(String lawnchairType) {
        if (lawnchairType == null)
            return "none";
        switch (lawnchairType) {
            case "noOp" :
                return "none";
            case "sleep" :
                return "sleep";
            case "openNotificationdata" :
                return "notifications";
            case "openQuickSettings" :
                return "quick_settings";
            case "openAppDrawer" :
                return "app_drawer";
            case "openAppSearch", "openSearch" :
                return "app_search";
            case "openAssistant" :
                return "assistant";
            case "recents" :
                return "recents";
            default :
                return null;
        }
    }
}
