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
 * File:    bliss/src/foundation/e/bliss/backup/workspace/FavoritesCursor.java
 * Module:  bliss source-set  (foundation.e.bliss.backup.workspace)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/backup/workspace/):
 *   ├── ContentValuesBuilder.java   — Lawnchair cursor row → ContentValues
 *   ├── FavoritesCursor.java        — column-index registry  ← THIS FILE
 *   ├── IconBlobWriter.java         — DEEP_SHORTCUT icon BLOB carry-across
 *   ├── ItemTypes.java              — ITEM_TYPE_* / CONTAINER_* constants
 *   ├── PostImportLayoutFix.java    — re-pin shortcuts + position re-apply
 *   ├── WidgetRebinder.java         — allocateAppWidgetId + bindAppWidgetIdIfAllowed
 *   └── WorkspaceImporter.java      — orchestrator
 *
 * Purpose:
 *   Single source of truth for the SELECT projection and its column indices
 *   used by WorkspaceImporter. Migration03's PWA-icon fix grew the column
 *   count to 13; an off-by-one would silently corrupt every imported icon,
 *   so all reads go through the typed accessors here.
 *
 * Consumed by:
 *   - foundation.e.bliss.backup.workspace.WorkspaceImporter
 *   - foundation.e.bliss.backup.workspace.ContentValuesBuilder
 *
 * Plan reference: Plans/Migration04/02-importer-decomposition.md §4.5
 */
package foundation.e.bliss.backup.workspace;

import android.database.Cursor;

import java.util.List;

/**
 * Typed accessor over a Lawnchair favorites cursor; centralises column indices.
 */
public final class FavoritesCursor {

    public static final String TABLE = "favorites";

    public static final List<String> PROJECTION = List.of("_id", "title", "intent", "container", "screen", "cellX",
            "cellY", "spanX", "spanY", "itemType", "appWidgetProvider", "rank", "icon");

    public static final String SELECTION = "container IN (-100, -101) OR container >= 0";
    public static final String ORDER_BY = "container, screen, cellY, cellX";

    private final Cursor c;

    public FavoritesCursor(Cursor c) {
        this.c = c;
    }

    public long id() {
        return c.getLong(0);
    }
    public String title() {
        return c.getString(1);
    }
    public String intent() {
        return c.getString(2);
    }
    public int container() {
        return c.getInt(3);
    }
    public int screen() {
        return c.getInt(4);
    }
    public int cellX() {
        return c.getInt(5);
    }
    public int cellY() {
        return c.getInt(6);
    }
    public int spanX() {
        return c.getInt(7);
    }
    public int spanY() {
        return c.getInt(8);
    }
    public int itemType() {
        return c.getInt(9);
    }
    public String widgetProv() {
        return c.getString(10);
    }
    public int rank() {
        return c.getInt(11);
    }
    public byte[] icon() {
        return c.getBlob(12);
    }
}
