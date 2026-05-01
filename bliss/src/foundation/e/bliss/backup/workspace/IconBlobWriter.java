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
 * File:    bliss/src/foundation/e/bliss/backup/workspace/IconBlobWriter.java
 * Module:  bliss source-set  (foundation.e.bliss.backup.workspace)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/backup/workspace/):
 *   ├── ContentValuesBuilder.java   — Lawnchair cursor row → ContentValues
 *   ├── FavoritesCursor.java        — column-index registry
 *   ├── IconBlobWriter.java         — DEEP_SHORTCUT icon BLOB carry-across  ← THIS FILE
 *   ├── ItemTypes.java              — ITEM_TYPE_* / CONTAINER_* constants
 *   ├── PostImportLayoutFix.java    — re-pin shortcuts + position re-apply
 *   ├── WidgetRebinder.java         — allocateAppWidgetId + bindAppWidgetIdIfAllowed
 *   └── WorkspaceImporter.java      — orchestrator
 *
 * Purpose:
 *   Encapsulates the FLAG_RESTORED_ICON | FLAG_RESTORE_STARTED bits the
 *   importer ORs into restoreFlags for DEEP_SHORTCUT rows that carry an
 *   icon BLOB (Migration03 fix; preserves the user's original artwork
 *   when the system ShortcutManager has GC'd the dynamic shortcut id).
 *   §5 invariant 3 — preserved verbatim.
 *
 * Consumed by:
 *   - foundation.e.bliss.backup.workspace.WorkspaceImporter
 *
 * Plan reference: Plans/Migration04/02-importer-decomposition.md §5 invariant 3
 */
package foundation.e.bliss.backup.workspace;

import com.android.launcher3.model.data.WorkspaceItemInfo;

/** OR-in DEEP_SHORTCUT restore flags when an icon BLOB is present. */
public final class IconBlobWriter {

    private IconBlobWriter() {
    }

    /**
     * Returns {@code restoreFlags} OR'd with {@code FLAG_RESTORED_ICON |
     * FLAG_RESTORE_STARTED} when the row is a DEEP_SHORTCUT and the icon BLOB is
     * non-empty; otherwise returns {@code restoreFlags} unchanged.
     */
    public static int decorate(int itemType, byte[] iconBlob, int restoreFlags) {
        if (itemType == ItemTypes.ITEM_TYPE_DEEP_SHORTCUT && iconBlob != null && iconBlob.length > 0) {
            restoreFlags |= WorkspaceItemInfo.FLAG_RESTORED_ICON | WorkspaceItemInfo.FLAG_RESTORE_STARTED;
        }
        return restoreFlags;
    }
}
