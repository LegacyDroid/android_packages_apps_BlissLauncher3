/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/controllers/home/IconPackController.java
 * Module:  main source-set  (com.android.launcher3.settings.controllers.home)
 * Role:    NEW
 *
 * Tree (settings/controllers/home/):
 *   See AccentColorController.java for the directory's full sibling listing.
 *
 * Purpose:
 *   List — installed icon-pack discovery + selection. Queries PackageManager
 *   for known icon-pack action filters, dedupes, exposes label-> package
 *   pairs to the ListPreference.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7
 */
package com.android.launcher3.settings.controllers.home;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.dagger.LauncherComponentProvider;
import com.android.launcher3.settings.controllers.PreferenceController;

import foundation.e.bliss.preferences.BlissPrefs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** List — installed icon-pack discovery and selection. */
public final class IconPackController implements PreferenceController {
    @NonNull @Override public String key() { return BlissPrefs.PREF_ICON_PACK; }

    @Override public void onAttach(@NonNull Preference preference, @NonNull Context ctx) {
        if (!(preference instanceof ListPreference)) return;
        ListPreference lp = (ListPreference) preference;
        PackageManager pm = ctx.getPackageManager();
        List<String> packLabels = new ArrayList<>();
        List<String> packValues = new ArrayList<>();
        packLabels.add(ctx.getString(R.string.icon_pack_system));
        packValues.add("");

        String[] iconPackActions = {
                "com.novalauncher.THEME",
                "org.adw.launcher.icons.ACTION_PICK_ICON",
                "com.dlto.atom.launcher.THEME",
        };
        Set<String> seen = new HashSet<>();
        for (String action : iconPackActions) {
            for (ResolveInfo ri : pm.queryIntentActivities(new Intent(action), 0)) {
                String pkg = ri.activityInfo.packageName;
                if (seen.add(pkg)) {
                    packLabels.add(ri.loadLabel(pm).toString());
                    packValues.add(pkg);
                }
            }
        }
        Intent categoryIntent = new Intent(Intent.ACTION_MAIN);
        categoryIntent.addCategory("com.anddoes.launcher.THEME");
        for (ResolveInfo ri : pm.queryIntentActivities(categoryIntent, 0)) {
            String pkg = ri.activityInfo.packageName;
            if (seen.add(pkg)) {
                packLabels.add(ri.loadLabel(pm).toString());
                packValues.add(pkg);
            }
        }

        String[] labels = packLabels.toArray(new String[0]);
        String[] values = packValues.toArray(new String[0]);
        lp.setEntries(labels);
        lp.setEntryValues(values);
        LauncherPrefs prefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();
        String current = prefs.get(LauncherPrefs.ICON_PACK);
        lp.setValue(current);
        int idx = lp.findIndexOfValue(current);
        if (idx >= 0) lp.setSummary(labels[idx]);
        lp.setOnPreferenceChangeListener((p, v) -> {
            String packName = (String) v;
            prefs.put(LauncherPrefs.ICON_PACK, packName);
            int newIdx = lp.findIndexOfValue(packName);
            if (newIdx >= 0) lp.setSummary(labels[newIdx]);
            return true;
        });
    }
}
