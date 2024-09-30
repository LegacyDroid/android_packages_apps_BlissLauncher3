/*
 * Copyright (C) 2024 MURENA SAS
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
package foundation.e.bliss

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.os.Build.VERSION
import android.os.UserHandle
import android.util.Log
import androidx.annotation.Keep
import com.android.launcher3.Flags.useNewIconForArchivedApps
import com.android.launcher3.R
import com.android.launcher3.icons.BaseIconFactory.IconOptions
import com.android.launcher3.icons.BitmapInfo
import com.android.launcher3.icons.cache.BaseIconCache
import com.android.launcher3.icons.cache.CachingLogic
import com.android.launcher3.icons.cache.LauncherActivityCachingLogic.TAG
import foundation.e.bliss.utils.resourcesToMap

@Keep
@Suppress("Unused")
class LauncherActivityCachingLogic(context: Context) : CachingLogic<LauncherActivityInfo> {
    private val aliasedApps by lazy {
        val list = context.resources.getStringArray(R.array.aliased_apps).toList()
        resourcesToMap(list)
    }

    override fun getLabel(info: LauncherActivityInfo): CharSequence {
        val customLabel = aliasedApps[info.componentName.packageName]
        return if (!customLabel.isNullOrEmpty()) {
            customLabel
        } else info.label
    }

    override fun getComponent(info: LauncherActivityInfo): ComponentName = info.componentName

    override fun getUser(info: LauncherActivityInfo): UserHandle = info.user

    override fun loadIcon(
        context: Context,
        cache: BaseIconCache,
        info: LauncherActivityInfo,
    ): BitmapInfo {
        cache.iconFactory.use { li ->
            val iconOptions: IconOptions = IconOptions().setUser(info.user)
            iconOptions.setIsArchived(
                useNewIconForArchivedApps() && VERSION.SDK_INT >= 35 && info.activityInfo.isArchived
            )
            val iconDrawable = cache.iconProvider.getIcon(info, li.fullResIconDpi)
            if (
                VERSION.SDK_INT >= 30 &&
                context.packageManager.isDefaultApplicationIcon(iconDrawable)
            ) {
                Log.w(
                    TAG,
                    "loadIcon: Default app icon returned from PackageManager." +
                            " component=${info.componentName}, user=${info.user}",
                    Exception(),
                )
                // Make sure this default icon always matches BaseIconCache#getDefaultIcon
                return cache.getDefaultIcon(info.user)
            }
            return li.createBadgedIconBitmap(iconDrawable, iconOptions)
        }
    }
}
