/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.launcher3.allapps.search;

import static com.android.launcher3.allapps.BaseAllAppsAdapter.VIEW_TYPE_EMPTY_SEARCH;
import static com.android.launcher3.util.Executors.MAIN_EXECUTOR;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Process;
import android.os.UserManager;
import android.provider.ContactsContract;
import android.provider.MediaStore;

import androidx.annotation.AnyThread;

import com.android.launcher3.AppFilter;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.allapps.BaseAllAppsAdapter.AdapterItem;
import com.android.launcher3.dagger.LauncherComponentProvider;
import com.android.launcher3.icons.BitmapInfo;
import com.android.launcher3.icons.LauncherIcons;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.search.SearchAlgorithm;
import com.android.launcher3.search.SearchCallback;
import com.android.launcher3.search.StringMatcherUtility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import android.content.pm.LauncherApps.ShortcutQuery;

/**
 * The default search implementation.
 */
public class DefaultAppSearchAlgorithm implements SearchAlgorithm<AdapterItem> {

    private static final int DEFAULT_MAX_RESULTS_COUNT = 5;

    private final LauncherAppState mAppState;
    private final Handler mResultHandler;
    private final boolean mAddNoResultsMessage;

    public DefaultAppSearchAlgorithm(Context context) {
        this(context, false);
    }

    public DefaultAppSearchAlgorithm(Context context, boolean addNoResultsMessage) {
        mAppState = LauncherAppState.getInstance(context);
        mResultHandler = new Handler(MAIN_EXECUTOR.getLooper());
        mAddNoResultsMessage = addNoResultsMessage;
    }

