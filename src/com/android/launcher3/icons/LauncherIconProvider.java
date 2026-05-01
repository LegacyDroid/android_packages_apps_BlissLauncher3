/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.launcher3.icons;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.dagger.ApplicationContext;
import com.android.launcher3.dagger.LauncherAppSingleton;
import com.android.launcher3.graphics.ShapeDelegate;
import com.android.launcher3.graphics.ThemeManager;
import com.android.launcher3.util.ApiWrapper;

import org.xmlpull.v1.XmlPullParser;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import foundation.e.bliss.iconpack.IconPackManager;
import foundation.e.bliss.icons.CustomAdaptiveIconDrawable;

/**
 * Extension of {@link IconProvider} with support for overriding theme icons
 */
@LauncherAppSingleton
public class LauncherIconProvider extends IconProvider {

    private static final String TAG_ICON = "icon";
    private static final String ATTR_PACKAGE = "package";
    private static final String ATTR_DRAWABLE = "drawable";

    private static final String TAG = "LIconProvider";
    private static final Map<String, ThemeData> DISABLED_MAP = Collections.emptyMap();

    private Map<String, ThemeData> mThemedIconMap;

    private final ApiWrapper mApiWrapper;
    private final ThemeManager mThemeManager;
    private final IconPackManager mIconPackManager;
    private String mLoadedIconPackPkg;

    @Inject
    public LauncherIconProvider(
            @ApplicationContext Context context,
            ThemeManager themeManager,
            ApiWrapper apiWrapper) {
        super(context);
        mThemeManager = themeManager;
        mApiWrapper = apiWrapper;
        mIconPackManager = new IconPackManager(context);
        setIconThemeSupported(mThemeManager.isMonoThemeEnabled());
        loadIconPackFromPrefs();
    }

