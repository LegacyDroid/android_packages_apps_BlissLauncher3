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
 * File:    bliss-compat/src/main/kotlin/foundation/e/bliss/compat/platform/DisplayIdCompat.kt
 * Module:  :bliss-compat  (foundation.e.bliss.compat.platform)
 * Role:    RELOCATED
 *
 * Tree (foundation/e/bliss/compat/platform/):
 *   ├── DisplayIdCompat.kt              — Context.getDisplayId() shim  ← THIS FILE
 *   ├── LineageSettingsCompat.kt        — LineageSettings.System.getInt() shim
 *   └── PrivateSpaceFlagsCompat.kt      — android.multiuser.Flags shim
 *
 * Purpose:
 *   Compatibility helper for `Context.getDisplayId()` which was added in
 *   API 36. On API 30-35, falls back to `Context.getDisplay().getDisplayId()`.
 *   Body unchanged from the M03 `com.android.launcher3.util.DisplayIdCompat`;
 *   only the package and the SDK literal (now `BuildCompat.isAtLeastBaklava`)
 *   change.
 *
 * Consumed by:
 *   - com.android.launcher3.InvariantDeviceProfile
 *   - com.android.launcher3.DeviceProfile
 *
 * Calls into:
 *   - foundation.e.bliss.compat.BuildCompat
 *
 * Plan reference: Plans/Migration04/01-compat-platform.md §3.4
 */
package foundation.e.bliss.compat.platform

import android.annotation.SuppressLint
import android.content.Context
import android.view.Display
import androidx.annotation.RequiresApi
import foundation.e.bliss.compat.BuildCompat

object DisplayIdCompat {

    @JvmStatic
    @SuppressLint("NewApi")
    fun getDisplayId(context: Context): Int {
        if (BuildCompat.isAtLeastBaklava) {
            return Api36Impl.getDisplayId(context)
        }
        return try {
            val display = context.display
            display?.displayId ?: Display.DEFAULT_DISPLAY
        } catch (e: UnsupportedOperationException) {
            // Non-visual contexts (e.g. Application) throw when getDisplay() is called
            Display.DEFAULT_DISPLAY
        }
    }

    @SuppressLint("NewApi")
    @RequiresApi(36)
    private object Api36Impl {
        @JvmStatic
        fun getDisplayId(context: Context): Int = context.displayId
    }
}
