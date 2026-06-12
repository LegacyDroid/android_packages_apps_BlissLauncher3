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
 * File:    bliss/src/foundation/e/bliss/backup/workspace/PostImportLayoutFix.java
 * Module:  bliss source-set  (foundation.e.bliss.backup.workspace)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/backup/workspace/):
 *   ├── ContentValuesBuilder.java   — Lawnchair cursor row → ContentValues
 *   ├── FavoritesCursor.java        — column-index registry
 *   ├── IconBlobWriter.java         — DEEP_SHORTCUT icon BLOB carry-across
 *   ├── ItemTypes.java              — ITEM_TYPE_* / CONTAINER_* constants
 *   ├── PostImportLayoutFix.java    — post-INSERT pipeline (this file)  ← THIS FILE
 *   ├── WidgetRebinder.java         — allocateAppWidgetId + bindAppWidgetIdIfAllowed
 *   └── WorkspaceImporter.java      — orchestrator
 *
 * Purpose:
 *   Encapsulates the post-INSERT cleanup pipeline that the legacy
 *   importer ran inline (lines 1762–1971 of the pre-decomposition
 *   LawnchairImportHelper). Sequence (preserved verbatim, §5 invariants):
 *     1. Re-pin DEEP_SHORTCUT items via LauncherApps.pinShortcuts.
 *     2. Refresh icon cache (updateIconParams clears the SQLite icon DB).
 *     3. forceReload + 500ms wait + first-pass position re-apply.
 *     4. Schedule second-pass position re-apply at +4 s + final forceReload.
 *     5. Trailing pref writes (PRESERVE_LAYOUT_GAPS, GRID_FOLDER_PREVIEW).
 *
 * Consumed by:
 *   - foundation.e.bliss.backup.workspace.WorkspaceImporter
 *
 * Calls into:
 *   - com.android.launcher3.LauncherAppState
 *   - com.android.launcher3.model.DatabaseHelper
 *
 * Plan reference: Plans/Migration04/02-importer-decomposition.md §4.6 + §5 invariant 5
 */
package foundation.e.bliss.backup.workspace;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.LauncherApps;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.dagger.LauncherComponentProvider;
import com.android.launcher3.model.DatabaseHelper;
import com.android.launcher3.util.Executors;

import foundation.e.bliss.utils.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Post-INSERT pipeline: re-pin shortcuts, refresh icon cache, two-pass position
 * re-apply.
 */
public final class PostImportLayoutFix {

    private static final Logger LOG = Logger.tag("LawnchairImport");

    private static final String COL_CONTAINER = "container";
    private static final String COL_SCREEN = "screen";
    private static final String COL_CELL_X = "cellX";
    private static final String COL_CELL_Y = "cellY";

    private PostImportLayoutFix() {
    }

    /**
     * Run the post-INSERT pipeline. Called from
     * {@link WorkspaceImporter#run(Context, byte[])} after the favorites INSERT has
     * completed and the source DB has been closed.
     */
    public static void apply(Context context, List<ContentValues> finalItems) {
        repinDeepShortcuts(context, finalItems);
        refreshIconCache(context);

        final Map<Long, int[]> originalPositions = captureOriginalPositions(finalItems);

        final java.util.concurrent.CountDownLatch bindLatch = new java.util.concurrent.CountDownLatch(1);
        final foundation.e.bliss.LauncherAppMonitor monitor = foundation.e.bliss.LauncherAppMonitor
                .getInstance(context);
        final foundation.e.bliss.LauncherAppMonitorCallback bindCb = new foundation.e.bliss.LauncherAppMonitorCallback() {
            @Override
            public void onLauncherWorkspaceBindingFinish() {
                bindLatch.countDown();
            }
        };
        monitor.registerCallback(bindCb);

        try {
            Executors.MAIN_EXECUTOR.submit(() -> LauncherAppState.getInstance(context).getModel().forceReload()).get();
        } catch (Exception e) {
            LOG.w("forceReload submit failed: " + e.getMessage());
        }

        // Wait for the loader's workspace-bind to finish, with a generous timeout
        // as a safety net. 5s covers slow devices; the latch normally fires in <1s.
        try {
            boolean ok = bindLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);
            if (!ok) {
                LOG.w("PostImportLayoutFix: bind-complete latch timed out at 5s; continuing.");
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } finally {
            monitor.unregisterCallback(bindCb);
        }

        try {
            Executors.MODEL_EXECUTOR.submit(() -> applyPositionFixupFirstPass(context, originalPositions)).get();
        } catch (Exception e) {
            LOG.w("first-pass position fixup submit failed: " + e.getMessage());
        }

        // Belt-and-braces: schedule a second pass after a short delay to cover
        // any late-binding loader work. Once telemetry confirms the latch is
        // sufficient, gate this behind LauncherPrefs.POSTIMPORT_SECOND_PASS.
        Executors.MAIN_EXECUTOR.getHandler().postDelayed(
                () -> Executors.MODEL_EXECUTOR.execute(() -> applyPositionFixupSecondPass(context, originalPositions)),
                1000L); // tightened from 4000ms — latch already covered the slow path

        LauncherPrefs prefs = LauncherComponentProvider.get(context).getLauncherPrefs();
        prefs.put(LauncherPrefs.PRESERVE_LAYOUT_GAPS, true);
        prefs.put(LauncherPrefs.GRID_FOLDER_PREVIEW, true);
    }

