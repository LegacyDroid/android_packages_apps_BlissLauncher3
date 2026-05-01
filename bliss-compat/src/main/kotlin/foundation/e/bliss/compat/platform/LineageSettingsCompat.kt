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
 * File:    bliss-compat/src/main/kotlin/foundation/e/bliss/compat/platform/LineageSettingsCompat.kt
 * Module:  :bliss-compat  (foundation.e.bliss.compat.platform)
 * Role:    RELOCATED
 *
 * Tree (foundation/e/bliss/compat/platform/):
 *   ├── DisplayIdCompat.kt              — Context.getDisplayId() shim
 *   ├── LineageSettingsCompat.kt        — LineageSettings.System.getInt() shim  ← THIS FILE
 *   └── PrivateSpaceFlagsCompat.kt      — android.multiuser.Flags shim
 *
 * Purpose:
 *   Compatibility wrapper for `LineageSettings.System.getInt`. The bundled
 *   `LineageSettings` (from `libs/classes.jar`, compiled against API 36)
 *   calls `ContentResolver.getAttributionSource()` and `getUserId()` which
 *   only exist on API 36+. On API < 36, we query the lineage settings
 *   content provider directly. Body unchanged from the M03 file; only the
 *   package and the SDK literal change.
 *
 * Consumed by:
 *   - com.android.launcher3.InvariantDeviceProfile
 *   - com.android.launcher3.DeviceProfile
 *
 * Calls into:
 *   - foundation.e.bliss.compat.BuildCompat
 *
 * Plan reference: Plans/Migration04/01-compat-platform.md §4
 */
package foundation.e.bliss.compat.platform

import android.content.ContentResolver
import android.net.Uri
import androidx.annotation.RequiresApi
import foundation.e.bliss.compat.BuildCompat
import lineageos.providers.LineageSettings

object LineageSettingsCompat {

    private val LINEAGE_SYSTEM_URI: Uri =
        Uri.parse("content://lineagesettings/system")

    @JvmStatic
    fun getSystemInt(resolver: ContentResolver, name: String, def: Int): Int {
        if (BuildCompat.isAtLeastBaklava) {
            return Api36Impl.getSystemInt(resolver, name, def)
        }
        // Query the lineage settings content provider directly to avoid
        // the bundled LineageSettings calling API 36-only ContentResolver methods.
        try {
            val uri = Uri.withAppendedPath(LINEAGE_SYSTEM_URI, name)
            resolver.query(uri, arrayOf("value"), null, null, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val value = cursor.getString(0)
                    if (value != null) {
                        return value.toInt()
                    }
                }
            }
        } catch (e: Exception) {
            // Content provider not available or parse error
        }
        return def
    }

    @RequiresApi(36)
    private object Api36Impl {
        @JvmStatic
        fun getSystemInt(resolver: ContentResolver, name: String, def: Int): Int =
            LineageSettings.System.getInt(resolver, name, def)
    }
}
