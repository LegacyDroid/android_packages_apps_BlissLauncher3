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
 * File:    bliss/src/foundation/e/bliss/backup/prefs/PrefMapper.java
 * Module:  bliss source-set  (foundation.e.bliss.backup.prefs)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/backup/prefs/):
 *   ├── BasicMappers.java          — typed primitives (boolean/int/string/float→percent)
 *   ├── DomainMappers.java         — domain-specific mappers (darkMode, hotseat, icon shape, …)
 *   ├── DrawerFoldersWriter.java   — JSON-list → Room store; called from DomainMappers
 *   ├── PrefMapper.java            — single-mapper functional interface  ← THIS FILE
 *   └── PrefMapperRegistry.java    — orchestrator; mirrors the legacy applyMappedPreferences
 *
 * Purpose:
 *   Functional-interface seam for a single pref-mapping unit. Reserved for
 *   Migration05 when individual mappers will be unit-testable in isolation.
 *   Phase 02 itself does NOT migrate every branch into a discrete mapper —
 *   the registry's body is the single point of orchestration with verbatim
 *   ordering preserved. This interface exists so the seam is in place.
 *
 * Plan reference: Plans/Migration04/02-importer-decomposition.md §4.3
 */
package foundation.e.bliss.backup.prefs;

import android.content.Context;

import com.android.launcher3.LauncherPrefs;

import java.util.Map;

/**
 * A single pref-mapping unit. Implementations may write zero, one, or several
 * BlissLauncher prefs. The return value is the count of writes.
 */
public interface PrefMapper {
    int apply(Context ctx, Map<String, Object> src, LauncherPrefs prefs);
}
