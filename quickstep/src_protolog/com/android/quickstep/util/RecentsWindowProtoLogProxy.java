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

/*
 * Bliss touchpoint(s) (Migration04):
 *   - Imports foundation.e.bliss.compat.desktop.DesktopFlagsCompat (relocated by Migration04)
 *     — Plan ref: Plans/Migration04/01-compat-platform.md §4
 *
 * The body of this file otherwise tracks AOSP. Keep diffs minimal so a
 * future origin/a16 rebase merges cleanly.
 */
package com.android.quickstep.util;

import static com.android.quickstep.util.QuickstepProtoLogGroup.RECENTS_WINDOW;
import static com.android.quickstep.util.QuickstepProtoLogGroup.isProtoLogInitialized;

import androidx.annotation.NonNull;

import com.android.internal.protolog.ProtoLog;
import com.android.internal.protolog.common.IProtoLogGroup;
import foundation.e.bliss.compat.desktop.DesktopFlagsCompat;

/**
 * Proxy class used for Recents Window ProtoLog support.
 * <p>
 * This file will have all of its static strings in the
 * {@link ProtoLog#d(IProtoLogGroup, String, Object...)} calls replaced by dynamic code/strings.
 * <p>
 * When a new Recents Window log needs to be added to the codebase, add it here under a new unique
 * method. Or, if an existing entry needs to be modified, simply update it here.
 */
public class RecentsWindowProtoLogProxy {
    public static void logOnStateSetStart(@NonNull String stateName) {
        if (!DesktopFlagsCompat.enableRecentsWindowProtoLog() || !isProtoLogInitialized()) return;
        ProtoLog.d(RECENTS_WINDOW, "onStateSetStart: %s", stateName);
    }

    public static void logOnStateSetEnd(@NonNull String stateName) {
        if (!DesktopFlagsCompat.enableRecentsWindowProtoLog() || !isProtoLogInitialized()) return;
        ProtoLog.d(RECENTS_WINDOW, "onStateSetEnd: %s", stateName);
    }

    public static void logStartRecentsWindow(boolean isShown, boolean windowViewIsNull) {
        if (!DesktopFlagsCompat.enableRecentsWindowProtoLog() || !isProtoLogInitialized()) return;
        ProtoLog.d(RECENTS_WINDOW,
                "Starting recents window: isShow= %b, windowViewIsNull=%b",
                isShown,
                windowViewIsNull);
    }

    public static void logCleanup(boolean isShown) {
        if (!DesktopFlagsCompat.enableRecentsWindowProtoLog() || !isProtoLogInitialized()) return;
        ProtoLog.d(RECENTS_WINDOW, "Cleaning up recents window: isShow= %b", isShown);
    }
}
