/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.launcher3.workprofile;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.pageindicators.Direction;
import com.android.launcher3.pageindicators.PageIndicator;
import com.android.launcher3.views.ActivityContext;

import java.util.function.Consumer;

/**
 * Supports two indicator colors, dedicated for personal and work tabs.
 */
public class PersonalWorkSlidingTabStrip extends LinearLayout implements PageIndicator {
    private final boolean mIsAlignOnIcon;
    private OnActivePageChangedListener mOnActivePageChangedListener;
    private int mLastActivePage = 0;

    public PersonalWorkSlidingTabStrip(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs,
                R.styleable.PersonalWorkSlidingTabStrip);
        mIsAlignOnIcon = typedArray.getBoolean(
                R.styleable.PersonalWorkSlidingTabStrip_alignOnIcon, false);
        typedArray.recycle();
        // Migration02 / Phase 7.5 — apply user-selected work-tab background tint up-front so it
        // also shows when the work tab is hidden but the strip is still on screen.
        applyWorkTabBgPref();
    }

    /**
     * Migration02 / Phase 7.5 — read {@link LauncherPrefs#WORK_TAB_BG_COLOR} and tint the strip.
     * Accepts:
     * <ul>
     *   <li>{@code "default"} (or null/empty) — leave the AOSP background untouched.</li>
     *   <li>{@code "black"} / {@code "white"} — predefined opaque solid colors.</li>
     *   <li>Hex string starting with {@code #} — passed straight to {@link Color#parseColor}.</li>
     * </ul>
     * All errors are swallowed; the strip falls back to its theme background. Re-applied from
     * {@link #setActiveMarker} so a first work-tab show after a pref change picks up the new
     * value (XC-6 also wires a pref-change listener in {@code LauncherApplication} to nudge a
     * model rebind).
     */
    public void applyWorkTabBgPref() {
        try {
            String c = LauncherPrefs.get(getContext()).get(LauncherPrefs.WORK_TAB_BG_COLOR);
            if (c == null || c.isEmpty() || "default".equals(c)) return;
            int color;
            if (c.startsWith("#")) {
                color = Color.parseColor(c);
            } else {
                switch (c) {
                    case "black":
                        color = 0xFF000000;
                        break;
                    case "white":
                        color = 0xFFFFFFFF;
                        break;
                    default:
                        return;
                }
            }
            setBackgroundColor(color);
        } catch (Throwable ignored) { /* keep AOSP default */ }
    }

    /**
     * Highlights tab with index pos
     */
    public void updateTabTextColor(int pos) {
        for (int i = 0; i < getChildCount(); i++) {
            Button tab = (Button) getChildAt(i);
            tab.setSelected(i == pos);
        }
    }

    @Override
    public void setScroll(int currentScroll, int totalScroll) {
        // No-op: this tab strip does not react to scroll events.
    }

    @Override
    public void setActiveMarker(int activePage) {
        updateTabTextColor(activePage);
        // Migration02 / Phase 7.5 — re-apply the BG tint on every active-marker change so a pref
        // toggle picks up before the next folder/drawer reopen.
        applyWorkTabBgPref();
        if (mOnActivePageChangedListener != null && mLastActivePage != activePage) {
            mOnActivePageChangedListener.onActivePageChanged(activePage);
        }
        mLastActivePage = activePage;
    }

    public void setOnActivePageChangedListener(OnActivePageChangedListener listener) {
        mOnActivePageChangedListener = listener;
    }

    @Override
    public void setMarkersCount(int numMarkers) {
        // No-op: marker count is implied by the static personal/work tab structure.
    }

    @Override
    public void setArrowClickListener(Consumer<Direction> listener) {
        // No-Op. All Apps doesn't need accessibility arrows for single click navigation.
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mIsAlignOnIcon) {
            // If any padding is not specified, restrict the width to emulate padding
            int size = MeasureSpec.getSize(widthMeasureSpec);
            size = getTabWidth(getContext(), size);
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * Returns distance between left and right app icons
     */
    public static int getTabWidth(Context context, int totalWidth) {
        DeviceProfile grid = ActivityContext.lookupContext(context).getDeviceProfile();
        int iconPadding = totalWidth / grid.numShownAllAppsColumns - grid.allAppsIconSizePx;
        return totalWidth - iconPadding;
    }

    /**
     * Interface definition for a callback to be invoked when an active page has been changed.
     */
    public interface OnActivePageChangedListener {
        /** Called when the active page has been changed. */
        void onActivePageChanged(int currentActivePage);
    }
}
