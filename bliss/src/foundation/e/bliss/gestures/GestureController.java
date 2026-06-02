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
 * File:    bliss/src/foundation/e/bliss/gestures/GestureController.java
 * Module:  bliss root app source-set
 * Role:    Manager for gesture-to-handler mapping with double-tap and swipe-down workspace gestures.
 */
package foundation.e.bliss.gestures;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherPrefs;

/**
 * Manages gesture-to-handler mapping. Reads handler names from LauncherPrefs on
 * each trigger so changes take effect immediately without a listener.
 *
 * Two gesture slots: - Double-tap on workspace (default: sleep) - Swipe-down on
 * workspace (default: notifications)
 */
public class GestureController {

    private final Launcher mLauncher;

    public GestureController(Launcher launcher) {
        mLauncher = launcher;
    }

    public void onDoubleTap() {
        String name = LauncherPrefs.get(mLauncher).get(LauncherPrefs.GESTURE_DOUBLE_TAP);
        GestureHandler.forName(name, mLauncher).onTrigger();
    }

    public void onSwipeDown() {
        String name = LauncherPrefs.get(mLauncher).get(LauncherPrefs.GESTURE_SWIPE_DOWN);
        GestureHandler.forName(name, mLauncher).onTrigger();
    }
}
