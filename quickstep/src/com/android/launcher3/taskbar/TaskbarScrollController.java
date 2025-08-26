/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.launcher3.taskbar;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.R;
import com.android.launcher3.model.data.ItemInfo;

/**
 * Controller for managing taskbar icon scrolling functionality.
 * Handles creation, configuration, and management of scroll views when
 * taskbar icons exceed the available space in portrait 3-button mode.
 */
public class TaskbarScrollController {
    private static final int SCROLL_THRESHOLD = 7;
    private static final int MAX_VISIBLE_ICONS_PORTRAIT = 6;

    private final TaskbarActivityContext mActivityContext;
    private final TaskbarView mTaskbarView;
    private final int mIconTouchSize;
    private final int mItemMarginLeftRight;

    // Scrolling state
    private boolean mShouldEnableScrolling = false;
    private boolean mShouldRepositionNavButtons = false;

    // Scroll components
    @Nullable private HorizontalScrollView mIconScrollView;
    @Nullable private LinearLayout mIconContainer;

    // Icon count tracking
    private int mLastHotseatIconCount = 0;
    private int mLastRecentTaskCount = 0;

    public TaskbarScrollController(TaskbarActivityContext activityContext, TaskbarView taskbarView,
            int iconTouchSize, int itemMarginLeftRight) {
        mActivityContext = activityContext;
        mTaskbarView = taskbarView;
        mIconTouchSize = iconTouchSize;
        mItemMarginLeftRight = itemMarginLeftRight;
    }

    /**
     * Updates scrolling behavior based on current device state and icon count.
     */
    public void updateScrollingBehavior() {
        boolean shouldEnableScrolling = shouldEnableIconScrolling();
        
        if (shouldEnableScrolling && !mShouldEnableScrolling) {
            enableIconScrolling();
        } else if (!shouldEnableScrolling && mShouldEnableScrolling) {
            disableIconScrolling();
        }
        
        mShouldEnableScrolling = shouldEnableScrolling;
        mShouldRepositionNavButtons = shouldEnableScrolling; // Same condition for both
    }

    /**
     * Updates icon counts and refreshes scrolling behavior if needed.
     */
    public void updateIconCounts(int hotseatIconCount, int recentTaskCount) {
        mLastHotseatIconCount = hotseatIconCount;
        mLastRecentTaskCount = recentTaskCount;
        updateScrollingBehavior();
    }

    /**
     * Returns the total number of icons currently tracked.
     */
    public int getTotalIconCount() {
        return mLastHotseatIconCount + mLastRecentTaskCount;
    }

    /**
     * Determines if icon scrolling should be enabled based on current conditions.
     */
    private boolean shouldEnableIconScrolling() {
        Resources resources = mTaskbarView.getResources();
        Configuration config = resources.getConfiguration();
        boolean isPortrait = config.orientation == Configuration.ORIENTATION_PORTRAIT;
        boolean isThreeButtonNav = mActivityContext.isThreeButtonNav();
        boolean isTablet = mActivityContext.getDeviceProfile().isTablet;
        
        int totalIconCount = getTotalIconCount();
        
        android.util.Log.d("TaskbarScrollView", "shouldEnableIconScrolling: totalIcons=" + totalIconCount + 
                ", isPortrait=" + isPortrait + ", isThreeButtonNav=" + isThreeButtonNav + 
                ", isTablet=" + isTablet + ", threshold=" + SCROLL_THRESHOLD);
        
        return isPortrait && isThreeButtonNav && isTablet && totalIconCount > SCROLL_THRESHOLD;
    }

    /**
     * Enables icon scrolling by creating and configuring scroll views.
     */
    private void enableIconScrolling() {
        if (mIconScrollView != null) return;
        removeExistingIconViews();
        
        // Inflate scroll view from XML layout
        LayoutInflater inflater = LayoutInflater.from(mActivityContext);
        mIconScrollView = (HorizontalScrollView) inflater.inflate(R.layout.taskbar_scroll_view, null);
        mIconContainer = mIconScrollView.findViewById(R.id.taskbar_icon_container);
        
        // Override touch handling for the scroll view
        mIconScrollView = new HorizontalScrollView(mActivityContext) {
            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                return super.onInterceptTouchEvent(ev);
            }
            
            @Override
            public boolean onTouchEvent(MotionEvent ev) {
                return super.onTouchEvent(ev);
            }
        };
        
        // Re-inflate and get container after creating custom scroll view
        View scrollContent = inflater.inflate(R.layout.taskbar_scroll_view, mIconScrollView, false);
        mIconContainer = scrollContent.findViewById(R.id.taskbar_icon_container);
        mIconScrollView.addView(scrollContent);
        
        // Set minimum width for container
        int estimatedIconSize = 80;
        int density = (int) mTaskbarView.getResources().getDisplayMetrics().density;
        int minContainerWidth = 8 * estimatedIconSize * density;
        mIconContainer.setMinimumWidth(minContainerWidth);
        
