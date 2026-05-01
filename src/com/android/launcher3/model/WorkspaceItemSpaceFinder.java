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
/*
 * Bliss touchpoint(s) (Migration04):
 *   - The preserve-gaps placement rule (place new items AFTER the last
 *     occupied cell in row-major order, instead of first-fit) is now
 *     gated on LauncherPolicy.idleApp(ctx).shouldAutoFillIdle() instead
 *     of a raw LauncherPrefs read. False → honour gaps (skip-to-end
 *     branch); true → AOSP first-fit. This is the actual call site that
 *     enforces the gap-preservation guarantee referenced from
 *     VerifyIdleAppTask's touchpoint block.
 *     — Plan ref: Plans/Migration04/05-launcher-policy-strategies.md §5.4
 *
 * The body of this file otherwise tracks AOSP. Keep diffs minimal so a
 * future origin/a16 rebase merges cleanly.
 */
package com.android.launcher3.model;

import static com.android.launcher3.Utilities.SHOULD_SHOW_FIRST_PAGE_WIDGET;
import static com.android.launcher3.WorkspaceLayoutManager.FIRST_SCREEN_ID;

import android.util.LongSparseArray;

import android.content.Context;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.dagger.ApplicationContext;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.util.GridOccupancy;
import com.android.launcher3.util.IntArray;
import com.android.launcher3.util.IntSet;

import java.util.ArrayList;

import javax.inject.Inject;

import foundation.e.bliss.policy.LauncherPolicy;

/**
 * Utility class to help find space for new workspace items
 */
public class WorkspaceItemSpaceFinder {

    private BgDataModel mDataModel;
    private InvariantDeviceProfile mIDP;
    private LauncherModel mModel;
    private final Context mContext;

    @Inject
    WorkspaceItemSpaceFinder(
            @ApplicationContext Context context,
            BgDataModel dataModel, InvariantDeviceProfile idp, LauncherModel model) {
        mContext = context;
        mDataModel = dataModel;
        mIDP = idp;
        mModel = model;
    }

    /**
     * Find a position on the screen for the given size or adds a new screen.
     *
     * @return screenId and the coordinates for the item in an int array of size 3.
     */
    public int[] findSpaceForItem(
            IntArray workspaceScreens, IntArray addedWorkspaceScreensFinal, int spanX, int spanY) {
        LongSparseArray<ArrayList<ItemInfo>> screenItems = new LongSparseArray<>();

        // Use sBgItemsIdMap as all the items are already loaded.
        synchronized (mDataModel.mLock) {
            for (ItemInfo info : mDataModel.itemsIdMap) {
                if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                    ArrayList<ItemInfo> items = screenItems.get(info.screenId);
                    if (items == null) {
                        items = new ArrayList<>();
                        screenItems.put(info.screenId, items);
                    }
                    items.add(info);
                }
            }
        }

        // Find appropriate space for the item.
        int screenId = 0;
        int[] coordinates = new int[2];
        boolean found = false;

        int screenCount = workspaceScreens.size();
        // First check the preferred screen.
        IntSet screensToExclude = new IntSet();
        if (FeatureFlags.QSB_ON_FIRST_SCREEN.get()
                && !SHOULD_SHOW_FIRST_PAGE_WIDGET) {
            screensToExclude.add(FIRST_SCREEN_ID);
        }

        for (int screen = 0; screen < screenCount; screen++) {
            screenId = workspaceScreens.get(screen);
            if (!screensToExclude.contains(screenId) && findNextAvailableIconSpaceInScreen(
                    screenItems.get(screenId), coordinates, spanX, spanY)) {
                // We found a space for it
                found = true;
                break;
            }
        }

        if (!found) {
            // Still no position found. Add a new screen to the end.
            screenId = mModel.getModelDbController().getNewScreenId();

            // Save the screen id for binding in the workspace
            workspaceScreens.add(screenId);
            addedWorkspaceScreensFinal.add(screenId);

            // If we still can't find an empty space, then God help us all!!!
            if (!findNextAvailableIconSpaceInScreen(
                    screenItems.get(screenId), coordinates, spanX, spanY)) {
                throw new RuntimeException("Can't find space to add the item");
            }
        }
        return new int[]{screenId, coordinates[0], coordinates[1]};
    }

    private boolean findNextAvailableIconSpaceInScreen(
            ArrayList<ItemInfo> occupiedPos, int[] xy, int spanX, int spanY) {
        GridOccupancy occupied = new GridOccupancy(mIDP.numColumnsFixed, mIDP.numRowsFixed);
        if (occupiedPos != null) {
            for (ItemInfo r : occupiedPos) {
                occupied.markCells(r, true);
            }
        }
        // Honour-gaps branch: place the new item AFTER the last occupied cell
        // (row-major) instead of first-fit. This is what differentiates "auto-add
        // new apps to home screen" from "auto-pack and obliterate user gaps" —
        // with this branch the user can leave intentional empty cells in the
        // middle of the workspace and new app installs will land at the end
        // (or roll onto a fresh page) instead of filling those gaps. Gated by
        // the Bliss-side IdleAppPolicy strategy (HonorGaps → false → take this
        // branch; AutoFill → true → fall through to AOSP first-fit).
        try {
            if (!LauncherPolicy.idleApp(mContext).shouldAutoFillIdle()
                    && occupiedPos != null && !occupiedPos.isEmpty()) {
                int lastY = 0, lastX = -1;
                for (ItemInfo r : occupiedPos) {
                    int endY = r.cellY + r.spanY - 1;
                    int endX = r.cellX + r.spanX - 1;
                    if (endY > lastY || (endY == lastY && endX > lastX)) {
                        lastY = endY;
                        lastX = endX;
                    }
                }
                // Scan starting from the cell AFTER (lastX, lastY) in row-major order.
                int startX = lastX + 1;
                int startY = lastY;
                if (startX >= mIDP.numColumnsFixed) {
                    startX = 0;
                    startY++;
                }
                for (int y = startY; y < mIDP.numRowsFixed; y++) {
                    for (int x = (y == startY ? startX : 0); x < mIDP.numColumnsFixed; x++) {
                        if (occupied.isRegionVacant(x, y, spanX, spanY)) {
                            xy[0] = x;
                            xy[1] = y;
                            return true;
                        }
                    }
                }
                return false; // no space at end → caller adds a new screen
            }
        } catch (Throwable ignored) {
            // Reflection / pref read failed — fall through to default first-fit.
        }
        return occupied.findVacantCell(xy, spanX, spanY);
    }
}
