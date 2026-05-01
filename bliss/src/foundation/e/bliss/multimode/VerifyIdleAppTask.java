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
 *
 */
/*
 * Bliss touchpoint(s) (Migration04):
 *   - Phase 05 evaluated wiring this task to LauncherPolicy.idleApp(ctx)
 *     (HonorGapsIdleAppPolicy.shouldAutoFillIdle() returns false). The audit
 *     found the Migration03 plan's preserve-gaps early-return was previously
 *     present here but had to be REMOVED to fix a regression where newly
 *     installed apps stopped reaching the workspace. The gap-preservation
 *     guarantee is now upheld by WorkspaceItemSpaceFinder, which honours the
 *     pref by placing new items AFTER the last occupied cell rather than
 *     first-fit-from-top-left. Adding a `shouldAutoFillIdle()` gate here
 *     would re-introduce that regression, so the task deliberately does NOT
 *     consult IdleAppPolicy. The policy + LauncherPolicy.idleApp() machinery
 *     is kept available in foundation.e.bliss.policy.idle for future call
 *     sites that legitimately need the toggle.
 *     — Plan ref: Plans/Migration04/05-launcher-policy-strategies.md §5.4
 *
 * The body of this file otherwise tracks Bliss baseline behaviour.
 */
package foundation.e.bliss.multimode;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.os.UserHandle;
import android.util.Pair;

import androidx.annotation.WorkerThread;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.icons.cache.CacheLookupFlag;
import com.android.launcher3.R;
import com.android.launcher3.model.BgDataModel;
import com.android.launcher3.model.ItemInstallQueue;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.ItemInfoMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import foundation.e.bliss.utils.Logger;

@WorkerThread
public class VerifyIdleAppTask implements Runnable {
    private static final String TAG = "VerifyIdleAppTask";

    private final List<String> mBlacklistedApps;

    private final Context mContext;
    private final Collection<AppInfo> mApps;
    private final String mPackageNames;
    private final UserHandle mUser;
    private final boolean mIsAddPackage;
    private boolean mIgnoreLoaded;

    private BgDataModel mBgdataModel;

    public VerifyIdleAppTask(Context context, Collection<AppInfo> apps, String packageNames, UserHandle user,
            boolean isAdd, BgDataModel bgDataModel) {
        mContext = context;
        mApps = apps;
        mPackageNames = packageNames;
        mUser = user;
        mIsAddPackage = isAdd;
        mBgdataModel = bgDataModel;
        mBlacklistedApps = Arrays.asList(context.getResources().getStringArray(R.array.blacklisted_apps));
    }

    private static void verifyShortcutHighRes(Context context, AppInfo appInfo) {
        if (appInfo != null) {
            CacheLookupFlag flag = appInfo.getMatchingLookupFlag();
            if (flag.useLowRes()) {
                LauncherAppState.getInstance(context).getIconCache().getTitleAndIcon(appInfo, flag);
            }
        }
    }

    @Override
    public void run() {
        if (!MultiModeController.isSingleLayerMode()) {
            return;
        }
        // Note: we deliberately do NOT bail out via LauncherPolicy.idleApp()
        // here. See the file-header touchpoint block above — bailing would
        // prevent newly installed apps from appearing on the home screen.
        // WorkspaceItemSpaceFinder upholds the gap-preservation guarantee
        // by placing new items AFTER the last occupied cell instead of
        // first-fit-from-top-left.

        final Map<ComponentKey, Object> map = new HashMap<>();
        if (mApps != null && mApps.size() > 0) {
            // All apps loading, we ignore loaded.
            mIgnoreLoaded = true;
            for (AppInfo app : mApps) {
                if (mBlacklistedApps.stream().noneMatch(
                        pkg -> pkg.equals(Objects.requireNonNull(app.getTargetPackage()).trim().toLowerCase()))) {
                    map.put(new ComponentKey(app.componentName, app.user), app);
                }
            }
        } else if (mPackageNames != null && mUser != null) {
            // App add or update, should not ignore loaded.
            mIgnoreLoaded = false;
            final LauncherApps launcherApps = mContext.getSystemService(LauncherApps.class);
            if (mIsAddPackage || launcherApps.isPackageEnabled(mPackageNames, mUser)) {
                final List<LauncherActivityInfo> infos = launcherApps.getActivityList(mPackageNames, mUser);
                for (LauncherActivityInfo info : infos) {
                    map.put(new ComponentKey(info.getComponentName(), info.getUser()), info);
                }
            }
        }

        verifyAllApps(mContext, map, mIsAddPackage);
    }

    List<Pair<ItemInfo, Object>> verifyAllApps(Context context, Map<ComponentKey, Object> map, boolean animated) {
        List<Pair<ItemInfo, Object>> newItems = new ArrayList<>();
        synchronized (mBgdataModel.mLock) {
            for (Map.Entry<ComponentKey, Object> entry : map.entrySet()) {
                ComponentKey componentKey = entry.getKey();
                HashSet<ComponentName> components = new HashSet<>(1);
                components.add(componentKey.componentName);
                Predicate<ItemInfo> matcher = ItemInfoMatcher.ofComponents(components, componentKey.user);
                if (mBgdataModel.itemsIdMap.stream().noneMatch(matcher)) {
                    Object obj = entry.getValue();
                    if (obj instanceof AppInfo) {
                        verifyShortcutHighRes(context, (AppInfo) obj);
                        newItems.add(Pair.create((AppInfo) obj, null));
                    } else if (obj instanceof LauncherActivityInfo) {
                        LauncherActivityInfo info = (LauncherActivityInfo) obj;
                        newItems.add(new ItemInstallQueue.PendingInstallShortcutInfo(
                                info.getApplicationInfo().packageName, info.getUser()).getItemInfo(context));
                    }
                    Logger.d(TAG, "will bind " + componentKey.componentName + " to workspace.");
                }
            }
        }

        LauncherAppState appState = LauncherAppState.getInstance(context);
        if (appState != null && !newItems.isEmpty()) {
            appState.getModel().addAndBindAddedWorkspaceItems(newItems, animated, mIgnoreLoaded);
        }
        return newItems;
    }
}