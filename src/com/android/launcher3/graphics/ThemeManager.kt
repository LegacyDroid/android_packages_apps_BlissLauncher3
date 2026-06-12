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

package com.android.launcher3.graphics

import android.content.Context
import android.content.res.Resources
import androidx.core.graphics.PathParser
import com.android.launcher3.EncryptionType
import foundation.e.bliss.icons.CustomAdaptiveIconDrawable
import com.android.launcher3.Item
import com.android.launcher3.LauncherPrefChangeListener
import com.android.launcher3.LauncherPrefs
import com.android.launcher3.LauncherPrefs.Companion.backedUpItem
import com.android.launcher3.dagger.ApplicationContext
import com.android.launcher3.dagger.LauncherAppComponent
import com.android.launcher3.dagger.LauncherAppSingleton
import com.android.launcher3.graphics.ShapeDelegate.Companion.pickBestShape
import com.android.launcher3.icons.IconThemeController
import com.android.launcher3.icons.mono.MonoIconThemeController
import com.android.launcher3.shapes.ShapesProvider
import com.android.launcher3.util.DaggerSingletonObject
import com.android.launcher3.util.DaggerSingletonTracker
import com.android.launcher3.util.Executors.MAIN_EXECUTOR
import com.android.launcher3.util.SimpleBroadcastReceiver
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject

/** Centralized class for managing Launcher icon theming */
@LauncherAppSingleton
class ThemeManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val prefs: LauncherPrefs,
    private val iconControllerFactory: IconControllerFactory,
    lifecycle: DaggerSingletonTracker,
) {

    /** Representation of the current icon state */
    var iconState = parseIconState(null)
        private set

    var isMonoThemeEnabled
        set(value) = prefs.put(THEMED_ICONS, value)
        get() = prefs.get(THEMED_ICONS)

    val themeController
        get() = iconState.themeController

    val isIconThemeEnabled
        get() = themeController != null

    val iconShape
        get() = iconState.iconShape

    val folderShape
        get() = iconState.folderShape

    private val listeners = CopyOnWriteArrayList<ThemeChangeListener>()

    init {
        val receiver = SimpleBroadcastReceiver(context, MAIN_EXECUTOR) { verifyIconState() }
        receiver.registerPkgActions("android", ACTION_OVERLAY_CHANGED)

        val keys = (iconControllerFactory.prefKeys + PREF_ICON_SHAPE)

        val keysArray = keys.toTypedArray()
        val prefKeySet = keys.map { it.sharedPrefKey }
        val prefListener = LauncherPrefChangeListener { key ->
            if (prefKeySet.contains(key)) verifyIconState()
        }
        prefs.addListener(prefListener, *keysArray)
        lifecycle.addCloseable {
            receiver.unregisterReceiverSafely()
            prefs.removeListener(prefListener, *keysArray)
        }
    }

    private fun verifyIconState() {
        val newState = parseIconState(iconState)
        if (newState == iconState) return
        iconState = newState

        listeners.forEach { it.onThemeChanged() }
    }

    fun addChangeListener(listener: ThemeChangeListener) = listeners.add(listener)

    fun removeChangeListener(listener: ThemeChangeListener) = listeners.remove(listener)

    /**
     * Build a 100x100 rounded-rect SVG path for a given corner radius percent
     * (0..50). 0 = square, 50 = circle. Called by [parseIconState] when the
     * user selects the "custom" icon shape.
     */
    private fun buildRoundRectPath(radiusPercent: Int): String {
        val r = radiusPercent.toFloat()
        // Path drawn clockwise from top-left after the corner, using arcs for corners.
        return "M$r 0 H${100f - r} A$r $r 0 0 1 100 $r V${100f - r}" +
                " A$r $r 0 0 1 ${100f - r} 100 H$r" +
                " A$r $r 0 0 1 0 ${100f - r} V$r" +
                " A$r $r 0 0 1 $r 0 Z"
    }

    private fun parseIconState(oldState: IconState?): IconState {
        val shapeOverrideKey = prefs.get(PREF_ICON_SHAPE)
        val shapeModel =
            ShapesProvider.iconShapes.firstOrNull { it.key == shapeOverrideKey }
        val customRoundRectPath: String? =
            if (shapeOverrideKey == "custom") {
                try {
                    val radius = prefs.get(LauncherPrefs.CUSTOM_ICON_SHAPE_RADIUS)
                        .coerceIn(0, 50)
                    buildRoundRectPath(radius)
                } catch (_: Throwable) {
                    null
                }
            } else null
        val iconMask =
            when {
                customRoundRectPath != null -> customRoundRectPath
                shapeModel != null -> shapeModel.pathString
                CONFIG_ICON_MASK_RES_ID == Resources.ID_NULL -> ""
                else -> context.resources.getString(CONFIG_ICON_MASK_RES_ID)
            }

        val iconShape =
            if (oldState != null && oldState.iconMask == iconMask) oldState.iconShape
            else pickBestShape(iconMask)

        // Publish the active mask to CustomAdaptiveIconDrawable so wrapped
        // icons render with the user-selected shape.
        try {
            if (iconMask.isNotEmpty()) {
                CustomAdaptiveIconDrawable.sMask = PathParser.createPathFromPathData(iconMask)
                CustomAdaptiveIconDrawable.sMaskId = iconMask.hashCode().toString()
                CustomAdaptiveIconDrawable.sInitialized = true
            }
        } catch (_: Throwable) {
            // Fall back to the built-in circle if the mask string is unparseable.
        }

        val folderShapeMask = shapeModel?.folderPathString ?: iconMask
        val folderShape =
            when {
                oldState != null && oldState.folderShapeMask == folderShapeMask ->
                    oldState.folderShape
                folderShapeMask == iconMask || folderShapeMask.isEmpty() -> iconShape
                else -> pickBestShape(folderShapeMask)
            }

        return IconState(
            iconMask = iconMask,
            folderShapeMask = folderShapeMask,
            themeController = iconControllerFactory.createThemeController(),
            iconScale = shapeModel?.iconScale ?: 1f,
            iconShape = iconShape,
            folderShape = folderShape,
        )
    }

    data class IconState(
        val iconMask: String,
        val folderShapeMask: String,
        val themeController: IconThemeController?,
        val themeCode: String = themeController?.themeID ?: "no-theme",
        val iconScale: Float = 1f,
        val iconShape: ShapeDelegate,
        val folderShape: ShapeDelegate,
    ) {
        fun toUniqueId() = "${iconMask.hashCode()},$themeCode"
    }

    /** Interface for receiving theme change events */
    fun interface ThemeChangeListener {
        fun onThemeChanged()
    }

    open class IconControllerFactory @Inject constructor(protected val prefs: LauncherPrefs) {

        // Migration02 Phase 3.1: gate the themeController on (THEMED_ICONS || DRAWER_THEMED_ICONS)
        // so the mono `BitmapInfo.themedBitmap` is generated whenever any flavour of theming is
        // on. The drawer-only split (BubbleTextView::shouldUseThemedDrawable) relies on the mono
        // bitmap being available even when the global toggle is off.
        open val prefKeys: List<Item> = listOf(THEMED_ICONS, LauncherPrefs.DRAWER_THEMED_ICONS)

        open fun createThemeController(): IconThemeController? {
            val anyThemed = prefs.get(THEMED_ICONS) || prefs.get(LauncherPrefs.DRAWER_THEMED_ICONS)
            return if (anyThemed) MONO_THEME_CONTROLLER else null
        }
    }

    companion object {

        @JvmField val INSTANCE = DaggerSingletonObject(LauncherAppComponent::getThemeManager)
        const val KEY_ICON_SHAPE = "icon_shape_model"

        const val KEY_THEMED_ICONS = "themed_icons"
        @JvmField val THEMED_ICONS = backedUpItem(KEY_THEMED_ICONS, false, EncryptionType.ENCRYPTED)
        @JvmField val PREF_ICON_SHAPE = backedUpItem(KEY_ICON_SHAPE, "", EncryptionType.ENCRYPTED)

        private const val ACTION_OVERLAY_CHANGED = "android.intent.action.OVERLAY_CHANGED"
        private val CONFIG_ICON_MASK_RES_ID: Int =
            Resources.getSystem().getIdentifier("config_icon_mask", "string", "android")

        // Use a constant to allow equality check in verifyIconState
        private val MONO_THEME_CONTROLLER = MonoIconThemeController()
    }
}
