/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
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