    private static void repinDeepShortcuts(Context context, List<ContentValues> finalItems) {
        Map<String, List<String>> shortcutIdsByPkg = collectDeepShortcutIds(finalItems);
        if (shortcutIdsByPkg.isEmpty())
            return;
        pinDeepShortcuts(context, shortcutIdsByPkg);
    }

    private static Map<String, List<String>> collectDeepShortcutIds(List<ContentValues> finalItems) {
        Map<String, List<String>> shortcutIdsByPkg = new HashMap<>();
        for (ContentValues cv : finalItems) {
            collectDeepShortcutIfPresent(cv, shortcutIdsByPkg);
        }
        return shortcutIdsByPkg;
    }

    private static void collectDeepShortcutIfPresent(ContentValues cv, Map<String, List<String>> shortcutIdsByPkg) {
        Integer t = cv.getAsInteger("itemType");
        if (t == null || t != ItemTypes.ITEM_TYPE_DEEP_SHORTCUT)
            return;
        String intentUri = cv.getAsString("intent");
        if (intentUri == null)
            return;
        try {
            android.content.Intent shortcutIntent = android.content.Intent.parseUri(intentUri, 0);
            String pkg = shortcutIntent.getPackage();
            if (pkg == null && shortcutIntent.getComponent() != null) {
                pkg = shortcutIntent.getComponent().getPackageName();
            }
            String id = shortcutIntent.getStringExtra("shortcut_id");
            if (pkg != null && id != null) {
                shortcutIdsByPkg.computeIfAbsent(pkg, k -> new ArrayList<>()).add(id);
            }
        } catch (java.net.URISyntaxException e) {
            LOG.w("Unparseable deep-shortcut intent (id=" + cv.getAsLong("_id") + "): " + e.getMessage());
        }
    }

    private static void pinDeepShortcuts(Context context, Map<String, List<String>> shortcutIdsByPkg) {
        LauncherApps la = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        UserHandle me = Process.myUserHandle();
        for (Map.Entry<String, List<String>> entry : shortcutIdsByPkg.entrySet()) {
            try {
                la.pinShortcuts(entry.getKey(), entry.getValue(), me);
                LOG.i("Pinned " + entry.getValue().size() + " deep shortcut(s) for " + entry.getKey());
            } catch (Throwable t) {
                LOG.w("pinShortcuts failed for " + entry.getKey() + ": " + t.getClass().getSimpleName() + ": "
                        + t.getMessage());
            }
        }
    }

    private static void refreshIconCache(Context context) {
        try {
            LauncherAppState las = LauncherAppState.getInstance(context);
            InvariantDeviceProfile idp = las.getInvariantDeviceProfile();
            las.getIconCache().updateIconParams(idp.fillResIconDpi, idp.iconBitmapSize);
        } catch (Throwable t) {
            LOG.w("Could not refresh icon cache: " + t.getMessage());
        }
    }

