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
 * File:    bliss/src/foundation/e/bliss/backup/workspace/ContentValuesBuilder.java
 * Module:  bliss source-set  (foundation.e.bliss.backup.workspace)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/backup/workspace/):
 *   ├── ContentValuesBuilder.java   — Lawnchair cursor row → ContentValues  ← THIS FILE
 *   ├── FavoritesCursor.java        — column-index registry
 *   ├── IconBlobWriter.java         — DEEP_SHORTCUT icon BLOB carry-across
 *   ├── ItemTypes.java              — ITEM_TYPE_* / CONTAINER_* constants
 *   ├── PostImportLayoutFix.java    — re-pin shortcuts + position re-apply
 *   ├── WidgetRebinder.java         — allocateAppWidgetId + bindAppWidgetIdIfAllowed
 *   └── WorkspaceImporter.java      — orchestrator
 *
 * Purpose:
 *   Lifts a Lawnchair favorites row (read via FavoritesCursor) into a
 *   ContentValues ready for INSERT into BlissLauncher's favorites table.
 *   Preserves the §5 invariant of identical insertion order — the
 *   put()-call sequence here is a verbatim copy of the original
 *   importWorkspaceDirectly body.
 *
 * Consumed by:
 *   - foundation.e.bliss.backup.workspace.WorkspaceImporter
 *
 * Plan reference: Plans/Migration04/02-importer-decomposition.md §4.7 + §5 invariant 2
 */
package foundation.e.bliss.backup.workspace;

import android.content.ContentValues;

/**
 * Build a Bliss-favorites ContentValues row from a FavoritesCursor + widget
 * bind result.
 */
public final class ContentValuesBuilder {

    private ContentValuesBuilder() {
    }

    /** Build the ContentValues row. Insertion order matches the legacy importer. */
    public static ContentValues fromLawnchair(FavoritesCursor r, int newAppWidgetId, int restoreFlags) {
        ContentValues cv = new ContentValues();
        cv.put("_id", r.id());
        cv.put("title", r.title());
        cv.put("intent", r.intent());
        cv.put("container", r.container());
        cv.put("screen", r.screen());
        cv.put("cellX", r.cellX());
        cv.put("cellY", r.cellY());
        cv.put("spanX", r.spanX());
        cv.put("spanY", r.spanY());
        cv.put("itemType", r.itemType());
        cv.put("appWidgetProvider", r.widgetProv());
        cv.put("rank", r.rank());
        cv.put("appWidgetId", newAppWidgetId);
        cv.put("modified", System.currentTimeMillis());
        cv.put("restored", restoreFlags);
        cv.put("options", 0);
        cv.put("appWidgetSource", -1);
        byte[] iconBlob = r.icon();
        if (iconBlob != null && iconBlob.length > 0) {
            cv.put("icon", iconBlob);
        }
        return cv;
    }
}
