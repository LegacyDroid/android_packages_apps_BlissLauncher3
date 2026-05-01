package com.android.launcher3;

import android.content.ComponentName;
import android.content.Context;

import com.android.launcher3.dagger.ApplicationContext;
import com.android.launcher3.dagger.LauncherComponentProvider;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

/**
 * Utility class to filter out components from various lists
 */
public class AppFilter {

    private final Set<ComponentName> mFilteredComponents;
    private final LauncherPrefs mPrefs;

    @Inject
    public AppFilter(@ApplicationContext Context context) {
        mFilteredComponents = Arrays.stream(
                context.getResources().getStringArray(R.array.filtered_components))
                .map(ComponentName::unflattenFromString)
                .collect(Collectors.toSet());
        mPrefs = LauncherComponentProvider.get(context).getLauncherPrefs();
    }

    public boolean shouldShowApp(ComponentName app) {
        if (mFilteredComponents.contains(app)) {
            return false;
        }
        Set<String> hiddenApps = mPrefs.get(LauncherPrefs.HIDDEN_APPS);
        if (hiddenApps != null && !hiddenApps.isEmpty()) {
            if (hiddenApps.contains(app.getPackageName())) return false;
        }
        // NOTE: Migration02 / Phase 1.10 — the JSON-backed drawer-folder filter that
        // previously hid folder members has been removed. Folder grouping now lives in
        // BlissAlphabeticalAppsList.addAppsWithSections, which both renders the folder
        // card AND removes its members from the flat list. Filtering here as well would
        // double-hide and produce missing apps.
        return true;
    }

    /**
     * Returns true if the given package is hidden by the user.
     */
    public boolean isHiddenApp(String packageName) {
        Set<String> hiddenApps = mPrefs.get(LauncherPrefs.HIDDEN_APPS);
        return hiddenApps != null && hiddenApps.contains(packageName);
    }
}
