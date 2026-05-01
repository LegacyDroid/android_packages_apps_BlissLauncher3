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
 * File:    bliss/src/foundation/e/bliss/backup/workspace/WidgetRebinder.java
 * Module:  bliss source-set  (foundation.e.bliss.backup.workspace)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/backup/workspace/):
 *   ├── ContentValuesBuilder.java   — Lawnchair cursor row → ContentValues
 *   ├── FavoritesCursor.java        — column-index registry
 *   ├── IconBlobWriter.java         — DEEP_SHORTCUT icon BLOB carry-across
 *   ├── ItemTypes.java              — ITEM_TYPE_* / CONTAINER_* constants
 *   ├── PostImportLayoutFix.java    — re-pin shortcuts + position re-apply
 *   ├── WidgetRebinder.java         — allocateAppWidgetId + bindAppWidgetIdIfAllowed  ← THIS FILE
 *   └── WorkspaceImporter.java      — orchestrator
 *
 * Purpose:
 *   Allocates a new appWidgetId for an imported Lawnchair widget and binds
 *   it to the originally-recorded provider. When binding fails (provider
 *   not installed, or BIND_APPWIDGET denied) the call returns a
 *   pending-restore Result with the appropriate restoreFlags so the row is
 *   inserted as a tap-to-restore placeholder. Side-effect order matters:
 *   the LauncherWidgetHolder allocation must happen before the row INSERT
 *   (§5 invariant 4).
 *
 * Consumed by:
 *   - foundation.e.bliss.backup.workspace.WorkspaceImporter
 *
 * Calls into:
 *   - com.android.launcher3.widget.LauncherWidgetHolder
 *
 * Plan reference: Plans/Migration04/02-importer-decomposition.md §5 invariant 4
 */
package foundation.e.bliss.backup.workspace;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.os.Process;

import com.android.launcher3.model.data.LauncherAppWidgetInfo;
import com.android.launcher3.widget.LauncherWidgetHolder;

import foundation.e.bliss.utils.Logger;

/** Rebinds a Lawnchair widget provider to a new appWidgetId. */
public final class WidgetRebinder {

    private static final Logger LOG = Logger.tag("LawnchairImport");

    private WidgetRebinder() {
    }

    /**
     * Result of a rebind attempt — either a fresh appWidgetId or a pending-restore
     * flag set.
     */
    public static final class Result {
        public final int newAppWidgetId; // -1 when binding failed
        public final int restoreFlags; // 0 when bound; pending-restore flags otherwise
        public final boolean skipped; // true when the row should not be inserted at all
        public final boolean pendingCounted; // true when the failure should be counted in skippedWidgets

        Result(int newAppWidgetId, int restoreFlags, boolean skipped, boolean pendingCounted) {
            this.newAppWidgetId = newAppWidgetId;
            this.restoreFlags = restoreFlags;
            this.skipped = skipped;
            this.pendingCounted = pendingCounted;
        }
    }

    /**
     * Attempt to rebind the widget recorded in {@code providerStr}. Mirrors the
     * original LawnchairImportHelper logic byte-for-byte: parse provider, check
     * installed, allocate+bind via LauncherWidgetHolder, fall back to
     * pending-restore flags when the bind doesn't succeed.
     */
    public static Result rebind(Context context, AppWidgetManager appWidgetManager, String providerStr) {
        ComponentName provider = providerStr == null ? null : ComponentName.unflattenFromString(providerStr);
        if (provider == null) {
            LOG.i("Skipping widget with unparseable provider: " + providerStr);
            return new Result(-1, 0, /* skipped */ true, /* pendingCounted */ true);
        }
        // Verify the widget provider is installed on this device.
        boolean providerInstalled = false;
        try {
            for (AppWidgetProviderInfo p : appWidgetManager.getInstalledProvidersForPackage(provider.getPackageName(),
                    Process.myUserHandle())) {
                if (provider.equals(p.provider)) {
                    providerInstalled = true;
                    break;
                }
            }
        } catch (Exception e) {
            // older API or restriction — ignore and try anyway
            providerInstalled = true;
        }

        boolean bound = false;
        int newAppWidgetId = -1;
        if (providerInstalled) {
            LauncherWidgetHolder holder = LauncherWidgetHolder.newInstance(context);
            try {
                int allocated = holder.allocateAppWidgetId();
                bound = appWidgetManager.bindAppWidgetIdIfAllowed(allocated, provider);
                if (bound) {
                    newAppWidgetId = allocated;
                } else {
                    holder.deleteAppWidgetId(allocated);
                }
            } catch (Exception e) {
                LOG.w("Widget bind failed for " + provider + ": " + e.getMessage());
            } finally {
                holder.destroy();
            }
        }

        if (bound) {
            return new Result(newAppWidgetId, 0, false, false);
        }

        // Insert as a pending-restore widget (mirrors original).
        int restoreFlags = LauncherAppWidgetInfo.FLAG_ID_NOT_VALID | LauncherAppWidgetInfo.FLAG_UI_NOT_READY
                | LauncherAppWidgetInfo.FLAG_RESTORE_STARTED;
        if (!providerInstalled) {
            restoreFlags |= LauncherAppWidgetInfo.FLAG_PROVIDER_NOT_READY;
            LOG.i("Widget pending — provider not installed: " + provider);
        } else {
            LOG.i("Widget pending — bind not allowed: " + provider);
        }
        return new Result(-1, restoreFlags, /* skipped */ false, /* pendingCounted */ true);
    }
}
