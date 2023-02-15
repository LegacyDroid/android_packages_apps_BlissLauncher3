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
 *
 */
package foundation.e.bliss.utils

import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.util.Log
import com.android.launcher3.InvariantDeviceProfile
import com.android.launcher3.LauncherSettings.Favorites.E_TABLE_NAME
import com.android.launcher3.LauncherSettings.Favorites.E_TABLE_NAME_ALL
import com.android.launcher3.LauncherSettings.Favorites.INTENT
import com.android.launcher3.LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT
import com.android.launcher3.LauncherSettings.Favorites.PROFILE_ID
import com.android.launcher3.model.DatabaseHelper
import com.android.launcher3.pm.UserCache
import com.android.launcher3.shortcuts.ShortcutKey

object BlissDbUtils {
    private const val TAG = "BlissDbUtils"

    @JvmStatic
    fun queryDeepShortcutsFromDb(context: Context): List<ShortcutKey> {
        val shortcutKeys = mutableListOf<ShortcutKey>()
        val dbName = InvariantDeviceProfile.INSTANCE[context].dbFile
        val dbHelper =
            DatabaseHelper(
                context,
                dbName,
                UserCache.INSTANCE.get(context)::getSerialNumberForUser
            ) {}

        val userManager = context.getSystemService(UserManager::class.java)

        try {
            dbHelper.writableDatabase.use { database ->
                database
                    .rawQuery(
                        "SELECT $INTENT, $PROFILE_ID FROM $E_TABLE_NAME_ALL WHERE itemType=$ITEM_TYPE_DEEP_SHORTCUT UNION " +
                            "SELECT $INTENT, $PROFILE_ID FROM $E_TABLE_NAME WHERE itemType=$ITEM_TYPE_DEEP_SHORTCUT",
                        null
                    )
                    .use { cursor ->
                        while (cursor.moveToNext()) {
                            val user = userManager.getUserForSerialNumber(cursor.getInt(1).toLong())
                            val intent = Intent.parseUri(cursor.getString(0), 0)
                            shortcutKeys.add(ShortcutKey.fromIntent(intent, user))
                        }
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "queryDeepShortcutsFromeDb: ", e)
        }

        return shortcutKeys
    }
}
