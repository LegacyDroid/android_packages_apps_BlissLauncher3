/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/drawer/HiddenAppsController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.drawer)
 * Role:    NEW
 *
 * Tree (settings/controllers/drawer/):
 *   See DrawerAnimationController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Multi-select dialog for "Hidden apps" — every launchable activity is
 *   shown as a checkbox; the selected package set is persisted in
 *   LauncherPrefs.HIDDEN_APPS and the model is forced to reload so the
 *   drawer updates without a launcher restart.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.drawer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.dagger.LauncherComponentProvider;
import com.android.launcher3.settings.controllers.PreferenceController;

import foundation.e.bliss.preferences.BlissPrefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Multi-select hidden-apps dialog. */
public final class HiddenAppsController implements PreferenceController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_HIDDEN_APPS; }

    @Override
    public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        LauncherPrefs prefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();
        Set<String> hiddenApps = prefs.get(LauncherPrefs.HIDDEN_APPS);
        if (hiddenApps == null) hiddenApps = Collections.emptySet();
        updateSummary(preference, hiddenApps.size(), ctx);

        preference.setOnPreferenceClickListener(p -> {
            showDialog(preference, ctx);
            return true;
        });
    }

    private static void updateSummary(Preference preference, int count, Context ctx) {
        if (count == 0) {
            preference.setSummary(R.string.hidden_apps_none);
        } else {
            preference.setSummary(ctx.getString(R.string.hidden_apps_count, count));
        }
    }

    private static void showDialog(Preference preference, Context ctx) {
        LauncherPrefs prefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();
        LauncherApps launcherApps = (LauncherApps) ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        List<LauncherActivityInfo> activities =
                launcherApps.getActivityList(null, Process.myUserHandle());

        List<LauncherActivityInfo> sortedApps = new ArrayList<>(activities);
        sortedApps.sort((a, b) -> a.getLabel().toString()
                .compareToIgnoreCase(b.getLabel().toString()));

        List<String> packageList = new ArrayList<>();
        List<String> labelList = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (LauncherActivityInfo info : sortedApps) {
            String pkg = info.getComponentName().getPackageName();
            if (seen.add(pkg)) {
                packageList.add(pkg);
                labelList.add(info.getLabel().toString());
            }
        }
        String[] appLabels = labelList.toArray(new String[0]);
        String[] appPackages = packageList.toArray(new String[0]);
        Set<String> currentHidden = prefs.get(LauncherPrefs.HIDDEN_APPS);
        if (currentHidden == null) currentHidden = Collections.emptySet();
        boolean[] checkedItems = new boolean[appPackages.length];
        for (int i = 0; i < appPackages.length; i++) {
            checkedItems[i] = currentHidden.contains(appPackages[i]);
        }
        Set<String> selectedPackages = new HashSet<>(currentHidden);

        new AlertDialog.Builder(ctx)
                .setTitle(R.string.hidden_apps_title)
                .setMultiChoiceItems(appLabels, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) selectedPackages.add(appPackages[which]);
                    else selectedPackages.remove(appPackages[which]);
                })
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    prefs.put(LauncherPrefs.HIDDEN_APPS, selectedPackages);
                    updateSummary(preference, selectedPackages.size(), ctx);
                    LauncherAppState.getInstance(ctx).getModel().forceReload();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
