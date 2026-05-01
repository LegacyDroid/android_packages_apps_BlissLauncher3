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
 * File:    bliss/src/foundation/e/bliss/backup/workspace/ItemTypes.java
 * Module:  bliss source-set  (foundation.e.bliss.backup.workspace)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/backup/workspace/):
 *   ├── ContentValuesBuilder.java   — Lawnchair cursor row → ContentValues
 *   ├── FavoritesCursor.java        — column-index registry for the SELECT projection
 *   ├── IconBlobWriter.java         — DEEP_SHORTCUT icon BLOB carry-across
 *   ├── ItemTypes.java              — ITEM_TYPE_* / CONTAINER_* constants  ← THIS FILE
 *   ├── PostImportLayoutFix.java    — re-pin shortcuts + position re-apply
 *   ├── WidgetRebinder.java         — allocateAppWidgetId + bindAppWidgetIdIfAllowed
 *   └── WorkspaceImporter.java      — orchestrator
 *
 * Purpose:
 *   Numeric constants for the Lawnchair favorites schema. Lifted from the
 *   former LawnchairImportHelper privates so the workspace package can
 *   reference them by name without re-declaring.
 *
 * Plan reference: Plans/Migration04/02-importer-decomposition.md §4 (lifted from lines 172–178)
 */
package foundation.e.bliss.backup.workspace;

/** Lawnchair favorites schema item-type and container constants. */
public final class ItemTypes {

    public static final int ITEM_TYPE_APPLICATION = 0;
    public static final int ITEM_TYPE_SHORTCUT = 1;
    public static final int ITEM_TYPE_FOLDER = 2;
    public static final int ITEM_TYPE_APPWIDGET = 4;
    public static final int ITEM_TYPE_DEEP_SHORTCUT = 6;
    public static final int CONTAINER_DESKTOP = -100;
    public static final int CONTAINER_HOTSEAT = -101;

    private ItemTypes() {
    }
}
