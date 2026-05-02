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
 * File:    bliss/src/foundation/e/bliss/backup/workspace/WorkspaceImporter.java
 * Module:  bliss source-set  (foundation.e.bliss.backup.workspace)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/backup/workspace/):
 *   ├── ContentValuesBuilder.java   — Lawnchair cursor row → ContentValues
 *   ├── FavoritesCursor.java        — column-index registry
 *   ├── IconBlobWriter.java         — DEEP_SHORTCUT icon BLOB carry-across
 *   ├── ItemTypes.java              — ITEM_TYPE_* / CONTAINER_* constants
 *   ├── PostImportLayoutFix.java    — re-pin shortcuts + position re-apply
 *   ├── WidgetRebinder.java         — allocateAppWidgetId + bindAppWidgetIdIfAllowed
 *   └── WorkspaceImporter.java      — top-level orchestrator (this file)  ← THIS FILE
 *
 * Purpose:
 *   Top-level orchestrator for the layout half of the Lawnchair import.
 *   Stages the launcher.db blob to a temp file, opens it read-only, walks
 *   the favorites cursor (ORDER BY container, screen, cellY, cellX —
 *   §5 invariant 2), builds ContentValues per row, INSERTs everything via
 *   a fresh DatabaseHelper bound to the current grid's db file, then runs
 *   PostImportLayoutFix.apply.
 *
 * Consumed by:
 *   - foundation.e.bliss.backup.LawnchairImportHelper  — orchestrator
 *
 * Calls into:
 *   - foundation.e.bliss.backup.workspace.WidgetRebinder
 *   - foundation.e.bliss.backup.workspace.IconBlobWriter
 *   - foundation.e.bliss.backup.workspace.ContentValuesBuilder
 *   - foundation.e.bliss.backup.workspace.PostImportLayoutFix
 *
 * Plan reference: Plans/Migration04/02-importer-decomposition.md §4.7
 */
package foundation.e.bliss.backup.workspace;

import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.UserManager;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.dagger.LauncherComponentProvider;
import com.android.launcher3.model.DatabaseHelper;
import com.android.launcher3.util.Executors;

import foundation.e.bliss.utils.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Top-level orchestrator: launcher.db bytes → BlissLauncher favorites table.
 */
public final class WorkspaceImporter {

    private static final Logger LOG = Logger.tag("LawnchairImport");

    private WorkspaceImporter() {
    }

    /**
     * Imports {@code dbBytes} (a Lawnchair launcher.db SQLite blob) into the
     * BlissLauncher favorites table for the current grid. Returns true when at
     * least one row was inserted.
     */
    public static boolean run(Context context, byte[] dbBytes) {
        File tempDb = null;
        SQLiteDatabase sourceDb = null;
        try {
            tempDb = stageDbToTemp(context, dbBytes);
            sourceDb = SQLiteDatabase.openDatabase(tempDb.getPath(), null,
                    SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);

            if (!hasFavoritesTable(sourceDb)) {
                LOG.w("No favorites table in Lawnchair DB");
                return false;
            }

            List<ContentValues> items = new ArrayList<>();
            int skippedWidgets = readFavorites(context, sourceDb, items);
            if (skippedWidgets > 0) {
                LOG.i(skippedWidgets + " widgets were not restored "
                        + "(see preceding log lines for per-widget reason)");
            }

            if (items.isEmpty()) {
                LOG.i("No importable items in Lawnchair DB");
                return false;
            }

            insertIntoBlissDb(context, items);
            PostImportLayoutFix.apply(context, items);
            LOG.i("Workspace imported: " + items.size() + " items");
            return true;
        } catch (Exception e) {
            LOG.w("Failed to import workspace from Lawnchair DB", e);
            return false;
        } finally {
            closeQuietly(sourceDb);
            deleteTemp(tempDb);
        }
    }

    private static File stageDbToTemp(Context context, byte[] dbBytes) throws java.io.IOException {
        File tempDb = File.createTempFile("lawnchair_import", ".db", context.getCacheDir());
        try (FileOutputStream fos = new FileOutputStream(tempDb)) {
            fos.write(dbBytes);
        }
        return tempDb;
    }

