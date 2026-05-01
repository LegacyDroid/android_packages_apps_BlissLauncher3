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
package com.android.launcher3.util;

import static com.android.quickstep.util.QuickstepProtoLogGroup.LAUNCHER_STATE_MANAGER;
import static com.android.quickstep.util.QuickstepProtoLogGroup.isProtoLogInitialized;

import androidx.annotation.NonNull;

import com.android.internal.protolog.ProtoLog;
import foundation.e.bliss.compat.desktop.DesktopFlagsCompat;

/**
 * Proxy class used for StateManager ProtoLog support.
 */
public class StateManagerProtoLogProxy {
    public static void logGoToState(
            @NonNull Object fromState, @NonNull Object toState, @NonNull String trace) {
        if (!DesktopFlagsCompat.enableStateManagerProtoLog() || !isProtoLogInitialized()) return;
        ProtoLog.d(LAUNCHER_STATE_MANAGER,
                "StateManager.goToState: fromState: %s, toState: %s, partial trace:\n%s",
                fromState,
                toState,
                trace);
    }

    public static void logCreateAtomicAnimation(
            @NonNull Object fromState, @NonNull Object toState, @NonNull String trace) {
        if (!DesktopFlagsCompat.enableStateManagerProtoLog() || !isProtoLogInitialized()) return;
        ProtoLog.d(LAUNCHER_STATE_MANAGER, "StateManager.createAtomicAnimation: "
                        + "fromState: %s, toState: %s, partial trace:\n%s",
                fromState,
                toState,
                trace);
    }

    public static void logOnStateTransitionStart(@NonNull Object state) {
        if (!DesktopFlagsCompat.enableStateManagerProtoLog() || !isProtoLogInitialized()) return;
        ProtoLog.d(LAUNCHER_STATE_MANAGER, "StateManager.onStateTransitionStart: state: %s", state);
    }

    public static void logOnStateTransitionEnd(@NonNull Object state) {
        if (!DesktopFlagsCompat.enableStateManagerProtoLog() || !isProtoLogInitialized()) return;
        ProtoLog.d(LAUNCHER_STATE_MANAGER, "StateManager.onStateTransitionEnd: state: %s", state);
    }

    public static void logCancelAnimation(boolean animationOngoing, @NonNull String trace) {
        if (!DesktopFlagsCompat.enableStateManagerProtoLog() || !isProtoLogInitialized()) return;
        ProtoLog.d(LAUNCHER_STATE_MANAGER,
                "StateManager.cancelAnimation: animation ongoing: %b, partial trace:\n%s",
                animationOngoing,
                trace);
    }
}
