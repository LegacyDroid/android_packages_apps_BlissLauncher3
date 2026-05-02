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
 * File:    bliss/src/foundation/e/bliss/backup/prefs/BasicMappers.java
 * Module:  bliss source-set  (foundation.e.bliss.backup.prefs)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/backup/prefs/):
 *   ├── BasicMappers.java          — typed primitives (this file)  ← THIS FILE
 *   ├── DomainMappers.java         — domain-specific mappers
 *   ├── DrawerFoldersWriter.java   — JSON-list → Room store
 *   ├── PrefMapper.java            — single-mapper functional interface
 *   └── PrefMapperRegistry.java    — orchestrator
 *
 * Purpose:
 *   Static typed-mapper helpers used by PrefMapperRegistry to translate a
 *   single Lawnchair pref into a single BlissLauncher pref. Bool/int/string
 *   pass-through, plus the float→int "percent" and "opacity" conversions
 *   the original importer needs (Lawnchair stores 0.0-2.0 floats, we store
 *   0-200 ints; for opacity it's 0.0-1.0 → 0-100). Lifted verbatim from
 *   LawnchairImportHelper.mapBoolean/Int/String/FloatToInt*.
 *
 * Consumed by:
 *   - foundation.e.bliss.backup.prefs.PrefMapperRegistry
 *   - foundation.e.bliss.backup.prefs.DomainMappers
 *
 * Plan reference: Plans/Migration04/02-importer-decomposition.md §4 (lifted from lines 984–1145)
 */
package foundation.e.bliss.backup.prefs;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;

import java.util.Map;

/** Typed-mapper helpers (boolean/int/string/float→int percent / opacity). */
public final class BasicMappers {

    private BasicMappers() {
    }

    public static boolean mapBoolean(Map<String, Object> src, LauncherPrefs prefs, String srcKey,
            ConstantItem<Boolean> destItem) {
        Object val = src.get(srcKey);
        if (val instanceof Boolean) {
            prefs.put(destItem, (Boolean) val);
            return true;
        }
        return false;
    }

    public static boolean mapInt(Map<String, Object> src, LauncherPrefs prefs, String srcKey,
            ConstantItem<Integer> destItem) {
        Object val = src.get(srcKey);
        if (val instanceof Integer) {
            prefs.put(destItem, (Integer) val);
            return true;
        } else if (val instanceof Long) {
            prefs.put(destItem, ((Long) val).intValue());
            return true;
        }
        return false;
    }

    public static boolean mapString(Map<String, Object> src, LauncherPrefs prefs, String srcKey,
            ConstantItem<String> destItem) {
        Object val = src.get(srcKey);
        if (val instanceof String && !((String) val).isEmpty()) {
            prefs.put(destItem, (String) val);
            return true;
        }
        return false;
    }

    /**
     * Map a float (0.0-2.0) from Lawnchair to an int (0-200) for BlissLauncher3.
     */
    public static int mapFloatToIntPercent(Map<String, Object> src, LauncherPrefs prefs, String[] srcKeys,
            ConstantItem<Integer> destItem) {
        for (String key : srcKeys) {
            Object val = src.get(key);
            if (val instanceof Float) {
                prefs.put(destItem, Math.round((Float) val * 100));
                return 1;
            } else if (val instanceof Double) {
                prefs.put(destItem, (int) Math.round((Double) val * 100));
                return 1;
            }
        }
        return 0;
    }

    /**
     * Variant of {@link #mapFloatToIntPercent} clamped to a configurable maximum.
     */
    public static int mapFloatToIntPercentClamped(Map<String, Object> src, LauncherPrefs prefs, String srcKey,
            ConstantItem<Integer> destItem, int maxPercent) {
        Object val = src.get(srcKey);
        if (!(val instanceof Float) && !(val instanceof Double))
            return 0;
        double f = val instanceof Float fv ? fv.doubleValue() : (Double) val;
        int pct = (int) Math.round(f * 100);
        if (pct < 0)
            pct = 0;
        if (pct > maxPercent)
            pct = maxPercent;
        prefs.put(destItem, pct);
        return 1;
    }

    /**
     * Map a float (0.0-1.0) from Lawnchair to an int (0-100) for BlissLauncher3.
     */
    public static int mapFloatToIntOpacity(Map<String, Object> src, LauncherPrefs prefs, String srcKey,
            ConstantItem<Integer> destItem) {
        Object val = src.get(srcKey);
        if (val instanceof Float) {
            prefs.put(destItem, Math.round((Float) val * 100));
            return 1;
        } else if (val instanceof Double) {
            prefs.put(destItem, (int) Math.round((Double) val * 100));
            return 1;
        }
        return 0;
    }
}
