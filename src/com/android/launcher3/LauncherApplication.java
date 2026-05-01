/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.android.launcher3.dagger.DaggerLauncherAppComponent;
import com.android.launcher3.dagger.LauncherAppComponent;
import com.android.launcher3.dagger.LauncherBaseAppComponent;
import com.android.launcher3.util.TraceHelper;

/**
 * Main application class for Launcher
 */
public class LauncherApplication extends Application {

    private volatile LauncherBaseAppComponent mAppComponent;
    /**
     * Migration02 / Phase 7.4 — cached auto-label color (luminance-derived) so per-icon
     * {@link BubbleTextView#getAutoLabelColor} is one volatile read, not a wallpaper-API call.
     * Recomputed on {@link Intent#ACTION_WALLPAPER_CHANGED}.
     */
    private volatile Integer mCachedAutoLabelColor = null;

    @Override
    public void onCreate() {
        super.onCreate();
        MainProcessInitializer.initialize(this);
        runOneShotMigrations();
        primeAutoLabelColor();
        registerWallpaperChangedReceiver();
    }

    /** Cached value used by {@link BubbleTextView#getAutoLabelColor()}. May be null (cold). */
    public Integer getCachedAutoLabelColor() {
        return mCachedAutoLabelColor;
    }

    private void primeAutoLabelColor() {
        try {
            mCachedAutoLabelColor =
                    BubbleTextView.computeAutoLabelColorFromWallpaper(this, android.graphics.Color.WHITE);
        } catch (Throwable ignored) { /* leave null; consumers fall back to mTextColor */ }
    }

