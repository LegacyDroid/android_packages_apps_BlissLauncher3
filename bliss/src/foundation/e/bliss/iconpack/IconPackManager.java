/*
 * Copyright (C) 2025 MURENA SAS
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
package foundation.e.bliss.iconpack;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages icon pack loading and icon resolution. Parses appfilter.xml from
 * third-party icon packs and provides drawable lookup by ComponentName.
 *
 * Ported from Lawnchair's CustomIconPack, simplified to a single-class
 * implementation.
 */
public class IconPackManager {

    private static final String TAG = "IconPackManager";

    private final Context mContext;
    private String mCurrentPackPackage;
    private Resources mPackResources;

    // Maps ComponentName (flattened string) → drawable name in the icon pack
    private final Map<String, String> mComponentToDrawable = new HashMap<>();

    // Caches drawable resource IDs to avoid repeated lookups
    private final Map<String, Integer> mDrawableIdCache = new HashMap<>();

    public IconPackManager(Context context) {
        mContext = context;
    }

    /**
     * Load an icon pack by package name. Parses appfilter.xml to build the
     * component-to-drawable mapping. Call this when the icon pack preference
     * changes.
     *
     * @param packageName
     *            The icon pack package name, or empty/null for system default.
     * @return true if the pack was loaded successfully.
     */
    public boolean loadIconPack(String packageName) {
        mComponentToDrawable.clear();
        mDrawableIdCache.clear();
        mPackResources = null;
        mCurrentPackPackage = null;

        if (TextUtils.isEmpty(packageName)) {
            return false;
        }

        try {
            PackageManager pm = mContext.getPackageManager();
            mPackResources = pm.getResourcesForApplication(packageName);
            mCurrentPackPackage = packageName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Icon pack not found: " + packageName, e);
            return false;
        }

        parseAppFilter();
        return !mComponentToDrawable.isEmpty();
    }

    /**
     * Parse appfilter.xml from the icon pack to build component→drawable mapping.
     * Supports both XML resource and assets fallback.
     */
    private void parseAppFilter() {
        XmlPullParser parser = getXmlParser("appfilter");
        if (parser == null)
            return;

        try {
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() != XmlPullParser.START_TAG)
                    continue;
                if ("item".equals(parser.getName())) {
                    handleAppFilterItem(parser);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error parsing appfilter.xml from " + mCurrentPackPackage, e);
        }
    }

    private void handleAppFilterItem(XmlPullParser parser) {
        String component = parser.getAttributeValue(null, "component");
        String drawable = parser.getAttributeValue(null, "drawable");
        if (component == null || drawable == null) {
            return;
        }
        // Normalize ComponentInfo{pkg/class} → pkg/class
        final String compStart = "ComponentInfo{";
        if (component.startsWith(compStart) && component.endsWith("}")) {
            component = component.substring(compStart.length(), component.length() - 1);
        }
        ComponentName cn = ComponentName.unflattenFromString(component);
        if (cn != null) {
            mComponentToDrawable.put(cn.flattenToString(), drawable);
        }
    }

    /**
     * Get an XML parser for the given resource name from the icon pack. Tries XML
     * resource first, then assets fallback.
     */
    private XmlPullParser getXmlParser(String name) {
        if (mPackResources == null || mCurrentPackPackage == null)
            return null;

        try {
            // Try XML resource
            int resId = mPackResources.getIdentifier(name, "xml", mCurrentPackPackage);
            if (resId != 0) {
                return mContext.getPackageManager().getXml(mCurrentPackPackage, resId, null);
            }
        } catch (Exception e) {
            Log.d(TAG, "No XML resource for " + name + ", trying assets");
        }

        try {
            // Fallback: assets
            InputStream is = mPackResources.getAssets().open(name + ".xml");
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(is, "UTF-8");
            return parser;
        } catch (Exception e) {
            Log.d(TAG, "No asset XML for " + name + " in " + mCurrentPackPackage);
        }
        return null;
    }

    /**
     * Get the icon drawable for a given component from the loaded icon pack.
     *
     * @param componentName
     *            The app's ComponentName
     * @param density
     *            The desired icon density (e.g., DisplayMetrics.DENSITY_XXHIGH)
     * @return The icon drawable from the pack, or null if not found
     */
    public Drawable getIconForComponent(ComponentName componentName, int density) {
        if (mPackResources == null || mCurrentPackPackage == null)
            return null;

        String drawableName = mComponentToDrawable.get(componentName.flattenToString());
        if (drawableName == null)
            return null;

        int resId = getDrawableResId(drawableName);
        if (resId == 0)
            return null;

        try {
            return mPackResources.getDrawableForDensity(resId, density, null);
        } catch (Resources.NotFoundException e) {
            return null;
        }
    }

    /**
     * Check if the icon pack has an icon for the given component.
     */
    public boolean hasIconForComponent(ComponentName componentName) {
        if (mCurrentPackPackage == null)
            return false;
        return mComponentToDrawable.containsKey(componentName.flattenToString());
    }

    private int getDrawableResId(String drawableName) {
        Integer cached = mDrawableIdCache.get(drawableName);
        if (cached != null)
            return cached;

        int id = mPackResources.getIdentifier(drawableName, "drawable", mCurrentPackPackage);
        mDrawableIdCache.put(drawableName, id);
        return id;
    }

    /** @return true if an icon pack is currently loaded */
    public boolean isIconPackLoaded() {
        return mCurrentPackPackage != null && !mComponentToDrawable.isEmpty();
    }

    /** @return the currently loaded icon pack package name, or null */
    public String getCurrentPackPackage() {
        return mCurrentPackPackage;
    }

    /** @return the number of icons in the loaded pack */
    public int getIconCount() {
        return mComponentToDrawable.size();
    }
}
