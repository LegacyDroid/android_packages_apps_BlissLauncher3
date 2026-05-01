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
 * File:    bliss/src/foundation/e/bliss/backup/prefs/DrawerFoldersWriter.java
 * Module:  bliss source-set  (foundation.e.bliss.backup.prefs)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/backup/prefs/):
 *   ├── BasicMappers.java          — typed primitives
 *   ├── DomainMappers.java         — domain-specific mappers
 *   ├── DrawerFoldersWriter.java   — JSON-list → Room store  ← THIS FILE
 *   ├── PrefMapper.java            — single-mapper functional interface
 *   └── PrefMapperRegistry.java    — orchestrator
 *
 * Purpose:
 *   Lifted from LawnchairImportHelper.writeDrawerFoldersToRoom. Migration02
 *   Phase 1.11 introduced this side-effect so users see drawer folders
 *   immediately after a Lawnchair import without waiting for the next
 *   runOneShotMigrations cycle. The writes are idempotent: DrawerFolderService
 *   .upsertBlocking clears each folder's items before re-inserting (XC-7).
 *
 * Consumed by:
 *   - foundation.e.bliss.backup.prefs.PrefMapperRegistry  — after pref_drawerFolders mapping
 *
 * Calls into:
 *   - foundation.e.bliss.folders.DrawerFolderService
 *
 * Plan reference: Plans/Migration04/02-importer-decomposition.md §4 (lifted from lines 1088–1138)
 */
package foundation.e.bliss.backup.prefs;

import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.os.Process;
import android.os.UserHandle;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.dagger.LauncherComponentProvider;
import com.android.launcher3.util.ComponentKey;

import foundation.e.bliss.folders.DrawerFolderService;
import foundation.e.bliss.utils.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Push the drawer-folders JSON map into the Room-backed drawer-folder DB so
 * users see folders immediately after a Lawnchair import.
 */
public final class DrawerFoldersWriter {

    private static final Logger LOG = Logger.tag("LawnchairImport");

    private DrawerFoldersWriter() {
    }

    public static void write(Context context, String json) {
        try {
            JSONObject obj = new JSONObject(json);
            Iterator<String> keys = obj.keys();
            LauncherApps la = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            UserHandle user = Process.myUserHandle();
            DrawerFolderService svc = DrawerFolderService.get(context);
            int rank = 0;
            while (keys.hasNext()) {
                String name = keys.next();
                Object raw = obj.opt(name);
                if (!(raw instanceof JSONArray pkgs))
                    continue;
                List<ComponentKey> contents = new ArrayList<>();
                for (int i = 0; i < pkgs.length(); i++) {
                    String pkg = pkgs.optString(i);
                    if (pkg == null || pkg.isEmpty())
                        continue;
                    try {
                        List<LauncherActivityInfo> acts = la.getActivityList(pkg, user);
                        if (acts != null && !acts.isEmpty()) {
                            contents.add(new ComponentKey(acts.get(0).getComponentName(), user));
                        }
                    } catch (Throwable ignored) {
                        /* package not installed */ }
                }
                if (!contents.isEmpty()) {
                    svc.upsertBlocking(0, name, rank++, contents);
                }
            }
            // Mark as migrated so the LauncherApplication one-shot path doesn't re-import.
            LauncherPrefs prefs = LauncherComponentProvider.get(context).getLauncherPrefs();
            prefs.put(LauncherPrefs.DRAWER_FOLDERS_MIGRATED_V1, true);
        } catch (Throwable t) {
            LOG.w("writeDrawerFoldersToRoom: failed", t);
        }
    }
}
