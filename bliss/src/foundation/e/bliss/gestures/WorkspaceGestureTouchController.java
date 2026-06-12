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
/*
 * File:    bliss/src/foundation/e/bliss/gestures/WorkspaceGestureTouchController.java
 * Module:  bliss root app source-set
 * Role:    TouchController detecting double-tap and swipe-down gestures on home screen.
 */
package foundation.e.bliss.gestures;

import android.graphics.PointF;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.touch.BothAxesSwipeDetector;
import com.android.launcher3.util.TouchController;

/**
 * TouchController that detects double-tap and swipe-down gestures on the
 * workspace. Ported from Lawnchair's VerticalSwipeTouchController +
 * DirectionalGestureListener.
 *
 * Only active when on the home screen (NORMAL state) with no floating views
 * open.
 */
public class WorkspaceGestureTouchController implements TouchController, BothAxesSwipeDetector.Listener {

    private static final float SWIPE_VELOCITY_THRESHOLD = 2.25f;

    private final Launcher mLauncher;
    private final GestureController mGestureController;
    private final GestureDetector mDoubleTapDetector;
    private final BothAxesSwipeDetector mSwipeDetector;
    private final float mVelocityThreshold;

    private boolean mSwipeActive;

    public WorkspaceGestureTouchController(Launcher launcher, GestureController gestureController) {
        mLauncher = launcher;
        mGestureController = gestureController;

        mDoubleTapDetector = new GestureDetector(launcher, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isOnWorkspace()) {
                    mGestureController.onDoubleTap();
                    return true;
                }
                return false;
            }
        });
        mDoubleTapDetector.setIsLongpressEnabled(false);

        mSwipeDetector = new BothAxesSwipeDetector(launcher, this);

        float density = launcher.getResources().getDisplayMetrics().density;
        mVelocityThreshold = SWIPE_VELOCITY_THRESHOLD * density;
    }

    private boolean isOnWorkspace() {
        return mLauncher.isInState(LauncherState.NORMAL)
                && !AbstractFloatingView.hasOpenView(mLauncher, AbstractFloatingView.TYPE_ALL);
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        if (!isOnWorkspace()) {
            return false;
        }

        mDoubleTapDetector.onTouchEvent(ev);

        mSwipeDetector.setDetectableScrollConditions(BothAxesSwipeDetector.DIRECTION_DOWN, false);
        mSwipeDetector.onTouchEvent(ev);

        return mSwipeActive;
    }

    @Override
    public boolean onControllerTouchEvent(MotionEvent ev) {
        mSwipeDetector.onTouchEvent(ev);
        return mSwipeActive;
    }

    // ---- BothAxesSwipeDetector.Listener ----

    @Override
    public void onDragStart(boolean start) {
        if (isOnWorkspace()) {
            mSwipeActive = true;
        }
    }

    @Override
    public boolean onDrag(PointF displacement, MotionEvent motionEvent) {
        return true;
    }

    @Override
    public void onDragEnd(PointF velocity) {
        if (mSwipeActive && velocity.y > mVelocityThreshold) {
            mGestureController.onSwipeDown();
        }
        mSwipeActive = false;
    }
}
