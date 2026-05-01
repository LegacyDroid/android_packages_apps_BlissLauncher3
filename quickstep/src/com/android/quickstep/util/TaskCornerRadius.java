/*
 * Copyright (C) 2019 The Android Open Source Project
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
 *   - Imports foundation.e.bliss.compat.quickstep.QuickStepContractCompat (relocated by Migration04)
 *     — Plan ref: Plans/Migration04/01-compat-platform.md §4
 *
 * The body of this file otherwise tracks AOSP. Keep diffs minimal so a
 * future origin/a16 rebase merges cleanly.
 */
package com.android.quickstep.util;

import static foundation.e.bliss.compat.quickstep.QuickStepContractCompat.supportsRoundedCornersOnWindows;

import android.content.Context;
import android.content.res.Resources;

import com.android.launcher3.R;
import com.android.launcher3.util.Themes;

public class TaskCornerRadius {

    public static float get(Context context) {
        Resources resources = context.getResources();
        if (!supportsRoundedCornersOnWindows(resources)) {
            return resources.getDimension(R.dimen.task_corner_radius_small);
        }

        float overriddenRadius =
                resources.getDimension(R.dimen.task_corner_radius_override);
        return (overriddenRadius > 0) ? overriddenRadius : Themes.getDialogCornerRadius(context);
    }
}