    private void loadIconPackFromPrefs() {
        try {
            String packPkg = LauncherPrefs.get(mContext).get(LauncherPrefs.ICON_PACK);
            if (!TextUtils.equals(packPkg, mLoadedIconPackPkg)) {
                mIconPackManager.loadIconPack(packPkg);
                mLoadedIconPackPkg = packPkg;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to load icon pack from prefs", e);
        }
    }

    /**
     * Enables or disables icon theme support
     */
    public void setIconThemeSupported(boolean isSupported) {
        mThemedIconMap = isSupported && FeatureFlags.USE_LOCAL_ICON_OVERRIDES.get()
                ? null : DISABLED_MAP;
    }

    @Override
    protected ThemeData getThemeDataForPackage(String packageName) {
        return getThemedIconMap().get(packageName);
    }

    @Override
    public void updateSystemState() {
        super.updateSystemState();
        mSystemState += "," + mThemeManager.getIconState().toUniqueId();
        String pack = mLoadedIconPackPkg != null ? mLoadedIconPackPkg : "";
        mSystemState += ",iconpack=" + pack;
    }

    @Override
    protected String getApplicationInfoHash(@NonNull ApplicationInfo appInfo) {
        return mApiWrapper.getApplicationInfoHash(appInfo);
    }

    @Nullable
    @Override
    protected Drawable loadAppInfoIcon(ApplicationInfo info, Resources resources, int density) {
        // Check icon pack first
        loadIconPackFromPrefs();
        if (mIconPackManager.isIconPackLoaded()) {
            ComponentName cn = mContext.getPackageManager()
                    .getLaunchIntentForPackage(info.packageName) != null
                    ? mContext.getPackageManager()
                            .getLaunchIntentForPackage(info.packageName).getComponent()
                    : null;
            if (cn != null) {
                Drawable packIcon = mIconPackManager.getIconForComponent(cn, density);
                if (packIcon != null) {
                    return maybeTintIconPackBackground(maybeWrapAdaptive(packIcon));
                }
            }
        }

        // Tries to load the round icon res, if the app defines it as an adaptive icon
        if (mThemeManager.getIconShape() instanceof ShapeDelegate.Circle) {
            int roundIconRes = mApiWrapper.getRoundIconRes(info);
            if (roundIconRes != 0 && roundIconRes != info.icon) {
                try {
                    Drawable d = resources.getDrawableForDensity(roundIconRes, density);
                    if (d instanceof AdaptiveIconDrawable) {
                        return maybeWrapAdaptive(d);
                    }
                } catch (Resources.NotFoundException exc) { }
            }
        }
        return maybeWrapAdaptive(super.loadAppInfoIcon(info, resources, density));
    }

    /**
     * Migration02 Phase 3.5 — when {@link LauncherPrefs#TINT_ICON_PACK_BG} is on AND any
     * flavour of theming is on (global or drawer-only), funnel the icon-pack
     * {@link AdaptiveIconDrawable} through {@link MonochromeIconFactory#wrap} so the resulting
     * {@code BitmapInfo} is generated from a mono-foreground layer. Icon-packs typically
     * don't ship a {@code getMonochrome()} layer, so this is the only way the AOSP themed
     * pipeline ({@link com.android.launcher3.icons.BaseIconFactory#mThemeController}) sees
     * an icon it can theme.
     *
     * <p>PLAN-DRIFT-M02: see Plans/Migration02/13-drift-log.md — when
     * {@code DRAWER_THEMED_ICONS=true && THEMED_ICONS=false}, this wrap collapses the
     * regular icon variant to mono on the workspace too. That's the consequence of doing
     * the wrap at the icon-cache layer (which has no per-display awareness). The plan
     * accepts this trade-off per §3.5 (`(THEMED_ICONS || drawer-only-applies)`).
     */
    @Nullable
    private Drawable maybeTintIconPackBackground(@Nullable Drawable d) {
        if (!(d instanceof AdaptiveIconDrawable adaptive)) return d;
        try {
            LauncherPrefs prefs = LauncherPrefs.get(mContext);
            if (!prefs.get(LauncherPrefs.TINT_ICON_PACK_BG)) return d;
            boolean anyThemed = prefs.get(ThemeManager.THEMED_ICONS)
                    || prefs.get(LauncherPrefs.DRAWER_THEMED_ICONS);
            if (!anyThemed) return d;
            // Use the canonical 56dp default icon-bitmap size; matches what
            // BaseIconFactory derives at runtime when no override is configured.
            int iconBitmapSize = (int) (56 * mContext.getResources().getDisplayMetrics().density);
            com.android.launcher3.icons.MonochromeIconFactory mono =
                    new com.android.launcher3.icons.MonochromeIconFactory(iconBitmapSize);
            android.graphics.Path shapePath = Utilities.getIconShapeOrNull(mContext);
            if (shapePath == null) {
                // launcher-icon-shapes flag off: ClippedMonoDrawable.draw won't reference
                // shapePath, but the constructor still requires non-null. Supply a full-canvas
                // rect path (cheap) to satisfy the contract.
                shapePath = new android.graphics.Path();
                shapePath.addRect(0f, 0f, 100f, 100f, android.graphics.Path.Direction.CW);
            }
            return mono.wrap(adaptive, shapePath, 1f);
        } catch (Throwable ignored) {
            return d;
        }
    }

    /**
     * If the user has icon-shape wrapping enabled and the supplied drawable is
     * a vanilla {@link AdaptiveIconDrawable}, re-wrap it with
     * {@link CustomAdaptiveIconDrawable} so the active shape mask is honored.
     */
    @Nullable
    private Drawable maybeWrapAdaptive(@Nullable Drawable d) {
        if (d == null) return null;
        try {
            if (LauncherPrefs.get(mContext).get(LauncherPrefs.WRAP_ADAPTIVE_ICONS)) {
                return CustomAdaptiveIconDrawable.wrap(d);
            }
        } catch (Throwable ignored) {
            // Pref system not yet initialized — return raw drawable.
        }
        return d;
    }

    private Map<String, ThemeData> getThemedIconMap() {
        if (mThemedIconMap != null) {
            return mThemedIconMap;
        }
        ArrayMap<String, ThemeData> map = new ArrayMap<>();
        Resources res = mContext.getResources();
        try (XmlResourceParser parser = res.getXml(R.xml.grayscale_icon_map)) {
            final int depth = parser.getDepth();
            int type;
            while ((type = parser.next()) != XmlPullParser.START_TAG
                    && type != XmlPullParser.END_DOCUMENT);

            while (((type = parser.next()) != XmlPullParser.END_TAG
                    || parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
                if (type != XmlPullParser.START_TAG) {
                    continue;
                }
                if (TAG_ICON.equals(parser.getName())) {
                    String pkg = parser.getAttributeValue(null, ATTR_PACKAGE);
                    int iconId = parser.getAttributeResourceValue(null, ATTR_DRAWABLE, 0);
                    if (iconId != 0 && !TextUtils.isEmpty(pkg)) {
                        map.put(pkg, new ThemeData(res, iconId));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to parse icon map", e);
        }
        mThemedIconMap = map;
        return mThemedIconMap;
    }
}
