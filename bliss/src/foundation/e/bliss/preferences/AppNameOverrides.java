/*
 * Copyright (C) 2026 MURENA SAS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package foundation.e.bliss.preferences;

import android.content.ComponentName;
import android.content.Context;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.dagger.LauncherComponentProvider;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Per-app display-name override map. Stored as a JSON string in
 * {@link LauncherPrefs#APP_NAME_OVERRIDES}. Keys use the Lawnchair-compatible
 * format {@code "pkg/component#userSerial"} so that imports from Lawnchair's
 * {@code pref_appNameMap} are a direct passthrough. The lookup is also tolerant
 * of keys without a {@code #userSerial} suffix.
 *
 * <p>
 * The map is parsed lazily and re-parsed when the underlying pref changes.
 */
public final class AppNameOverrides {

    private static final String TAG = "AppNameOverrides";

    private static volatile ConcurrentMap<String, String> sCache = new ConcurrentHashMap<>();
    private static volatile String sCachedRaw = null;

    private AppNameOverrides() {
    }

    /** Refresh the in-memory cache from the pref. Cheap: no-op if unchanged. */
    private static void ensureCacheFresh(Context context) {
        String raw;
        try {
            raw = LauncherComponentProvider.get(context).getLauncherPrefs().get(LauncherPrefs.APP_NAME_OVERRIDES);
        } catch (Exception e) {
            return;
        }
        if (raw == null)
            raw = "";
        if (raw.equals(sCachedRaw))
            return;
        sCachedRaw = raw;
        if (raw.isEmpty()) {
            sCache = new ConcurrentHashMap<>();
            return;
        }
        try {
            JSONObject obj = new JSONObject(raw);
            ConcurrentMap<String, String> map = new ConcurrentHashMap<>(obj.length());
            Iterator<String> it = obj.keys();
            while (it.hasNext()) {
                String k = it.next();
                String v = obj.optString(k, "");
                if (!v.isEmpty())
                    map.put(k, v);
            }
            sCache = map;
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse app name overrides JSON: " + e.getMessage());
            sCache = new ConcurrentHashMap<>();
        }
    }

    /**
     * Returns the user-supplied label override for the given component+user, or
     * {@code null} if none is set.
     */
    @Nullable
    public static String getOverride(Context context, @Nullable ComponentName component, @Nullable UserHandle user) {
        if (component == null)
            return null;
        ensureCacheFresh(context);
        if (sCache.isEmpty())
            return null;
        String flat = component.flattenToString();
        long serial = 0;
        try {
            UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
            UserHandle uh = user != null ? user : Process.myUserHandle();
            serial = um != null ? um.getSerialNumberForUser(uh) : 0;
        } catch (Exception ignored) {
            // Default to 0 (single-user device) on failure
        }
        String keyed = flat + "#" + serial;
        String hit = sCache.get(keyed);
        if (hit != null)
            return hit;
        return sCache.get(flat);
    }

    /** Force-invalidate the cache so the next lookup re-reads the pref. */
    public static void invalidate() {
        sCachedRaw = null;
        sCache = new ConcurrentHashMap<>();
    }

    /**
     * Write or remove an override. Pass an empty string for {@code newName} to
     * remove an existing override (restore the default app name). Updates the
     * underlying JSON pref atomically and invalidates the cache so the next draw
     * reflects the change.
     */
    public static void setOverride(Context context, @Nullable ComponentName component, @Nullable UserHandle user,
            @Nullable String newName) {
        if (component == null)
            return;
        try {
            UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
            UserHandle uh = user != null ? user : Process.myUserHandle();
            long serial = um != null ? um.getSerialNumberForUser(uh) : 0;
            String key = component.flattenToString() + "#" + serial;

            LauncherPrefs prefs = LauncherComponentProvider.get(context).getLauncherPrefs();
            String raw = prefs.get(LauncherPrefs.APP_NAME_OVERRIDES);
            JSONObject obj = (raw == null || raw.isEmpty()) ? new JSONObject() : new JSONObject(raw);
            if (newName == null || newName.trim().isEmpty()) {
                obj.remove(key);
                obj.remove(component.flattenToString()); // legacy unsuffixed key
            } else {
                obj.put(key, newName.trim());
            }
            prefs.put(LauncherPrefs.APP_NAME_OVERRIDES, obj.toString());
            invalidate();
        } catch (Exception e) {
            Log.w(TAG, "setOverride failed: " + e.getMessage());
        }
    }
}
