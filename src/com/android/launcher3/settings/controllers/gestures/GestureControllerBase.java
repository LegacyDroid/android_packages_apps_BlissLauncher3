/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/gestures/GestureControllerBase.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.gestures)
 * Role:    NEW
 *
 * Tree (settings/controllers/gestures/):
 *   ├── GestureControllerBase.java       — shared per-gesture wiring  ← THIS FILE
 *   ├── GestureDoubleTapController.java
 *   ├── GestureSwipeDownController.java
 *   ├── GestureEdgeLeftController.java
 *   └── GestureEdgeRightController.java
 *
 * Purpose:
 *   Boilerplate base for the four gesture controllers — same handler list,
 *   same app-picker dialog when the user picks "Launch app". Subclasses
 *   declare key() and the LauncherPrefs item; the rest is shared.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.gestures;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.dagger.LauncherComponentProvider;
import com.android.launcher3.settings.controllers.PreferenceController;

import foundation.e.bliss.gestures.GestureHandler;

import java.util.ArrayList;
import java.util.List;

/** Shared wiring for the four gesture-binding ListPreferences. */
abstract class GestureControllerBase implements PreferenceController {

    protected abstract ConstantItem<String> prefItem();

    @Override
    public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        if (!(preference instanceof ListPreference)) return;
        final ListPreference lp = (ListPreference) preference;
        final ConstantItem<String> item = prefItem();
        final LauncherPrefs prefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();

        final String[] labels = new String[]{
                ctx.getString(R.string.gesture_none),
                ctx.getString(R.string.gesture_sleep),
                ctx.getString(R.string.gesture_notifications),
                ctx.getString(R.string.gesture_quick_settings),
                ctx.getString(R.string.gesture_app_drawer),
                ctx.getString(R.string.gesture_app_search),
                ctx.getString(R.string.gesture_assistant),
                ctx.getString(R.string.gesture_recents),
                ctx.getString(R.string.gesture_launch_app),
        };
        final String[] values = new String[]{
                GestureHandler.HANDLER_NONE,
                GestureHandler.HANDLER_SLEEP,
                GestureHandler.HANDLER_NOTIFICATIONS,
                GestureHandler.HANDLER_QUICK_SETTINGS,
                GestureHandler.HANDLER_APP_DRAWER,
                GestureHandler.HANDLER_APP_SEARCH,
                GestureHandler.HANDLER_ASSISTANT,
                GestureHandler.HANDLER_RECENTS,
                GestureHandler.HANDLER_APP_PREFIX,
        };
        lp.setEntries(labels);
        lp.setEntryValues(values);
        String current = prefs.get(item);
        lp.setValue(current);
        updateSummary(lp, current, labels, ctx);

        lp.setOnPreferenceChangeListener((p, v) -> {
            String handlerName = (String) v;
            if (GestureHandler.HANDLER_APP_PREFIX.equals(handlerName)) {
                showGestureAppPicker(lp, item, labels, ctx);
                return false;
            }
            prefs.put(item, handlerName);
            updateSummary(lp, handlerName, labels, ctx);
            return true;
        });
    }

    private static void updateSummary(ListPreference lp, String value, String[] labels,
            Context ctx) {
        if (value != null && value.startsWith(GestureHandler.HANDLER_APP_PREFIX)) {
            String component = value.substring(GestureHandler.HANDLER_APP_PREFIX.length());
            try {
                ComponentName cn = ComponentName.unflattenFromString(component);
                if (cn != null) {
                    PackageManager pm = ctx.getPackageManager();
                    String appLabel = pm.getActivityInfo(cn, 0).loadLabel(pm).toString();
                    lp.setSummary(ctx.getString(R.string.gesture_launch_app) + ": " + appLabel);
                    return;
                }
            } catch (Exception e) { /* fall through */ }
            lp.setSummary(ctx.getString(R.string.gesture_launch_app));
        } else {
            int idx = lp.findIndexOfValue(value);
            if (idx >= 0) lp.setSummary(labels[idx]);
        }
    }

    private static void showGestureAppPicker(ListPreference lp, ConstantItem<String> item,
            String[] labels, Context ctx) {
        LauncherPrefs prefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();
        LauncherApps launcherApps =
                (LauncherApps) ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        List<LauncherActivityInfo> activities =
                launcherApps.getActivityList(null, Process.myUserHandle());
        List<LauncherActivityInfo> sorted = new ArrayList<>(activities);
        sorted.sort((a, b) -> a.getLabel().toString()
                .compareToIgnoreCase(b.getLabel().toString()));
        String[] appLabels = new String[sorted.size()];
        String[] appComponents = new String[sorted.size()];
        for (int i = 0; i < sorted.size(); i++) {
            LauncherActivityInfo info = sorted.get(i);
            appLabels[i] = info.getLabel().toString();
            appComponents[i] = info.getComponentName().flattenToString();
        }
        new AlertDialog.Builder(ctx)
                .setTitle(R.string.gesture_pick_app_title)
                .setItems(appLabels, (dialog, which) -> {
                    String handlerValue = GestureHandler.HANDLER_APP_PREFIX + appComponents[which];
                    prefs.put(item, handlerValue);
                    lp.setValue(handlerValue);
                    updateSummary(lp, handlerValue, labels, ctx);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