    private void registerWallpaperChangedReceiver() {
        try {
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    primeAutoLabelColor();
                    // Ask the model to rebind so visible BubbleTextViews re-pick a color.
                    try {
                        com.android.launcher3.LauncherAppState.getInstance(context)
                                .getModel().rebindCallbacks();
                    } catch (Throwable ignored) { /* model may not be ready yet */ }
                }
            };
            IntentFilter filter = new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED);
            // Receiver-not-exported is the safer default on Android 13+; we only consume a
            // protected system broadcast.
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } catch (Throwable ignored) { /* best-effort */ }
    }

    /** One-shot migrations that run once per pref-version. Keep cheap and idempotent. */
    private void runOneShotMigrations() {
        try {
            LauncherPrefs prefs = LauncherPrefs.get(this);
            // v1: HOTSEAT_BOTTOM_FACTOR semantics changed from additive (default 0)
            // to multiplicative (default 100). Existing users with the old default of
            // 0 would otherwise see a zero-height dock bottom space.
            if (!prefs.get(LauncherPrefs.HOTSEAT_BOTTOM_FACTOR_MIGRATED_V1)) {
                if (prefs.get(LauncherPrefs.HOTSEAT_BOTTOM_FACTOR) == 0) {
                    prefs.put(LauncherPrefs.HOTSEAT_BOTTOM_FACTOR, 100);
                }
                prefs.put(LauncherPrefs.HOTSEAT_BOTTOM_FACTOR_MIGRATED_V1, true);
            }
            // Migration02 / Phase 4: rewrite Migration01-era CLOSING_APP_OVERLAY
            // values (fade/scale/slide_down) into Lawnchair-aligned values
            // (none/fade_in/suck_in). Anything we don't recognise becomes "none".
            if (!prefs.get(LauncherPrefs.CLOSING_OVERLAY_MIGRATED_V1)) {
                String mode = prefs.get(LauncherPrefs.CLOSING_APP_OVERLAY);
                if (mode != null && !mode.isEmpty()
                        && !"none".equals(mode)
                        && !"fade_in".equals(mode)
                        && !"suck_in".equals(mode)) {
                    prefs.put(LauncherPrefs.CLOSING_APP_OVERLAY, "none");
                }
                prefs.put(LauncherPrefs.CLOSING_OVERLAY_MIGRATED_V1, true);
            }
            // Migration02 / Phase 1.9: move drawer-folder JSON pref into Room.
            migrateDrawerFoldersJsonToRoom(prefs);
            // Push DOT_TEXT_COLOR pref into DotRenderer.sCounterTextOverride at boot
            // and on subsequent changes. iconloaderlib doesn't depend on Bliss so we
            // pump the value from this side via a static field.
            applyDotTextColor(prefs);
            prefs.addListener((key) -> applyDotTextColor(prefs),
                    LauncherPrefs.DOT_TEXT_COLOR);

            // Migration02 Phase 3.6: when THEMED_ICONS or DRAWER_THEMED_ICONS toggles,
            // ask LauncherModel to rebind callbacks so all visible BubbleTextViews
            // re-evaluate shouldUseThemedDrawable() and pick the correct variant from
            // BitmapInfo. ThemeManager itself listens to both prefs (parseIconState
            // regenerates IconState when either toggles) so the themedBitmap is also
            // re-materialised when a user enables theming for the first time.
            prefs.addListener((key) -> {
                if (com.android.launcher3.graphics.ThemeManager.THEMED_ICONS
                            .getSharedPrefKey().equals(key)
                        || LauncherPrefs.DRAWER_THEMED_ICONS.getSharedPrefKey().equals(key)) {
                    try {
                        com.android.launcher3.LauncherAppState.getInstance(this)
                                .getModel().rebindCallbacks();
                    } catch (Throwable ignored) { /* model may not be ready yet */ }
                }
            }, com.android.launcher3.graphics.ThemeManager.THEMED_ICONS,
               LauncherPrefs.DRAWER_THEMED_ICONS);

            // Migration02 Phase 7.4/7.5 (XC-6 fan-out): re-render workspace + drawer when label
            // mode toggles, and re-apply the work-tab background tint when its color pref changes.
            prefs.addListener((key) -> {
                try {
                    if (LauncherPrefs.HOME_LABEL_COLOR_MODE.getSharedPrefKey().equals(key)) {
                        primeAutoLabelColor();
                        com.android.launcher3.LauncherAppState.getInstance(this)
                                .getModel().rebindCallbacks();
                    } else if (LauncherPrefs.WORK_TAB_BG_COLOR.getSharedPrefKey().equals(key)) {
                        com.android.launcher3.LauncherAppState.getInstance(this)
                                .getModel().rebindCallbacks();
                    }
                } catch (Throwable ignored) { /* model may not be ready yet */ }
            }, LauncherPrefs.HOME_LABEL_COLOR_MODE, LauncherPrefs.WORK_TAB_BG_COLOR);
        } catch (Exception ignored) {
            // Migration is best-effort.
        }
    }

    /**
     * Idempotent migration: one-time copy of the legacy {@code DRAWER_FOLDERS_MAP} JSON pref
     * into the Room-backed drawer-folder DB (see Migration02 Phase 1.9). Re-running yields the
     * same DB state because {@link foundation.e.bliss.folders.DrawerFolderService#upsert} clears
     * existing items per folder before re-inserting.
     */
    private void migrateDrawerFoldersJsonToRoom(LauncherPrefs prefs) {
        try {
            if (prefs.get(LauncherPrefs.DRAWER_FOLDERS_MIGRATED_V1)) return;

            String json = prefs.get(LauncherPrefs.DRAWER_FOLDERS_MAP);
            if (json == null || json.isEmpty() || "{}".equals(json)) {
                prefs.put(LauncherPrefs.DRAWER_FOLDERS_MIGRATED_V1, true);
                return;
            }

            // Defer the heavy work to MODEL_EXECUTOR; the migration involves Room DAO + a
            // LauncherApps query. Mark migrated once the work *enqueues* so we don't rerun on
            // every launch even if something explodes during DB write.
            prefs.put(LauncherPrefs.DRAWER_FOLDERS_MIGRATED_V1, true);

            final android.content.Context appCtx = this;
            com.android.launcher3.util.Executors.MODEL_EXECUTOR.execute(() -> {
                try {
                    org.json.JSONObject obj = new org.json.JSONObject(json);
                    java.util.Iterator<String> keys = obj.keys();
                    android.content.pm.LauncherApps la =
                            (android.content.pm.LauncherApps) appCtx.getSystemService(
                                    android.content.Context.LAUNCHER_APPS_SERVICE);
                    android.os.UserHandle user = android.os.Process.myUserHandle();
                    foundation.e.bliss.folders.DrawerFolderService svc =
                            foundation.e.bliss.folders.DrawerFolderService.get(appCtx);
                    int rank = 0;
                    while (keys.hasNext()) {
                        String name = keys.next();
                        Object raw = obj.opt(name);
                        if (!(raw instanceof org.json.JSONArray pkgs)) continue;
                        java.util.List<com.android.launcher3.util.ComponentKey> contents =
                                new java.util.ArrayList<>();
                        for (int i = 0; i < pkgs.length(); i++) {
                            String pkg = pkgs.optString(i);
                            if (pkg == null || pkg.isEmpty()) continue;
                            try {
                                java.util.List<android.content.pm.LauncherActivityInfo> acts =
                                        la.getActivityList(pkg, user);
                                if (acts != null && !acts.isEmpty()) {
                                    contents.add(new com.android.launcher3.util.ComponentKey(
                                            acts.get(0).getComponentName(), user));
                                }
                            } catch (Throwable ignored) { /* package not installed */ }
                        }
                        if (!contents.isEmpty()) {
                            svc.upsertBlocking(0, name, rank++, contents);
                        }
                    }
                } catch (Throwable t) {
                    android.util.Log.w("LauncherApp",
                            "Drawer folder JSON->Room migration failed", t);
                }
            });
        } catch (Throwable ignored) { /* best-effort */ }
    }

    // Migration02 Phase 3.6: applyDrawerThemedIcons() removed. Toggling the drawer-themed
    // pref no longer flips the global mono toggle — that was the Migration01 PorterDuff-era
    // shortcut. Instead, ThemeManager.IconControllerFactory now treats DRAWER_THEMED_ICONS
    // as a themedBitmap-generation trigger and BubbleTextView::shouldUseThemedDrawable()
    // picks the per-display variant.

    private void applyDotTextColor(LauncherPrefs prefs) {
        try {
            String mode = prefs.get(LauncherPrefs.DOT_TEXT_COLOR);
            int color;
            switch (mode == null ? "auto" : mode) {
                case "white": color = android.graphics.Color.WHITE; break;
                case "black": color = android.graphics.Color.BLACK; break;
                default: color = 0; // auto
            }
            com.android.launcher3.icons.DotRenderer.sCounterTextOverride = color;
        } catch (Throwable ignored) { /* keep auto */ }
    }

    public LauncherAppComponent getAppComponent() {
        if (mAppComponent == null) {
            synchronized (this) {
                // Check for null again, as it may have been assigned on a different thread. This
                // avoids holding synchronization locks everytime.
                if (mAppComponent == null) {
                    // Initialize the dagger component on demand as content providers can get
                    // accessed before the Launcher application (b/36917845#comment4)
                    initDaggerComponent(DaggerLauncherAppComponent.builder()
                            .iconsDbName(LauncherFiles.APP_ICONS_DB));
                }
            }
        }
        // Since supertype setters will return a supertype.builder and @Component.Builder types
        // must not have any generic types.
        // We need to cast mAppComponent to {@link LauncherAppComponent} since appContext()
        // method is defined in the super class LauncherBaseComponent#Builder.
        return (LauncherAppComponent) mAppComponent;
    }

    /**
     * Init with the desired dagger component.
     */
    public void initDaggerComponent(LauncherBaseAppComponent.Builder componentBuilder) {
        mAppComponent = componentBuilder
                .appContext(this)
                .setSafeModeEnabled(TraceHelper.allowIpcs(
                        "isSafeMode", () -> getPackageManager().isSafeMode()))
                .build();
    }
}