    private static boolean hasFavoritesTable(SQLiteDatabase sourceDb) {
        try (Cursor tables = sourceDb.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='favorites'",
                null)) {
            return tables.moveToFirst();
        }
    }

    private static int readFavorites(Context context, SQLiteDatabase sourceDb, List<ContentValues> items) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int skippedWidgets = 0;
        String query = "SELECT " + String.join(", ", FavoritesCursor.PROJECTION) + " FROM " + FavoritesCursor.TABLE
                + " WHERE " + FavoritesCursor.SELECTION + " ORDER BY " + FavoritesCursor.ORDER_BY;
        try (Cursor cursor = sourceDb.rawQuery(query, null)) {
            FavoritesCursor row = new FavoritesCursor(cursor);
            while (cursor.moveToNext()) {
                skippedWidgets += processRow(context, appWidgetManager, row, items);
            }
        }
        return skippedWidgets;
    }

    private static int processRow(Context context, AppWidgetManager appWidgetManager, FavoritesCursor row,
            List<ContentValues> items) {
        int itemType = row.itemType();
        byte[] iconBlob = row.icon();
        int newAppWidgetId = -1;
        int restoreFlags = 0;
        int skipped = 0;
        if (itemType == ItemTypes.ITEM_TYPE_APPWIDGET) {
            WidgetRebinder.Result rb = WidgetRebinder.rebind(context, appWidgetManager, row.widgetProv());
            if (rb.skipped) {
                return rb.pendingCounted ? 1 : 0;
            }
            newAppWidgetId = rb.newAppWidgetId;
            restoreFlags = rb.restoreFlags;
            if (rb.pendingCounted)
                skipped = 1;
        }
        restoreFlags = IconBlobWriter.decorate(itemType, iconBlob, restoreFlags);
        items.add(ContentValuesBuilder.fromLawnchair(row, newAppWidgetId, restoreFlags));
        return skipped;
    }

    private static void closeQuietly(SQLiteDatabase db) {
        if (db != null) {
            try {
                db.close();
            } catch (Exception ignored) {
                // Best-effort close on import-failure path.
            }
        }
    }

    private static void deleteTemp(File tempDb) {
        if (tempDb == null)
            return;
        try {
            java.nio.file.Files.deleteIfExists(tempDb.toPath());
        } catch (java.io.IOException e) {
            LOG.w("Failed to delete temp DB " + tempDb.getPath() + ": " + e.getMessage());
        }
    }

    /** INSERT all rows into the current grid's BlissLauncher favorites table. */
    private static void insertIntoBlissDb(Context context, List<ContentValues> finalItems) throws Exception {
        Executors.MODEL_EXECUTOR.submit(() -> {
            String tableName = LauncherSettings.Favorites.TABLE_NAME;
            // Derive target db file from GRID_NAME pref directly. After
            // mapWorkspaceGrid has run, GRID_NAME is the target grid; the db
            // file convention is "launcher_<gridName>.db". Falls back to
            // IDP.dbFile for default/unknown grids.
            String gridName = LauncherComponentProvider.get(context).getLauncherPrefs().get(LauncherPrefs.GRID_NAME);
            String targetDbFile;
            if (gridName != null && !gridName.isEmpty()) {
                targetDbFile = "launcher_" + gridName + ".db";
            } else {
                targetDbFile = LauncherAppState.getInstance(context).getInvariantDeviceProfile().dbFile;
            }
            LOG.i("Writing " + finalItems.size() + " items to " + targetDbFile);
            DatabaseHelper helper = new DatabaseHelper(context, targetDbFile,
                    user -> ((UserManager) context.getSystemService(Context.USER_SERVICE)).getSerialNumberForUser(user),
                    () -> {
                    });
            try {
                SQLiteDatabase destDb = helper.getWritableDatabase();
                destDb.beginTransaction();
                try {
                    destDb.delete(tableName, null, null);
                    for (ContentValues cv : finalItems) {
                        destDb.insertWithOnConflict(tableName, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                    }
                    destDb.setTransactionSuccessful();
                    LOG.i("Inserted " + finalItems.size() + " items into " + targetDbFile + "/" + tableName);
                } finally {
                    destDb.endTransaction();
                }
                // DEBUG: dump first 8 workspace items with their cellY positions
                // to confirm the INSERT preserves Lawnchair's sparse layout.
                try (Cursor c = destDb.rawQuery("SELECT _id, screen, cellX, cellY, itemType, title FROM " + tableName
                        + " WHERE container=-100 ORDER BY screen, cellY, cellX", null)) {
                    StringBuilder sb = new StringBuilder("post-INSERT layout: ");
                    int n = 0;
                    while (c.moveToNext() && n < 8) {
                        sb.append("[s=").append(c.getInt(1)).append(",x=").append(c.getInt(2)).append(",y=")
                                .append(c.getInt(3)).append(",t=").append(c.getInt(4)).append(",'")
                                .append(c.getString(5)).append("'] ");
                        n++;
                    }
                    LOG.i(sb.toString());
                }
            } finally {
                helper.close();
            }
        }).get();
    }
}
