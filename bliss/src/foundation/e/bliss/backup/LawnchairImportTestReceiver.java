/*
 * Copyright (C) 2026 MURENA SAS
 *
 * Debug-only test trigger for the Lawnchair import flow. Sends a broadcast
 * with a file path; the receiver runs the import without going through the
 * Storage Access Framework picker. Useful for automated testing.
 *
 *   adb shell am broadcast \
 *     -a foundation.e.blisslauncher.test.IMPORT \
 *     --es path /sdcard/Download/whatever.lawnchairbackup \
 *     -n foundation.e.blisslauncher.test/foundation.e.bliss.backup.LawnchairImportTestReceiver
 */
/*
 * Bliss touchpoint(s) (Migration04):
 *   - Tighten onReceive() to fail-fast on non-debug builds via BuildConfig.DEBUG
 *     (in addition to the pre-existing BuildConfig.IS_DEBUG_DEVICE check), so
 *     that a release-flavoured APK shipped to a user with a stale install on
 *     the device that exposes this receiver via the leftover manifest entry
 *     can't be coerced into running the import via adb.
 *     — Plan ref: Plans/Migration04/08-observability-and-testing.md §2
 *   - Phase 02 (importer decomposition):
 *       * The result type is now top-level foundation.e.bliss.backup.ImportResult
 *         (lifted out of LawnchairImportHelper).
 *       * Logging routes through foundation.e.bliss.utils.Logger.tag(…); the
 *         android.util.Log import is gone.
 *     — Plan ref: Plans/Migration04/02-importer-decomposition.md §6, §8 phase 9
 *
 * The body of this file otherwise tracks pre-Migration04 state.
 */
package foundation.e.bliss.backup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.android.launcher3.BuildConfig;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.util.Executors;

import foundation.e.bliss.utils.Logger;

import java.io.FileInputStream;
import java.io.InputStream;

public class LawnchairImportTestReceiver extends BroadcastReceiver {

    private static final String TAG = "LawnchairImportTest";
    private static final Logger LOG = Logger.tag(TAG);

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!BuildConfig.DEBUG || !BuildConfig.IS_DEBUG_DEVICE) {
            LOG.w("Test receiver invoked on non-debug build; ignoring.");
            return;
        }
        String path = intent.getStringExtra("path");
        if (path == null) {
            LOG.w("No 'path' extra supplied");
            return;
        }
        LOG.i("Triggering import from path=" + path);
        // Run off the main thread — importFromLawnchair internally blocks on
        // MODEL_EXECUTOR which would deadlock if we called it from the
        // BroadcastReceiver's main-thread onReceive.
        final String pathFinal = path;
        Executors.MODEL_EXECUTOR.execute(() -> {
            try (InputStream is = new FileInputStream(pathFinal)) {
                ImportResult result = LawnchairImportHelper.importFromLawnchair(context, is);
                LOG.i("Import done: success=" + result.success + " settings=" + result.settingsImported + " layout="
                        + result.layoutImported + " message=" + result.message);
                if (result.success) {
                    LauncherAppState.getInstance(context).getModel().forceReload();
                }
            } catch (Exception e) {
                LOG.e("Import threw exception", e);
            }
        });
    }
}