    private static Map<Long, int[]> captureOriginalPositions(List<ContentValues> finalItems) {
        Map<Long, int[]> originalPositions = new HashMap<>();
        for (ContentValues cv : finalItems) {
            Long id = cv.getAsLong("_id");
            Integer container = cv.getAsInteger(COL_CONTAINER);
            Integer screen = cv.getAsInteger(COL_SCREEN);
            Integer cellX = cv.getAsInteger(COL_CELL_X);
            Integer cellY = cv.getAsInteger(COL_CELL_Y);
            if (id == null || container == null || screen == null || cellX == null || cellY == null)
                continue;
            // Only desktop items — folder children keep whatever the loader gives them.
            if (container == ItemTypes.CONTAINER_DESKTOP) {
                originalPositions.put(id, new int[]{screen, cellX, cellY});
            }
        }
        return originalPositions;
    }

    private static String resolveDbFile(Context context) {
        String gridName = LauncherComponentProvider.get(context).getLauncherPrefs().get(LauncherPrefs.GRID_NAME);
        return (gridName != null && !gridName.isEmpty())
                ? "launcher_" + gridName + ".db"
                : LauncherAppState.getInstance(context).getInvariantDeviceProfile().dbFile;
    }

    private static DatabaseHelper openHelper(Context context, String dbFile) {
        return new DatabaseHelper(context, dbFile,
                user -> ((UserManager) context.getSystemService(Context.USER_SERVICE)).getSerialNumberForUser(user),
                () -> {
                });
    }

    private static void applyPositionFixupFirstPass(Context context, Map<Long, int[]> originalPositions) {
        String dbFile = resolveDbFile(context);
        DatabaseHelper helperFix = openHelper(context, dbFile);
        int patched = 0;
        try {
            SQLiteDatabase db = helperFix.getWritableDatabase();
            db.beginTransaction();
            try {
                for (Map.Entry<Long, int[]> e : originalPositions.entrySet()) {
                    int[] pos = e.getValue();
                    ContentValues v = new ContentValues();
                    v.put(COL_SCREEN, pos[0]);
                    v.put(COL_CELL_X, pos[1]);
                    v.put(COL_CELL_Y, pos[2]);
                    v.put(COL_CONTAINER, ItemTypes.CONTAINER_DESKTOP);
                    int rows = db.update(LauncherSettings.Favorites.TABLE_NAME, v, "_id=?",
                            new String[]{String.valueOf(e.getKey())});
                    if (rows > 0)
                        patched++;
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            // Verify post-fixup, BEFORE we close the helper.
            try (Cursor c2 = db.rawQuery("SELECT _id, screen, cellX, cellY, title FROM "
                    + LauncherSettings.Favorites.TABLE_NAME + " WHERE container=-100 ORDER BY screen, cellY, cellX",
                    null)) {
                while (c2.moveToNext()) {
                    LOG.d("post-fixup verify id=" + c2.getLong(0) + " screen=" + c2.getInt(1) + " cellX=" + c2.getInt(2)
                            + " cellY=" + c2.getInt(3) + " title=" + c2.getString(4));
                }
            }
        } finally {
            helperFix.close();
        }
        LOG.i("Re-applied original positions to " + patched + " imported workspace items (loader had moved them)");
    }

    private static void applyPositionFixupSecondPass(Context context, Map<Long, int[]> originalPositions) {
        String dbFile = resolveDbFile(context);
        DatabaseHelper helperFix2 = openHelper(context, dbFile);
        int patched2 = 0;
        try {
            SQLiteDatabase db = helperFix2.getWritableDatabase();
            db.beginTransaction();
            try {
                for (Map.Entry<Long, int[]> e : originalPositions.entrySet()) {
                    int[] pos = e.getValue();
                    ContentValues v = new ContentValues();
                    v.put(COL_SCREEN, pos[0]);
                    v.put(COL_CELL_X, pos[1]);
                    v.put(COL_CELL_Y, pos[2]);
                    v.put(COL_CONTAINER, ItemTypes.CONTAINER_DESKTOP);
                    int rows = db.update(LauncherSettings.Favorites.TABLE_NAME, v, "_id=?",
                            new String[]{String.valueOf(e.getKey())});
                    if (rows > 0)
                        patched2++;
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } finally {
            helperFix2.close();
        }
        LOG.i("Second-pass position fixup updated " + patched2 + " rows");

        // Final forceReload: now the DB holds the user's original positions
        // and all earlier listener-driven loader passes have drained.
        Executors.MAIN_EXECUTOR.execute(() -> LauncherAppState.getInstance(context).getModel().forceReload());
    }
}