    @Override
    public void cancel(boolean interruptActiveRequests) {
        if (interruptActiveRequests) {
            mResultHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void doSearch(String query, SearchCallback<AdapterItem> callback) {
        mAppState.getModel().enqueueModelUpdateTask((taskController, dataModel, apps) ->  {
            Context ctx = mAppState.getContext();
            ArrayList<AdapterItem> result = getTitleMatchResult(ctx, apps.data, query);

            // Calculator: prepend math result if query is a math expression
            LauncherPrefs calcPrefs = LauncherComponentProvider.get(ctx).getLauncherPrefs();
            boolean showCalc = calcPrefs.get(LauncherPrefs.SHOW_CALCULATOR);
            if (showCalc) {
                String calcResult = evaluateMathExpression(query.trim());
                if (calcResult != null) {
                    AppInfo calcInfo = new AppInfo();
                    calcInfo.title = query.trim() + " = " + calcResult;
                    result.add(0, AdapterItem.asApp(calcInfo));
                }
            }

            // Contact search: append matching contacts. Lawnchair's SEARCH_PEOPLE pref
            // is treated as an OR with CONTACT_SEARCH so imports light up the feature.
            try {
                LauncherPrefs lp = LauncherComponentProvider.get(ctx).getLauncherPrefs();
                boolean contactSearch = lp.get(LauncherPrefs.CONTACT_SEARCH)
                        || lp.get(LauncherPrefs.SEARCH_PEOPLE);
                if (contactSearch && query.length() >= 2) {
                    ArrayList<AdapterItem> contacts = searchContacts(ctx, query.trim(), 3);
                    result.addAll(contacts);
                }
            } catch (Exception e) { /* permission or pref not available */ }

            // Shortcut search: append matching app shortcuts
            try {
                boolean shortcutSearch = LauncherComponentProvider.get(ctx).getLauncherPrefs()
                        .get(LauncherPrefs.SHORTCUT_SEARCH);
                if (shortcutSearch && query.length() >= 2) {
                    ArrayList<AdapterItem> shortcuts = searchShortcuts(ctx, query.trim(), 3);
                    result.addAll(shortcuts);
                }
            } catch (Exception e) { /* permission or pref not available */ }

            // Settings entries: open the system Settings app pointed at the matched section.
            try {
                LauncherPrefs lp = LauncherComponentProvider.get(ctx).getLauncherPrefs();
                if (lp.get(LauncherPrefs.SEARCH_SETTINGS) && query.length() >= 2) {
                    result.addAll(searchSettings(ctx, query.trim(), 3));
                }
            } catch (Exception ignored) { /* search disabled or pref unavailable */ }

            // File search: query MediaStore.Files for filename matches.
            try {
                LauncherPrefs lp = LauncherComponentProvider.get(ctx).getLauncherPrefs();
                if (lp.get(LauncherPrefs.SEARCH_FILES) && query.length() >= 3) {
                    result.addAll(searchMediaStore(ctx, query.trim(), 3, /*audioOnly=*/false));
                }
            } catch (Exception ignored) { /* search disabled or pref unavailable */ }

            // Audio search
            try {
                LauncherPrefs lp = LauncherComponentProvider.get(ctx).getLauncherPrefs();
                if (lp.get(LauncherPrefs.SEARCH_AUDIO) && query.length() >= 3) {
                    result.addAll(searchAudio(ctx, query.trim(), 3));
                }
            } catch (Exception ignored) { /* search disabled or pref unavailable */ }

            // Visual media (photos + videos) — Phase 6.1: separate MediaStore backend so
            // images/videos surface even when the Files toggle is off, and they show
            // gallery-style with proper view intents.
            try {
                LauncherPrefs lp = LauncherComponentProvider.get(ctx).getLauncherPrefs();
                if (lp.get(LauncherPrefs.SEARCH_VISUAL_MEDIA) && query.length() >= 3) {
                    result.addAll(searchVisualMedia(ctx, query.trim(), 3));
                }
            } catch (Exception ignored) { /* search disabled or pref unavailable */ }

            // Suggested apps: when no query is typed and SHOW_SUGGESTED_APPS is on,
            // surface the top 3 most-used apps as a suggestion header in the drawer.
            try {
                LauncherPrefs lp = LauncherComponentProvider.get(ctx).getLauncherPrefs();
                if (lp.get(LauncherPrefs.SHOW_SUGGESTED_APPS) && query.trim().isEmpty()
                        && result.isEmpty()) {
                    java.util.List<android.app.usage.UsageStats> stats =
                            new foundation.e.bliss.suggestions.AppUsageStats(ctx).getUsageStats();
                    int added = 0;
                    for (android.app.usage.UsageStats us : stats) {
                        if (added >= 3) break;
                        for (com.android.launcher3.model.data.AppInfo info : apps.data) {
                            if (info.componentName != null
                                    && info.componentName.getPackageName()
                                            .equals(us.getPackageName())) {
                                result.add(AdapterItem.asApp(info));
                                added++;
                                break;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) { /* usage-stats permission missing or pref off */ }

            if (mAddNoResultsMessage && result.isEmpty()) {
                result.add(getEmptyMessageAdapterItem(query));
            }
            mResultHandler.post(() -> callback.onSearchResult(query, result));
        });
    }

    private static AdapterItem getEmptyMessageAdapterItem(String query) {
        AdapterItem item = new AdapterItem(VIEW_TYPE_EMPTY_SEARCH);
        // Add a place holder info to propagate the query
        AppInfo placeHolder = new AppInfo();
        placeHolder.title = query;
        item.itemInfo = placeHolder;
        return item;
    }

    /**
     * Filters {@link AppInfo}s matching specified query
     */
    @AnyThread
    public static ArrayList<AdapterItem> getTitleMatchResult(Context context, List<AppInfo> apps,
            String query) {
        // Do an intersection of the words in the query and each title, and filter out all the
        // apps that don't match all of the words in the query.
        final String queryTextLower = query.toLowerCase();
        final ArrayList<AdapterItem> result = new ArrayList<>();
        StringMatcherUtility.StringMatcher matcher =
                StringMatcherUtility.StringMatcher.getInstance();

        LauncherPrefs prefs = LauncherComponentProvider.get(context).getLauncherPrefs();

        Set<String> hiddenApps = prefs.get(LauncherPrefs.HIDDEN_APPS);
        if (hiddenApps == null) {
            hiddenApps = Collections.emptySet();
        }
        // "never" (default): always skip hidden apps in search
        // "always": include hidden apps in search results
        // "if_name_typed": include only when query exactly matches the app's full title
        final String hiddenPolicy = prefs.get(LauncherPrefs.HIDDEN_APPS_IN_SEARCH);

        int maxResults = prefs.get(LauncherPrefs.SEARCH_RESULT_COUNT);
        if (maxResults <= 0) {
            maxResults = DEFAULT_MAX_RESULTS_COUNT;
        }

        boolean fuzzyEnabled = prefs.get(LauncherPrefs.FUZZY_SEARCH);
        ArrayList<AdapterItem> fuzzyResults = new ArrayList<>();

        int resultCount = 0;
        int total = apps.size();
        UserManager userManager = UserManager.get(context);
        for (int i = 0; i < total; i++) {
            AppInfo info = apps.get(i);
            if (userManager.isQuietModeEnabled(info.user)) {
                continue;
            }
            if (hiddenApps.contains(info.componentName.getPackageName())) {
                if ("never".equals(hiddenPolicy)) {
                    continue;
                } else if ("if_name_typed".equals(hiddenPolicy)
                        && !info.title.toString().equalsIgnoreCase(query)) {
                    continue;
                }
                // else "always" — fall through, include in search results
            }
            if (StringMatcherUtility.matches(queryTextLower, info.title.toString(), matcher)) {
                if (resultCount < maxResults) {
                    result.add(AdapterItem.asApp(info));
                    resultCount++;
                }
            } else if (fuzzyEnabled && resultCount + fuzzyResults.size() < maxResults) {
                // Subsequence match: all query chars appear in order in the title
                if (isSubsequenceMatch(queryTextLower, info.title.toString().toLowerCase())) {
                    fuzzyResults.add(AdapterItem.asApp(info));
                }
            }
        }
        // Append fuzzy results after exact matches, up to max
        int remaining = maxResults - result.size();
        for (int i = 0; i < Math.min(remaining, fuzzyResults.size()); i++) {
            result.add(fuzzyResults.get(i));
        }
        return result;
    }

    /** Returns true if all characters of query appear in target in order. */
    private static boolean isSubsequenceMatch(String query, String target) {
        int qi = 0;
        int ql = query.length();
        int tl = target.length();
        for (int ti = 0; ti < tl && qi < ql; ti++) {
            if (query.charAt(qi) == target.charAt(ti)) {
                qi++;
            }
        }
        return qi == ql;
    }

    private static final Pattern MATH_PATTERN =
            Pattern.compile("^[\\d+\\-*/().\\s]+$");

    /**
     * Evaluates a simple math expression containing +, -, *, /, parentheses.
     * Returns the result as a string, or null if the input is not a valid expression.
     */
    static String evaluateMathExpression(String expr) {
        if (expr == null || expr.isEmpty()) return null;
        // Must contain at least one operator to be a math expression
        if (!expr.contains("+") && !expr.contains("-") && !expr.contains("*")
                && !expr.contains("/")) {
            return null;
        }
        if (!MATH_PATTERN.matcher(expr).matches()) return null;
        try {
            double result = evalExpr(expr.replaceAll("\\s", ""), new int[]{0});
            if (Double.isInfinite(result) || Double.isNaN(result)) return null;
            // Format: remove trailing .0 for integer results
            if (result == Math.floor(result) && !Double.isInfinite(result)
                    && Math.abs(result) < 1e15) {
                return String.valueOf((long) result);
            }
            return String.valueOf(result);
        } catch (Exception e) {
            return null;
        }
    }

    // Recursive descent parser: expr -> term ((+|-) term)*
    private static double evalExpr(String s, int[] pos) {
        double result = evalTerm(s, pos);
        while (pos[0] < s.length()) {
            char op = s.charAt(pos[0]);
            if (op == '+' || op == '-') {
                pos[0]++;
                double term = evalTerm(s, pos);
                result = (op == '+') ? result + term : result - term;
            } else {
                break;
            }
        }
        return result;
    }

    // term -> factor ((*|/) factor)*
    private static double evalTerm(String s, int[] pos) {
        double result = evalFactor(s, pos);
        while (pos[0] < s.length()) {
            char op = s.charAt(pos[0]);
            if (op == '*' || op == '/') {
                pos[0]++;
                double factor = evalFactor(s, pos);
                result = (op == '*') ? result * factor : result / factor;
            } else {
                break;
            }
        }
        return result;
    }

    // factor -> number | '(' expr ')' | unary minus
    private static double evalFactor(String s, int[] pos) {
        if (pos[0] < s.length() && s.charAt(pos[0]) == '(') {
            pos[0]++; // skip '('
            double result = evalExpr(s, pos);
            if (pos[0] < s.length() && s.charAt(pos[0]) == ')') {
                pos[0]++; // skip ')'
            }
            return result;
        }
        // Handle unary minus
        boolean negative = false;
        if (pos[0] < s.length() && s.charAt(pos[0]) == '-') {
            negative = true;
            pos[0]++;
        }
        int start = pos[0];
        while (pos[0] < s.length()
                && (Character.isDigit(s.charAt(pos[0])) || s.charAt(pos[0]) == '.')) {
            pos[0]++;
        }
        if (start == pos[0]) throw new NumberFormatException("No number at pos " + pos[0]);
        double num = Double.parseDouble(s.substring(start, pos[0]));
        return negative ? -num : num;
    }

    /**
     * Search device contacts matching the query. Returns results as fake AppInfo items
     * with the contact name as title and an intent to view the contact.
     */
    private static ArrayList<AdapterItem> searchContacts(Context context, String query,
            int maxResults) {
        ArrayList<AdapterItem> results = new ArrayList<>();
        try {
            Uri uri = ContactsContract.Contacts.CONTENT_URI;
            String[] projection = {
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.LOOKUP_KEY
            };
            String selection = ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " LIKE ?";
            String[] selectionArgs = { "%" + query + "%" };
            String sortOrder = ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " ASC";

            Cursor cursor = context.getContentResolver().query(
                    uri, projection, selection, selectionArgs, sortOrder);
            if (cursor != null) {
                while (cursor.moveToNext() && results.size() < maxResults) {
                    String name = cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY));
                    String lookupKey = cursor.getString(
                            cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY));
                    long contactId = cursor.getLong(
                            cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));

                    AppInfo contactInfo = new AppInfo();
                    contactInfo.title = "\uD83D\uDC64 " + name; // 👤 prefix
                    contactInfo.intent = new Intent(Intent.ACTION_VIEW,
                            ContactsContract.Contacts.getLookupUri(contactId, lookupKey));
                    results.add(AdapterItem.asApp(contactInfo));
                }
                cursor.close();
            }
        } catch (Exception e) {
            // No contacts permission or query failed
        }
        return results;
    }

    /**
     * Search app shortcuts matching the query. Returns results as fake AppInfo items
     * with the shortcut label as title.
     */
    private static ArrayList<AdapterItem> searchShortcuts(Context context, String query,
            int maxResults) {
        ArrayList<AdapterItem> results = new ArrayList<>();
        try {
            LauncherApps launcherApps = (LauncherApps) context.getSystemService(
                    Context.LAUNCHER_APPS_SERVICE);
            if (launcherApps == null) return results;

            ShortcutQuery shortcutQuery = new ShortcutQuery();
            shortcutQuery.setQueryFlags(ShortcutQuery.FLAG_MATCH_DYNAMIC
                    | ShortcutQuery.FLAG_MATCH_MANIFEST);

            List<ShortcutInfo> shortcuts = launcherApps.getShortcuts(
                    shortcutQuery, Process.myUserHandle());
            if (shortcuts == null) return results;

            String queryLower = query.toLowerCase();
            for (ShortcutInfo shortcut : shortcuts) {
                if (results.size() >= maxResults) break;

                CharSequence label = shortcut.getShortLabel();
                if (label == null) label = shortcut.getLongLabel();
                if (label == null) continue;

                String labelStr = label.toString();
                if (labelStr.toLowerCase().contains(queryLower)) {
                    AppInfo shortcutInfo = new AppInfo();
                    shortcutInfo.title = "\u2197 " + labelStr; // ↗ prefix
                    ComponentName activity = shortcut.getActivity();
                    if (activity != null) {
                        shortcutInfo.intent = new Intent(Intent.ACTION_MAIN)
                                .setComponent(activity)
                                .addCategory(Intent.CATEGORY_LAUNCHER);
                    }
                    results.add(AdapterItem.asApp(shortcutInfo));
                }
            }
        } catch (Exception e) {
            // No shortcut permission or query failed
        }
        return results;
    }

    /**
     * Search system Settings activities whose label matches the query. Each match
     * surfaces as a fake AppInfo whose intent is the Settings entry the user can tap.
     */
    private static ArrayList<AdapterItem> searchSettings(Context context, String query,
            int maxResults) {
        ArrayList<AdapterItem> results = new ArrayList<>();
        try {
            String lower = query.toLowerCase();
            Intent search = new Intent(Intent.ACTION_MAIN);
            search.setPackage("com.android.settings");
            java.util.List<android.content.pm.ResolveInfo> infos =
                    context.getPackageManager().queryIntentActivities(search, 0);
            for (android.content.pm.ResolveInfo ri : infos) {
                if (results.size() >= maxResults) break;
                CharSequence label = ri.loadLabel(context.getPackageManager());
                if (label == null) continue;
                if (!label.toString().toLowerCase().contains(lower)) continue;
                AppInfo info = new AppInfo();
                info.title = label.toString();
                info.bitmap = bitmapFromDrawableRes(context,
                        R.drawable.ic_search_result_settings);
                info.intent = new Intent()
                        .setClassName(ri.activityInfo.packageName, ri.activityInfo.name)
                        .setAction(Intent.ACTION_MAIN);
                results.add(AdapterItem.asApp(info));
            }
        } catch (Exception ignored) { /* settings query unavailable */ }
        return results;
    }

    /**
     * MediaStore filename search; audioOnly=true filters to MediaStore.Audio.
     * For the non-audio (general "Files") path, image/% and video/% MIME types are
     * excluded so the dedicated visual-media backend (Phase 6.1) is the single source
     * for those types — no double-show.
     */
    private static ArrayList<AdapterItem> searchMediaStore(Context context, String query,
            int maxResults, boolean audioOnly) {
        ArrayList<AdapterItem> results = new ArrayList<>();
        try {
            Uri uri = audioOnly
                    ? MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    : MediaStore.Files.getContentUri("external");
            String nameCol = audioOnly
                    ? MediaStore.Audio.Media.TITLE
                    : MediaStore.Files.FileColumns.DISPLAY_NAME;
            String[] projection = { MediaStore.Files.FileColumns._ID, nameCol };
            String selection;
            String[] selectionArgs;
            if (audioOnly) {
                selection = nameCol + " LIKE ?";
                selectionArgs = new String[] { "%" + query + "%" };
            } else {
                // Files backend: exclude image/% and video/% so the visual-media backend
                // owns those types exclusively (Phase 6.1).
                selection = nameCol + " LIKE ? AND ("
                        + MediaStore.Files.FileColumns.MIME_TYPE + " IS NULL OR ("
                        + MediaStore.Files.FileColumns.MIME_TYPE + " NOT LIKE ? AND "
                        + MediaStore.Files.FileColumns.MIME_TYPE + " NOT LIKE ?))";
                selectionArgs = new String[] { "%" + query + "%", "image/%", "video/%" };
            }
            Cursor c = context.getContentResolver().query(uri, projection, selection,
                    selectionArgs, nameCol + " ASC LIMIT " + maxResults);
            if (c != null) {
                int iconRes = audioOnly
                        ? R.drawable.ic_search_result_audio
                        : R.drawable.ic_search_result_file;
                while (c.moveToNext() && results.size() < maxResults) {
                    String name = c.getString(c.getColumnIndexOrThrow(nameCol));
                    AppInfo info = new AppInfo();
                    info.title = name;
                    info.bitmap = bitmapFromDrawableRes(context, iconRes);
                    info.intent = new Intent(Intent.ACTION_VIEW);
                    results.add(AdapterItem.asApp(info));
                }
                c.close();
            }
        } catch (Exception ignored) { /* MediaStore query unavailable */ }
        return results;
    }

    private static ArrayList<AdapterItem> searchAudio(Context context, String query,
            int maxResults) {
        return searchMediaStore(context, query, maxResults, /*audioOnly=*/true);
    }

    /**
     * Phase 6.1 visual-media backend: queries MediaStore.Files for image/% and
     * video/% MIME types ordered by DATE_MODIFIED DESC, mints a real ACTION_VIEW
     * intent at the typed content URI so taps open the gallery / video viewer.
     */
    private static ArrayList<AdapterItem> searchVisualMedia(Context ctx, String query,
            int max) {
        ArrayList<AdapterItem> results = new ArrayList<>();
        String[] types = { "image/%", "video/%" };
        for (String mime : types) {
            if (results.size() >= max) break;
            try {
                Uri uri = MediaStore.Files.getContentUri("external");
                String[] proj = {
                        MediaStore.Files.FileColumns._ID,
                        MediaStore.Files.FileColumns.DISPLAY_NAME,
                        MediaStore.Files.FileColumns.MIME_TYPE
                };
                String sel = MediaStore.Files.FileColumns.MIME_TYPE + " LIKE ? AND "
                        + MediaStore.Files.FileColumns.DISPLAY_NAME + " LIKE ?";
                String[] args = { mime, "%" + query + "%" };
                int remaining = max - results.size();
                Cursor c = ctx.getContentResolver().query(uri, proj, sel, args,
                        MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC LIMIT "
                                + remaining);
                if (c != null) {
                    while (c.moveToNext() && results.size() < max) {
                        String name = c.getString(c.getColumnIndexOrThrow(
                                MediaStore.Files.FileColumns.DISPLAY_NAME));
                        String type = c.getString(c.getColumnIndexOrThrow(
                                MediaStore.Files.FileColumns.MIME_TYPE));
                        long id = c.getLong(c.getColumnIndexOrThrow(
                                MediaStore.Files.FileColumns._ID));
                        AppInfo info = new AppInfo();
                        info.title = name;
                        int iconRes = type != null && type.startsWith("video")
                                ? R.drawable.ic_search_result_video
                                : R.drawable.ic_search_result_image;
                        info.bitmap = bitmapFromDrawableRes(ctx, iconRes);
                        info.intent = new Intent(Intent.ACTION_VIEW)
                                .setDataAndType(ContentUris.withAppendedId(uri, id), type);
                        results.add(AdapterItem.asApp(info));
                    }
                    c.close();
                }
            } catch (Exception ignored) { /* MediaStore query unavailable */ }
        }
        return results;
    }

    /**
     * Phase 6.5: convert a vector drawable resource into a launcher-ready
     * {@link BitmapInfo}. Used by the search backends to attach a type icon to
     * each result instead of the previous unicode-emoji prefix.
     *
     * PLAN-DRIFT-M02: see 13-drift-log.md entry for Phase 6.5 — the plan's
     * literal `BitmapInfo.fromIconRes(ctx, res)` factory does not exist in this
     * fork; we go through `LauncherIcons.createBadgedIconBitmap` instead.
     */
    private static BitmapInfo bitmapFromDrawableRes(Context ctx, int drawableRes) {
        try {
            Drawable d = ctx.getDrawable(drawableRes);
            if (d == null) return BitmapInfo.LOW_RES_INFO;
            try (LauncherIcons li = LauncherIcons.obtain(ctx)) {
                return li.createBadgedIconBitmap(d);
            }
        } catch (Throwable ignored) {
            return BitmapInfo.LOW_RES_INFO;
        }
    }
}
