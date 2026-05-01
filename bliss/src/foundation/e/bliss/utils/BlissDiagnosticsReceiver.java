/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/src/foundation/e/bliss/utils/BlissDiagnosticsReceiver.java
 * Module:  bliss source-set  (foundation.e.bliss.utils)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/utils/):
 *   ├── BlissConstants.kt              — shared constants used across bliss-side files
 *   ├── BlissDbHelper.kt               — SQLite helper for the Bliss launcher.db
 *   ├── BlissDbUtils.kt                — query helpers around BlissDbHelper
 *   ├── BlissDiagnosticsReceiver.java  — debug-only DUMP_PREFS broadcast receiver  ← THIS FILE
 *   ├── BlissUtils.kt                  — misc helper functions
 *   ├── Logger.kt                      — centralised logger
 *   ├── ObservableList.kt              — observable list primitive
 *   ├── OnDataChangedListener.kt       — listener interface for data-change signals
 *   └── PrivateSpaceCompat.java        — private-space SDK shim (will move to :bliss-compat)
 *
 * Purpose:
 *   Debug-only diagnostics shim for triaging launcher state. Exposes one adb
 *   broadcast action (DUMP_PREFS) which dumps the contents of the default
 *   SharedPreferences file to logcat. Receiver is fail-fast on non-debug
 *   builds and additionally manifest-gated to debug-only by the build.
 *
 *   Trigger:
 *     adb shell am broadcast \
 *       -a foundation.e.blisslauncher.test.DUMP_PREFS \
 *       -n foundation.e.blisslauncher.test/foundation.e.bliss.utils.BlissDiagnosticsReceiver
 *
 * Plan reference: Plans/Migration04/08-observability-and-testing.md §2
 */
package foundation.e.bliss.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.launcher3.BuildConfig;
import com.android.launcher3.LauncherFiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BlissDiagnosticsReceiver extends BroadcastReceiver {

    private static final String TAG = "BlissDiagnostics";
    private static final String ACTION_DUMP_PREFS = "foundation.e.blisslauncher.test.DUMP_PREFS";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!BuildConfig.DEBUG || !BuildConfig.IS_DEBUG_DEVICE) {
            Log.w(TAG, "Diagnostics receiver invoked on non-debug build; ignoring.");
            return;
        }
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (!ACTION_DUMP_PREFS.equals(action)) {
            Log.w(TAG, "Unknown action: " + action);
            return;
        }
        dumpPrefs(context);
    }

    private static void dumpPrefs(Context context) {
        SharedPreferences sp = context.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        Map<String, ?> all = sp.getAll();
        List<String> keys = new ArrayList<>(all.keySet());
        Collections.sort(keys);
        Log.i(TAG, "===== BlissLauncher prefs dump (" + keys.size() + " entries) =====");
        for (String key : keys) {
            Object value = all.get(key);
            String valueDesc = value == null ? "null" : value.getClass().getSimpleName() + ":" + value;
            Log.i(TAG, "  " + key + " = " + valueDesc);
        }
        Log.i(TAG, "===== end prefs dump =====");
    }
}