        // Add scroll view to taskbar with constrained width
        int maxVisibleWidth = calculateScrollViewWidth();
        FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(
                maxVisibleWidth, FrameLayout.LayoutParams.MATCH_PARENT);
        scrollParams.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
        scrollParams.leftMargin = mTaskbarView.getResources().getDimensionPixelSize(R.dimen.taskbar_icon_spacing);
    }
    
    /**
     * Removes existing icon views when switching from non-scroll mode to scroll mode.
     */
    private void removeExistingIconViews() {
        for (int i = mTaskbarView.getChildCount() - 1; i >= 0; i--) {
            View child = mTaskbarView.getChildAt(i);
            if (child.getTag() instanceof ItemInfo || child instanceof com.android.launcher3.BubbleTextView) {
                mTaskbarView.removeViewAt(i);
            }
        }
    }
    
    /**
     * Disables icon scrolling and cleans up scroll views.
     */
    private void disableIconScrolling() {
        if (mIconScrollView != null && mIconScrollView.getParent() != null) {
            mTaskbarView.removeView(mIconScrollView);
        }
        if (mIconContainer != null) {
            mIconContainer.removeAllViews();
        }
        mIconScrollView = null;
        mIconContainer = null;
    }

    /**
     * Calculates the appropriate width for the scroll view based on screen size and icon count.
     */
    public int calculateScrollViewWidth() {
        DeviceProfile deviceProfile = mActivityContext.getDeviceProfile();
        int screenWidth = deviceProfile.widthPx;
        int navButtonSpace = deviceProfile.hotseatBarEndOffset;
        int safetyMargin = mTaskbarView.getResources().getDimensionPixelSize(R.dimen.taskbar_icon_spacing) * 2;
        int reservedSpace = navButtonSpace + safetyMargin;
        
        int availableWidth = screenWidth - reservedSpace;
        
        int iconSize = 2 * mItemMarginLeftRight + mIconTouchSize;
        int totalIcons = getTotalIconCount();
        int visibleIcons = Math.min(totalIcons, MAX_VISIBLE_ICONS_PORTRAIT);
        
        int idealWidth = visibleIcons * iconSize;
        
        int containerPadding = mTaskbarView.getResources().getDimensionPixelSize(R.dimen.taskbar_icon_spacing);
        idealWidth += (2 * containerPadding);
        
        int finalWidth = Math.min(idealWidth, availableWidth);
        return finalWidth;
    }

    /**
     * Updates the container width based on current icon count and layout requirements.
     */
    public void updateContainerWidth() {
        if (mIconContainer == null) return;
        
        int totalIconCount = getTotalIconCount();
        if (totalIconCount == 0) {
            mIconContainer.setMinimumWidth(0);
            return;
        }
        
        int iconSize = 2 * mItemMarginLeftRight + mIconTouchSize;
        int actualContainerWidth = totalIconCount * iconSize;
        int containerPadding = mTaskbarView.getResources().getDimensionPixelSize(R.dimen.taskbar_icon_spacing);
        actualContainerWidth += (2 * containerPadding);
        
        mIconContainer.setMinimumWidth(actualContainerWidth);
        mIconContainer.requestLayout();
    }

    /**
     * Prepares scroll view for adding icons by ensuring it's properly attached to the parent.
     */
    public void prepareScrollViewForIcons() {
        if (mShouldEnableScrolling && mIconContainer != null && mIconScrollView != null && mIconScrollView.getParent() == null) {
            int maxVisibleWidth = calculateScrollViewWidth();
            if (maxVisibleWidth < 200) {
                maxVisibleWidth = 600;
            }
            
            FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(
                    maxVisibleWidth, FrameLayout.LayoutParams.MATCH_PARENT);
            scrollParams.gravity = Gravity.TOP | Gravity.START; // Remove CENTER_VERTICAL
            scrollParams.leftMargin = 0;
            scrollParams.topMargin = 0;
            scrollParams.rightMargin = 0;
            
            if (mIconScrollView != null) {
                mIconScrollView.setMinimumWidth(maxVisibleWidth);
                mIconScrollView.requestLayout();
                mTaskbarView.addView(mIconScrollView, scrollParams);
            }
        }
    }

    /**
     * Clears all icons from the scroll container.
     */
    public void clearScrollContainer() {
        if (mShouldEnableScrolling && mIconContainer != null) {
            mIconContainer.removeAllViews();
        }
    }

    /**
     * Adds a view to the appropriate container (scroll container if scrolling enabled, main view otherwise).
     */
    public boolean addIconToContainer(View iconView, int itemPadding) {
        if (mShouldEnableScrolling && mIconContainer != null) {
            iconView.setPadding(itemPadding, itemPadding, itemPadding, itemPadding);
            LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                    mIconTouchSize, mIconTouchSize);
            scrollLp.setMarginStart(mItemMarginLeftRight);
            scrollLp.setMarginEnd(mItemMarginLeftRight);
            mIconContainer.addView(iconView, scrollLp);
            return true; // Indicates icon was added to scroll container
        }
        return false; // Indicates icon should be added to main container
    }

    /**
     * Handles layout positioning for the scroll view.
     */
    public void layoutScrollView() {
        if (mShouldEnableScrolling && mIconScrollView != null && mIconScrollView.getParent() == mTaskbarView) {
            int scrollWidth = calculateScrollViewWidth();
            mIconScrollView.layout(0, mIconScrollView.getTop(), scrollWidth, mIconScrollView.getBottom());
        }
    }

    // Getters for current state
    public boolean shouldEnableScrolling() {
        return mShouldEnableScrolling;
    }

    public boolean shouldRepositionNavButtons() {
        return mShouldRepositionNavButtons;
    }

    @Nullable
    public HorizontalScrollView getIconScrollView() {
        return mIconScrollView;
    }

    @Nullable
    public LinearLayout getIconContainer() {
        return mIconContainer;
    }
}