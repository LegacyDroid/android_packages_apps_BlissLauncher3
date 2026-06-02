/*
 * Copyright (C) 2023 The Android Open Source Project
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
 *   - Bliss-owned `backedUpItem(...)` declarations now reference
 *     foundation.e.bliss.preferences.BlissPrefs.PREF_* constants instead
 *     of inlining the raw "pref_*" string. This removes the last
 *     duplicate-key sites for PRESERVE_LAYOUT_GAPS, GRID_FOLDER_PREVIEW,
 *     FIRST_RUN_LAYOUT_CHOICE_DONE, and the four migration / toggle
 *     prefs (workspace lock, two-line toggle, hotseat-factor V1 marker,
 *     closing-overlay V1 marker) that previously skipped the registry.
 *     — Plan ref: Plans/Migration04/03-prefs-registry.md §3.2
 *   - Adds FIRST_RUN_STEPS_DONE (Set<String>): the per-step completion
 *     ledger consumed by FirstRunStateStore. Generalises the single-bit
 *     FIRST_RUN_LAYOUT_CHOICE_DONE gate so the wizard can host more than
 *     one step without growing more bool prefs.
 *     — Plan ref: Plans/Migration04/07-first-run-wizard.md §3.3
 *
 * The body of this file otherwise tracks AOSP. Keep diffs minimal so a
 * future origin/a16 rebase merges cleanly.
 */
package com.android.launcher3

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import com.android.launcher3.BuildConfig.WIDGET_ON_FIRST_SCREEN
import com.android.launcher3.GridType.Companion.GRID_TYPE_ANY
import com.android.launcher3.InvariantDeviceProfile.GRID_NAME_PREFS_KEY
import com.android.launcher3.InvariantDeviceProfile.NON_FIXED_LANDSCAPE_GRID_NAME_PREFS_KEY
import com.android.launcher3.LauncherFiles.DEVICE_PREFERENCES_KEY
import com.android.launcher3.LauncherFiles.SHARED_PREFERENCES_KEY
import com.android.launcher3.dagger.ApplicationContext
import com.android.launcher3.dagger.LauncherAppComponent
import com.android.launcher3.dagger.LauncherAppSingleton
import com.android.launcher3.model.DeviceGridState
import com.android.launcher3.pm.InstallSessionHelper
import com.android.launcher3.provider.RestoreDbTask
import com.android.launcher3.provider.RestoreDbTask.FIRST_LOAD_AFTER_RESTORE_KEY
import com.android.launcher3.settings.SettingsActivity
import com.android.launcher3.states.RotationHelper
import com.android.launcher3.util.DaggerSingletonObject
import com.android.launcher3.util.DisplayController
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import foundation.e.bliss.preferences.BlissPrefs

/**
 * Manages Launcher [SharedPreferences] through [Item] instances.
 *
 * TODO(b/262721340): Replace all direct SharedPreference refs with LauncherPrefs / Item methods.
 */
@LauncherAppSingleton
open class LauncherPrefs
@Inject
constructor(@ApplicationContext private val encryptedContext: Context) {

    private val deviceProtectedSharedPrefs: SharedPreferences by lazy {
        encryptedContext
            .createDeviceProtectedStorageContext()
            .getSharedPreferences(BOOT_AWARE_PREFS_KEY, MODE_PRIVATE)
    }

    open protected fun getSharedPrefs(item: Item): SharedPreferences =
        item.run {
            if (encryptionType == EncryptionType.DEVICE_PROTECTED) deviceProtectedSharedPrefs
            else encryptedContext.getSharedPreferences(sharedPrefFile, MODE_PRIVATE)
        }

    /** Returns the value with type [T] for [item]. */
    fun <T> get(item: ContextualItem<T>): T =
        getInner(item, item.defaultValueFromContext(encryptedContext))

    /** Returns the value with type [T] for [item]. */
    fun <T> get(item: ConstantItem<T>): T = getInner(item, item.defaultValue)

    /**
     * Retrieves the value for an [Item] from [SharedPreferences]. It handles method typing via the
     * default value type, and will throw an error if the type of the item provided is not a
     * `String`, `Boolean`, `Float`, `Int`, `Long`, or `Set<String>`.
     */
    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    private fun <T> getInner(item: Item, default: T): T {
        val sp = getSharedPrefs(item)
        return when {
            item.type == String::class.java -> sp.getString(item.sharedPrefKey, default as? String)
            item.type == Boolean::class.java || item.type == java.lang.Boolean::class.java ->
                sp.getBoolean(item.sharedPrefKey, default as Boolean)
            item.type == Int::class.java || item.type == java.lang.Integer::class.java ->
                sp.getInt(item.sharedPrefKey, default as Int)
            item.type == Float::class.java || item.type == java.lang.Float::class.java ->
                sp.getFloat(item.sharedPrefKey, default as Float)
            item.type == Long::class.java || item.type == java.lang.Long::class.java ->
                sp.getLong(item.sharedPrefKey, default as Long)
            Set::class.java.isAssignableFrom(item.type) ->
                sp.getStringSet(item.sharedPrefKey, default as? Set<String>)
            else ->
                throw IllegalArgumentException(
                    "item type: ${item.type}" + " is not compatible with sharedPref methods"
                )
        }
            as T
    }

    /**
     * Stores each of the values provided in `SharedPreferences` according to the configuration
     * contained within the associated items provided. Internally, it uses apply, so the caller
     * cannot assume that the values that have been put are immediately available for use.
     *
     * The forEach loop is necessary here since there is 1 `SharedPreference.Editor` returned from
     * prepareToPutValue(itemsToValues) for every distinct `SharedPreferences` file present in the
     * provided item configurations.
     */
    fun put(vararg itemsToValues: Pair<Item, Any>): Unit =
        prepareToPutValues(itemsToValues).forEach { it.apply() }

    /** See referenced `put` method above. */
    fun <T : Any> put(item: Item, value: T): Unit = put(item.to(value))

    /**
     * Synchronously stores all the values provided according to their associated Item
     * configuration.
     */
    fun putSync(vararg itemsToValues: Pair<Item, Any>): Unit =
        prepareToPutValues(itemsToValues).forEach { it.commit() }

    /**
     * Updates the values stored in `SharedPreferences` for each corresponding Item-value pair. If
     * the item is boot aware, this method updates both the boot aware and the encrypted files. This
     * is done because: 1) It allows for easy roll-back if the data is already in encrypted prefs
     * and we need to turn off the boot aware data feature & 2) It simplifies Backup/Restore, which
     * already points to encrypted storage.
     *
     * Returns a list of editors with all transactions added so that the caller can determine to use
     * .apply() or .commit()
     */
    private fun prepareToPutValues(
        updates: Array<out Pair<Item, Any>>
    ): List<SharedPreferences.Editor> {
        val updatesPerPrefFile = updates.groupBy { getSharedPrefs(it.first) }.toMap()

        return updatesPerPrefFile.map { (sharedPref, itemList) ->
            sharedPref.edit().apply { itemList.forEach { (item, value) -> putValue(item, value) } }
        }
    }

    /**
     * Handles adding values to `SharedPreferences` regardless of type. This method is especially
     * helpful for updating `SharedPreferences` values for `List<<Item>Any>` that have multiple
     * types of Item values.
     */
    @Suppress("UNCHECKED_CAST")
    internal fun SharedPreferences.Editor.putValue(
        item: Item,
        value: Any?,
    ): SharedPreferences.Editor =
        when {
            item.type == String::class.java -> putString(item.sharedPrefKey, value as? String)
            item.type == Boolean::class.java || item.type == java.lang.Boolean::class.java ->
                putBoolean(item.sharedPrefKey, value as Boolean)
            item.type == Int::class.java || item.type == java.lang.Integer::class.java ->
                putInt(item.sharedPrefKey, value as Int)
            item.type == Float::class.java || item.type == java.lang.Float::class.java ->
                putFloat(item.sharedPrefKey, value as Float)
            item.type == Long::class.java || item.type == java.lang.Long::class.java ->
                putLong(item.sharedPrefKey, value as Long)
            Set::class.java.isAssignableFrom(item.type) ->
                putStringSet(item.sharedPrefKey, value as? Set<String>)
            else ->
                throw IllegalArgumentException(
                    "item type: ${item.type} is not compatible with sharedPref methods"
                )
        }

    /**
     * After calling this method, the listener will be notified of any future updates to the
     * `SharedPreferences` files associated with the provided list of items. The listener will need
     * to filter update notifications so they don't activate for non-relevant updates.
     */
    fun addListener(listener: LauncherPrefChangeListener, vararg items: Item) {
        items
            .map { getSharedPrefs(it) }
            .distinct()
            .forEach { it.registerOnSharedPreferenceChangeListener(listener) }
    }

    /**
     * Stops the listener from getting notified of any more updates to any of the
     * `SharedPreferences` files associated with any of the provided list of [Item].
     */
    fun removeListener(listener: LauncherPrefChangeListener, vararg items: Item) {
        // If a listener is not registered to a SharedPreference, unregistering it does nothing
        items
            .map { getSharedPrefs(it) }
            .distinct()
            .forEach { it.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    /**
     * Checks if all the provided [Item] have values stored in their corresponding
     * `SharedPreferences` files.
     */
    fun has(vararg items: Item): Boolean {
        items
            .groupBy { getSharedPrefs(it) }
            .forEach { (prefs, itemsSublist) ->
                if (!itemsSublist.none { !prefs.contains(it.sharedPrefKey) }) return false
            }
        return true
    }

    /**
     * Asynchronously removes the [Item]'s value from its corresponding `SharedPreferences` file.
     */
    fun remove(vararg items: Item) = prepareToRemove(items).forEach { it.apply() }

    /** Synchronously removes the [Item]'s value from its corresponding `SharedPreferences` file. */
    fun removeSync(vararg items: Item) = prepareToRemove(items).forEach { it.commit() }

    /**
     * Removes the key value pairs stored in `SharedPreferences` for each corresponding Item. If the
     * item is boot aware, this method removes the data from both the boot aware and encrypted
     * files.
     *
     * @return a list of editors with all transactions added so that the caller can determine to use
     *   .apply() or .commit()
     */
    private fun prepareToRemove(items: Array<out Item>): List<SharedPreferences.Editor> {
        val itemsPerFile = items.groupBy { getSharedPrefs(it) }.toMap()

        return itemsPerFile.map { (prefs, items) ->
            prefs.edit().also { editor ->
                items.forEach { item -> editor.remove(item.sharedPrefKey) }
            }
        }
    }

    companion object {
        @VisibleForTesting const val BOOT_AWARE_PREFS_KEY = "boot_aware_prefs"

        @JvmField val INSTANCE = DaggerSingletonObject(LauncherAppComponent::getLauncherPrefs)

        @JvmStatic fun get(context: Context): LauncherPrefs = INSTANCE.get(context)

        const val TASKBAR_PINNING_KEY = "TASKBAR_PINNING_KEY"
        const val TASKBAR_PINNING_DESKTOP_MODE_KEY = "TASKBAR_PINNING_DESKTOP_MODE_KEY"
        const val SHOULD_SHOW_SMARTSPACE_KEY = "SHOULD_SHOW_SMARTSPACE_KEY"

        @JvmField
        val ENABLE_TWOLINE_ALLAPPS_TOGGLE = backedUpItem(BlissPrefs.PREF_ENABLE_TWO_LINE_TOGGLE, false)
        @JvmField val WORKSPACE_LOCK = backedUpItem(BlissPrefs.PREF_WORKSPACE_LOCK, false)
        @JvmField val PROMISE_ICON_IDS = backedUpItem(InstallSessionHelper.PROMISE_ICON_IDS, "")
        @JvmField val WORK_EDU_STEP = backedUpItem("showed_work_profile_edu", 0)
        @JvmField
        val WORKSPACE_SIZE =
            backedUpItem(DeviceGridState.KEY_WORKSPACE_SIZE, "", EncryptionType.ENCRYPTED)
        @JvmField
        val HOTSEAT_COUNT =
            backedUpItem(DeviceGridState.KEY_HOTSEAT_COUNT, -1, EncryptionType.ENCRYPTED)
        @JvmField
        val TASKBAR_PINNING =
            backedUpItem(TASKBAR_PINNING_KEY, false, EncryptionType.DEVICE_PROTECTED)
        @JvmField
        val TASKBAR_PINNING_IN_DESKTOP_MODE =
            backedUpItem(TASKBAR_PINNING_DESKTOP_MODE_KEY, true, EncryptionType.DEVICE_PROTECTED)

        @JvmField
        val DEVICE_TYPE =
            backedUpItem(
                DeviceGridState.KEY_DEVICE_TYPE,
                InvariantDeviceProfile.TYPE_PHONE,
                EncryptionType.ENCRYPTED,
            )
        @JvmField
        val DB_FILE = backedUpItem(DeviceGridState.KEY_DB_FILE, "", EncryptionType.ENCRYPTED)
        @JvmField
        val GRID_TYPE =
            backedUpItem(DeviceGridState.KEY_GRID_TYPE, GRID_TYPE_ANY, EncryptionType.ENCRYPTED)
        @JvmField
        val SHOULD_SHOW_SMARTSPACE =
            backedUpItem(
                SHOULD_SHOW_SMARTSPACE_KEY,
                WIDGET_ON_FIRST_SCREEN,
                EncryptionType.DEVICE_PROTECTED,
            )
        @JvmField
        val RESTORE_DEVICE =
            backedUpItem(
                RestoreDbTask.RESTORED_DEVICE_TYPE,
                InvariantDeviceProfile.TYPE_PHONE,
                EncryptionType.ENCRYPTED,
            )
        @JvmField
        val NO_DB_FILES_RESTORED =
            nonRestorableItem("no_db_files_restored", false, EncryptionType.DEVICE_PROTECTED)
        @JvmField
        val IS_FIRST_LOAD_AFTER_RESTORE =
            nonRestorableItem(FIRST_LOAD_AFTER_RESTORE_KEY, false, EncryptionType.ENCRYPTED)
        @JvmField
        val NEEDS_WIDGET_REBIND_AFTER_RESTORE =
            nonRestorableItem("needs_widget_rebind_after_restore", false, EncryptionType.ENCRYPTED)
        @JvmField
        val NEEDS_WORKSPACE_REORDER_AFTER_RESTORE =
            nonRestorableItem(
                "needs_workspace_reorder_after_restore",
                false,
                EncryptionType.ENCRYPTED,
            )
        @JvmField val APP_WIDGET_IDS = backedUpItem(RestoreDbTask.APPWIDGET_IDS, "")
        @JvmField val OLD_APP_WIDGET_IDS = backedUpItem(RestoreDbTask.APPWIDGET_OLD_IDS, "")

        @JvmField
        val GRID_NAME =
            ConstantItem(
                GRID_NAME_PREFS_KEY,
                isBackedUp = true,
                defaultValue = null,
                encryptionType = EncryptionType.ENCRYPTED,
                type = String::class.java,
            )
        @JvmField
        val ALLOW_ROTATION =
            backedUpItem(RotationHelper.ALLOW_ROTATION_PREFERENCE_KEY, Boolean::class.java) {
                RotationHelper.getAllowRotationDefaultValue(DisplayController.INSTANCE.get(it).info)
            }

        @JvmField
        val FIXED_LANDSCAPE_MODE = backedUpItem(BlissPrefs.PREF_FIXED_LANDSCAPE_MODE, false)

        @JvmField
        val NON_FIXED_LANDSCAPE_GRID_NAME =
            ConstantItem(
                NON_FIXED_LANDSCAPE_GRID_NAME_PREFS_KEY,
                isBackedUp = true,
                defaultValue = null,
                encryptionType = EncryptionType.ENCRYPTED,
                type = String::class.java,
            )

        // Preferences for widget configurations
        @JvmField
        val RECONFIGURABLE_WIDGET_EDUCATION_TIP_SEEN =
            backedUpItem("launcher.reconfigurable_widget_education_tip_seen", false)

        @JvmField
        val IS_SINGLE_LAYER_ENABLED = backedUpItem(BlissPrefs.PREF_SINGLE_LAYER_MODE, false)

        @JvmField
        val IS_NOTIF_COUNT_ENABLED = backedUpItem(BlissPrefs.PREF_NOTIF_COUNT, true)

        @JvmField
        val ICON_SIZE_FACTOR = backedUpItem(BlissPrefs.PREF_ICON_SIZE_FACTOR, 100)

        @JvmField
        val SHOW_HOME_LABELS = backedUpItem(BlissPrefs.PREF_SHOW_HOME_LABELS, true)

        @JvmField
        val SHOW_DRAWER_LABELS = backedUpItem(BlissPrefs.PREF_SHOW_DRAWER_LABELS, true)

        @JvmField
        val HIDDEN_APPS: ConstantItem<Set<String>> = backedUpItem(BlissPrefs.PREF_HIDDEN_APPS, emptySet<String>())

        @JvmField
        val SHOW_DOCK = backedUpItem(BlissPrefs.PREF_SHOW_DOCK, true)

        @JvmField
        val DOCK_ICON_COUNT = backedUpItem(BlissPrefs.PREF_DOCK_ICON_COUNT, -1)

        @JvmField
        val BLUR_INTENSITY = backedUpItem(BlissPrefs.PREF_BLUR_INTENSITY, 100)

        @JvmField
        val GESTURE_DOUBLE_TAP = backedUpItem(BlissPrefs.PREF_GESTURE_DOUBLE_TAP, "sleep")

        @JvmField
        val GESTURE_SWIPE_DOWN = backedUpItem(BlissPrefs.PREF_GESTURE_SWIPE_DOWN, "notifications")

        @JvmField
        val DRAWER_COLUMNS = backedUpItem(BlissPrefs.PREF_DRAWER_COLUMNS, -1)

        @JvmField
        val SHOW_FOLDER_LABELS = backedUpItem(BlissPrefs.PREF_SHOW_FOLDER_LABELS, true)

        @JvmField
        val FOLDER_COLUMNS = backedUpItem(BlissPrefs.PREF_FOLDER_COLUMNS, -1)

        @JvmField
        val ICON_PACK = backedUpItem(BlissPrefs.PREF_ICON_PACK, "")

        @JvmField
        val DRAWER_OPACITY = backedUpItem(BlissPrefs.PREF_DRAWER_OPACITY, 100)

        @JvmField
        val DRAWER_BG_COLOR = backedUpItem(BlissPrefs.PREF_DRAWER_BG_COLOR, "default")

        @JvmField
        val SEARCH_RESULT_COUNT = backedUpItem(BlissPrefs.PREF_SEARCH_RESULT_COUNT, 5)

        @JvmField
        val SEARCH_PROVIDER = backedUpItem(BlissPrefs.PREF_SEARCH_PROVIDER, "default")

        @JvmField
        val WALLPAPER_SCROLLING = backedUpItem(BlissPrefs.PREF_WALLPAPER_SCROLLING, true)

        @JvmField
        val FOLDER_BG_COLOR = backedUpItem(BlissPrefs.PREF_FOLDER_BG_COLOR, "default")

        @JvmField
        val FOLDER_BG_OPACITY = backedUpItem(BlissPrefs.PREF_FOLDER_BG_OPACITY, 100)

        @JvmField
        val FOLDER_PREVIEW_BG_OPACITY = backedUpItem(BlissPrefs.PREF_FOLDER_PREVIEW_BG_OPACITY, 100)

        @JvmField
        val PAGE_INDICATOR_HEIGHT_FACTOR =
                backedUpItem(BlissPrefs.PREF_PAGE_INDICATOR_HEIGHT_FACTOR, 100)

        @JvmField
        val WORKSPACE_TOP_PADDING_FACTOR =
                backedUpItem(BlissPrefs.PREF_WORKSPACE_TOP_PADDING_FACTOR, 100)

        @JvmField
        val WORKSPACE_BOTTOM_PADDING_FACTOR =
                backedUpItem(BlissPrefs.PREF_WORKSPACE_BOTTOM_PADDING_FACTOR, 100)

        @JvmField
        val ADD_ICON_TO_HOME = backedUpItem(BlissPrefs.PREF_ADD_ICON_TO_HOME, true)

        @JvmField
        val WRAP_ADAPTIVE_ICONS = backedUpItem(BlissPrefs.PREF_WRAP_ADAPTIVE_ICONS, true)

        // Feature flag for the spring-based folder open/close animation.
        // Migration02 Phase 2: Lawnchair animator ported, default flipped to true.
        @JvmField
        val FOLDER_SPRING_ANIM = backedUpItem(BlissPrefs.PREF_FOLDER_SPRING_ANIM, true)

        @JvmField
        val HIDDEN_APPS_IN_SEARCH =
                backedUpItem(BlissPrefs.PREF_HIDDEN_APPS_IN_SEARCH, "never")

        @JvmField
        val DOT_TEXT_COLOR = backedUpItem(BlissPrefs.PREF_DOT_TEXT_COLOR, "auto")

        @JvmField
        val WALLPAPER_DEPTH_EFFECT =
                backedUpItem(BlissPrefs.PREF_WALLPAPER_DEPTH_EFFECT, true)

        @JvmField
        val ALLOW_WIDGET_OVERLAP =
                backedUpItem(BlissPrefs.PREF_ALLOW_WIDGET_OVERLAP, false)

        @JvmField
        val DRAWER_HAPTIC = backedUpItem(BlissPrefs.PREF_DRAWER_HAPTIC, false)

        @JvmField
        val HOME_LABEL_COLOR_MODE =
                backedUpItem(BlissPrefs.PREF_HOME_LABEL_COLOR_MODE, "auto")

        @JvmField
        val DRAWER_CELL_HEIGHT_FACTOR =
                backedUpItem(BlissPrefs.PREF_DRAWER_CELL_HEIGHT_FACTOR, 100)

        @JvmField
        val DRAWER_LEFT_RIGHT_MARGIN_FACTOR =
                backedUpItem(BlissPrefs.PREF_DRAWER_LEFT_RIGHT_MARGIN_FACTOR, 100)

        @JvmField
        val DRAWER_SHOW_SCROLLBAR =
                backedUpItem(BlissPrefs.PREF_DRAWER_SHOW_SCROLLBAR, true)

        @JvmField
        val WORK_TAB_BG_COLOR =
                backedUpItem(BlissPrefs.PREF_WORK_TAB_BG_COLOR, "default")

        @JvmField
        val DRAWER_THEMED_ICONS =
                backedUpItem(BlissPrefs.PREF_DRAWER_THEMED_ICONS, false)

        @JvmField
        val TINT_ICON_PACK_BG =
                backedUpItem(BlissPrefs.PREF_TINT_ICON_PACK_BG, false)

        @JvmField
        val HOTSEAT_INSET_LEFT = backedUpItem(BlissPrefs.PREF_HOTSEAT_INSET_LEFT, 0)

        @JvmField
        val HOTSEAT_INSET_RIGHT = backedUpItem(BlissPrefs.PREF_HOTSEAT_INSET_RIGHT, 0)

        @JvmField
        val HOTSEAT_INSET_TOP = backedUpItem(BlissPrefs.PREF_HOTSEAT_INSET_TOP, 0)

        @JvmField
        val HOTSEAT_INSET_BOTTOM = backedUpItem(BlissPrefs.PREF_HOTSEAT_INSET_BOTTOM, 0)

        @JvmField
        val DOCK_SEARCH_BAR = backedUpItem(BlissPrefs.PREF_DOCK_SEARCH_BAR, true)

        @JvmField
        val SHOW_SUGGESTED_APPS = backedUpItem(BlissPrefs.PREF_SHOW_SUGGESTED_APPS, false)

        @JvmField
        val SEARCH_PEOPLE = backedUpItem(BlissPrefs.PREF_SEARCH_PEOPLE, false)

        @JvmField
        val SEARCH_FILES = backedUpItem(BlissPrefs.PREF_SEARCH_FILES, false)

        @JvmField
        val SEARCH_AUDIO = backedUpItem(BlissPrefs.PREF_SEARCH_AUDIO, false)

        @JvmField
        val SEARCH_VISUAL_MEDIA = backedUpItem(BlissPrefs.PREF_SEARCH_VISUAL_MEDIA, false)

        @JvmField
        val SEARCH_SETTINGS = backedUpItem(BlissPrefs.PREF_SEARCH_SETTINGS, false)

        @JvmField
        val SEARCH_STARTPAGE_SUGGESTION =
                backedUpItem(BlissPrefs.PREF_SEARCH_STARTPAGE_SUGGESTION, false)

        @JvmField
        val WEB_SUGGESTION_URL = backedUpItem(BlissPrefs.PREF_WEB_SUGGESTION_URL, "")

        @JvmField
        val WEB_SUGGESTION_NAME = backedUpItem(BlissPrefs.PREF_WEB_SUGGESTION_NAME, "")

        @JvmField
        val WEB_SUGGESTION_MAX_DELAY =
                backedUpItem(BlissPrefs.PREF_WEB_SUGGESTION_MAX_DELAY, 1000)

        @JvmField
        val WEB_SUGGESTION_MAX_COUNT =
                backedUpItem(BlissPrefs.PREF_WEB_SUGGESTION_MAX_COUNT, 5)

        /** Custom icon shape corner radius percent (0=square, 50=circle). */
        @JvmField
        val CUSTOM_ICON_SHAPE_RADIUS =
                backedUpItem(BlissPrefs.PREF_CUSTOM_ICON_SHAPE_RADIUS, 25)

        /** Drawer-folders feature flag. Render path is scaffolded only; full UI
         *  is a separate scope. Off by default so existing flat drawer renders. */
        @JvmField
        val DRAWER_FOLDERS_ENABLED =
                backedUpItem(BlissPrefs.PREF_DRAWER_FOLDERS_ENABLED, false)

        /** JSON {"folderName":["pkg1","pkg2", ...], ...} for drawer folders.
         *  Kept as the import shape; runtime persistence has moved to Room
         *  (see DrawerFolderService) as of Migration02 / Phase 1. */
        @JvmField
        val DRAWER_FOLDERS_MAP =
                backedUpItem(BlissPrefs.PREF_DRAWER_FOLDERS_MAP, "{}")

        /** One-shot flag tracking the JSON→Room migration (Migration02 / Phase 1.9). */
        @JvmField
        val DRAWER_FOLDERS_MIGRATED_V1 =
                backedUpItem(BlissPrefs.PREF_DRAWER_FOLDERS_MIGRATED_V1, false)

        @JvmField
        val INFINITE_SCROLLING = backedUpItem(BlissPrefs.PREF_INFINITE_SCROLLING, false)

        @JvmField
        val SHOW_STATUS_BAR = backedUpItem(BlissPrefs.PREF_SHOW_STATUS_BAR, true)

        @JvmField
        val FORCE_WIDGET_RESIZE = backedUpItem(BlissPrefs.PREF_FORCE_WIDGET_RESIZE, false)

        @JvmField
        val WIDGET_ROUNDED_CORNERS = backedUpItem(BlissPrefs.PREF_WIDGET_ROUNDED_CORNERS, true)

        @JvmField
        val WIDGET_UNLIMITED_SIZE = backedUpItem(BlissPrefs.PREF_WIDGET_UNLIMITED_SIZE, false)

        @JvmField
        val DRAWER_ICON_SIZE_FACTOR = backedUpItem(BlissPrefs.PREF_DRAWER_ICON_SIZE_FACTOR, 100)

        @JvmField
        val HOME_LABEL_SIZE_FACTOR = backedUpItem(BlissPrefs.PREF_HOME_LABEL_SIZE_FACTOR, 100)

        @JvmField
        val DRAWER_LABEL_SIZE_FACTOR = backedUpItem(BlissPrefs.PREF_DRAWER_LABEL_SIZE_FACTOR, 100)

        @JvmField
        val FONT_FAMILY = backedUpItem(BlissPrefs.PREF_FONT_FAMILY, "default")

        @JvmField
        val ACCENT_COLOR = backedUpItem(BlissPrefs.PREF_ACCENT_COLOR, "system")

        @JvmField
        val SHOW_AT_A_GLANCE = backedUpItem(BlissPrefs.PREF_SHOW_AT_A_GLANCE, false)

        @JvmField
        val HIDE_DRAWER_SEARCH = backedUpItem(BlissPrefs.PREF_HIDE_DRAWER_SEARCH, false)

        @JvmField
        val AUTO_SHOW_KEYBOARD = backedUpItem(BlissPrefs.PREF_AUTO_SHOW_KEYBOARD, false)

        @JvmField
        val FUZZY_SEARCH = backedUpItem(BlissPrefs.PREF_FUZZY_SEARCH, false)

        @JvmField
        val SHOW_CALCULATOR = backedUpItem(BlissPrefs.PREF_SHOW_CALCULATOR, true)

        @JvmField
        val DRAWER_SORT_ORDER = backedUpItem(BlissPrefs.PREF_DRAWER_SORT_ORDER, "alphabetical")

        @JvmField
        val DOCK_BG_COLOR = backedUpItem(BlissPrefs.PREF_DOCK_BG_COLOR, "default")

        @JvmField
        val DOCK_BG_OPACITY = backedUpItem(BlissPrefs.PREF_DOCK_BG_OPACITY, 100)

        @JvmField
        val SEARCH_BAR_COLOR = backedUpItem(BlissPrefs.PREF_SEARCH_BAR_COLOR, "default")

        @JvmField
        val SEARCH_BAR_RADIUS = backedUpItem(BlissPrefs.PREF_SEARCH_BAR_RADIUS, 100)

        @JvmField
        val DOT_COLOR = backedUpItem(BlissPrefs.PREF_DOT_COLOR, "default")

        @JvmField
        val PAGE_TRANSITION = backedUpItem(BlissPrefs.PREF_PAGE_TRANSITION, "default")

        @JvmField
        val DARK_MODE = backedUpItem(BlissPrefs.PREF_DARK_MODE, "auto")

        @JvmField
        val WIDGET_PADDING = backedUpItem(BlissPrefs.PREF_WIDGET_PADDING, 0)

        @JvmField
        val DRAWER_LIST_VIEW = backedUpItem(BlissPrefs.PREF_DRAWER_LIST_VIEW, false)

        @JvmField
        val CONTACT_SEARCH = backedUpItem(BlissPrefs.PREF_CONTACT_SEARCH, false)

        @JvmField
        val SHORTCUT_SEARCH = backedUpItem(BlissPrefs.PREF_SHORTCUT_SEARCH, false)

        @JvmField
        val SEARCH_BAR_BOTTOM = backedUpItem(BlissPrefs.PREF_SEARCH_BAR_BOTTOM, false)

        @JvmField
        val GESTURE_EDGE_LEFT = backedUpItem(BlissPrefs.PREF_GESTURE_EDGE_LEFT, "none")

        @JvmField
        val GESTURE_EDGE_RIGHT = backedUpItem(BlissPrefs.PREF_GESTURE_EDGE_RIGHT, "none")

        @JvmField
        val DOCK_LABELS = backedUpItem(BlissPrefs.PREF_DOCK_LABELS, false)

        @JvmField
        val DOCK_CORNER_RADIUS = backedUpItem(BlissPrefs.PREF_DOCK_CORNER_RADIUS, 0)

        @JvmField
        val APP_LAUNCH_ANIMATION = backedUpItem(BlissPrefs.PREF_APP_LAUNCH_ANIMATION, "default")

        @JvmField
        val FONT_WEIGHT = backedUpItem(BlissPrefs.PREF_FONT_WEIGHT, "normal")

        @JvmField
        val KEYBOARD_AUTO_HIDE = backedUpItem(BlissPrefs.PREF_KEYBOARD_AUTO_HIDE, true)

        @JvmField
        val FOLDER_BADGES = backedUpItem(BlissPrefs.PREF_FOLDER_BADGES, true)

        @JvmField
        val DRAWER_ANIMATION = backedUpItem(BlissPrefs.PREF_DRAWER_ANIMATION, "default")

        /**
         * Comma-separated ordered list of home long-press popup tokens.
         * Empty string = use default (hardcoded) order. Recognized tokens:
         * "wallpaper", "widgets", "edit_mode", "all_apps", "settings",
         * "lock", "sys_settings". Unknown tokens are skipped.
         */
        @JvmField
        val HOME_POPUP_ORDER = backedUpItem(BlissPrefs.PREF_HOME_POPUP_ORDER, "")

        /**
         * JSON map of per-app label overrides. Keys are
         * "pkg/component#userSerial" (Lawnchair-compatible format); values are
         * the user-supplied display name. Empty string = no overrides.
         */
        @JvmField
        val APP_NAME_OVERRIDES = backedUpItem(BlissPrefs.PREF_APP_NAME_OVERRIDES, "")

        /** 0-100: percentage to lift the dock from its default bottom position
         *  (0 = default flush-with-bottom, 100 = lifted by approx. dock height). */
        @JvmField
        val HOTSEAT_BOTTOM_FACTOR = backedUpItem(BlissPrefs.PREF_HOTSEAT_BOTTOM_FACTOR, 100)

        @JvmField
        val HOTSEAT_BOTTOM_FACTOR_MIGRATED_V1 =
                backedUpItem(BlissPrefs.PREF_HOTSEAT_FACTOR_V1_MIGRATED, false)

        /** 50-200: scale factor (percent) for folder icon labels; 100 = default. */
        @JvmField
        val FOLDER_LABEL_SIZE_FACTOR = backedUpItem(BlissPrefs.PREF_FOLDER_LABEL_SIZE_FACTOR, 100)

        // Smartspace sub-toggles. These store user intent imported from
        // Lawnchair; SHOW_AT_A_GLANCE remains the master switch consumed
        // by the existing widget.
        @JvmField
        val SMARTSPACE_SHOW_TIME = backedUpItem(BlissPrefs.PREF_SMARTSPACE_SHOW_TIME, true)

        @JvmField
        val SMARTSPACE_SHOW_DATE = backedUpItem(BlissPrefs.PREF_SMARTSPACE_SHOW_DATE, true)

        @JvmField
        val SMARTSPACE_NOW_PLAYING = backedUpItem(BlissPrefs.PREF_SMARTSPACE_NOW_PLAYING, true)

        /** "system" (follow Android), "12", or "24". */
        @JvmField
        val SMARTSPACE_TIME_FORMAT = backedUpItem(BlissPrefs.PREF_SMARTSPACE_TIME_FORMAT, "system")

        @JvmField
        val USE_24H_FORMAT = backedUpItem(BlissPrefs.PREF_USE_24H_FORMAT, false)

        /** Force light icons / dark backdrop on the status bar. */
        @JvmField
        val DARK_STATUS_BAR = backedUpItem(BlissPrefs.PREF_DARK_STATUS_BAR, false)

        /** Lawnchair-aligned closing-app overlay mode. Values are
         *  `none` / `fade_in` / `suck_in` (matched by CloseOverlayMode.prefValue).
         *  Migration02 / Phase 4 wires these to the real
         *  FullScreenOverlayView animations. */
        @JvmField
        val CLOSING_APP_OVERLAY = backedUpItem(BlissPrefs.PREF_CLOSING_APP_OVERLAY, "none")

        /** One-shot flag tracking the Migration01 -> Migration02 / Phase 4
         *  closing-overlay value migration. Migration01 stored fade/scale/slide_down;
         *  Migration02 only knows none/fade_in/suck_in. We rewrite stale values to
         *  `none` once and never look again. */
        @JvmField
        val CLOSING_OVERLAY_MIGRATED_V1 =
                backedUpItem(BlissPrefs.PREF_CLOSING_OVERLAY_V1_MIGRATED, false)

        /** When true, drawer remembers last scroll position across opens. */
        @JvmField
        val REMEMBER_DRAWER_POSITION = backedUpItem(BlissPrefs.PREF_REMEMBER_DRAWER_POSITION, false)

        /**
         * When true, the workspace reorder algorithm preserves intentional empty cells
         * instead of forcing the iOS-style top-left compaction in single-layer mode.
         * Set to true automatically by the Lawnchair importer (Lawnchair allows gaps and
         * users restoring such layouts expect them preserved). Default false keeps the
         * stock BlissLauncher single-layer-mode packing behaviour for fresh installs.
         */
        @JvmField
        val PRESERVE_LAYOUT_GAPS = backedUpItem(BlissPrefs.PREF_PRESERVE_LAYOUT_GAPS, false)

        /**
         * When true, closed folder icons use the 2×2 grid preview layout (the
         * Lawnchair / iOS-style appearance). When false, BlissLauncher's default
         * clipped-circle clustering is used. The aconfig flag
         * {@code enable_launcher_icon_shapes} also force-enables this. Set to
         * true automatically by the Lawnchair importer.
         */
        @JvmField
        val GRID_FOLDER_PREVIEW = backedUpItem(BlissPrefs.PREF_GRID_FOLDER_PREVIEW, false)

        /**
         * Tracks whether the layout-mode chooser dialog (single-layer vs
         * multi-layer with drawer) has been shown to the user. Default false →
         * dialog appears on first launch. Set to true after the user picks an
         * option (or after a Lawnchair import, which sets the layout choice
         * implicitly and skips the wizard).
         */
        @JvmField
        val FIRST_RUN_LAYOUT_CHOICE_DONE =
                backedUpItem(BlissPrefs.PREF_FIRST_RUN_LAYOUT_CHOICE_DONE, false)

        /**
         * Per-step completion ledger for the first-run wizard
         * (foundation.e.bliss.firstrun.FirstRunWizard). Each entry is a
         * step's stable id (e.g. "layout_mode_v1", "import_lawnchair_v1").
         * When the set covers every known step's id,
         * FIRST_RUN_LAYOUT_CHOICE_DONE flips to true and the wizard is
         * suppressed on subsequent launches. Migration04 phase 07.
         */
        @JvmField
        val FIRST_RUN_STEPS_DONE: ConstantItem<Set<String>> =
                backedUpItem(BlissPrefs.PREF_FIRST_RUN_STEPS_DONE, emptySet<String>())

        /** JSON map of folderId -> "#RRGGBB" overrides. Per-folder colors
         *  beyond the global FOLDER_BG_COLOR. */
        @JvmField
        val FOLDER_COLOR_OVERRIDES = backedUpItem(BlissPrefs.PREF_FOLDER_COLOR_OVERRIDES, "")

        /** Phase 5: JSON array of recent ARGB hex strings (max 8). Used by BlissColorPickerView. */
        @JvmField
        val RECENT_COLORS = backedUpItem(BlissPrefs.PREF_RECENT_COLORS, "[]")

        @JvmStatic
        fun <T> backedUpItem(
            sharedPrefKey: String,
            defaultValue: T,
            encryptionType: EncryptionType = EncryptionType.ENCRYPTED,
        ): ConstantItem<T> =
            ConstantItem(sharedPrefKey, isBackedUp = true, defaultValue, encryptionType)

        @JvmStatic
        fun <T> backedUpItem(
            sharedPrefKey: String,
            type: Class<out T>,
            encryptionType: EncryptionType = EncryptionType.ENCRYPTED,
            defaultValueFromContext: (c: Context) -> T,
        ): ContextualItem<T> =
            ContextualItem(
                sharedPrefKey,
                isBackedUp = true,
                defaultValueFromContext,
                encryptionType,
                type,
            )

        @JvmStatic
        fun <T> nonRestorableItem(
            sharedPrefKey: String,
            defaultValue: T,
            encryptionType: EncryptionType = EncryptionType.ENCRYPTED,
        ): ConstantItem<T> =
            ConstantItem(sharedPrefKey, isBackedUp = false, defaultValue, encryptionType)

        @Deprecated("Don't use shared preferences directly. Use other LauncherPref methods.")
        @JvmStatic
        fun getPrefs(context: Context): SharedPreferences {
            // Use application context for shared preferences, so we use single cached instance
            return context.applicationContext.getSharedPreferences(
                SHARED_PREFERENCES_KEY,
                MODE_PRIVATE,
            )
        }

        @Deprecated("Don't use shared preferences directly. Use other LauncherPref methods.")
        @JvmStatic
        fun getDevicePrefs(context: Context): SharedPreferences {
            // Use application context for shared preferences, so we use a single cached instance
            return context.applicationContext.getSharedPreferences(
                DEVICE_PREFERENCES_KEY,
                MODE_PRIVATE,
            )
        }
    }
}

abstract class Item {
    abstract val sharedPrefKey: String
    abstract val isBackedUp: Boolean
    abstract val type: Class<*>
    abstract val encryptionType: EncryptionType
    val sharedPrefFile: String
        get() = if (isBackedUp) SHARED_PREFERENCES_KEY else DEVICE_PREFERENCES_KEY

    fun <T> to(value: T): Pair<Item, T> = Pair(this, value)
}

data class ConstantItem<T>(
    override val sharedPrefKey: String,
    override val isBackedUp: Boolean,
    val defaultValue: T,
    override val encryptionType: EncryptionType,
    // The default value can be null. If so, the type needs to be explicitly stated, or else NPE
    override val type: Class<out T> = defaultValue!!::class.java,
) : Item() {

    fun get(c: Context): T = LauncherPrefs.get(c).get(this)
}

data class ContextualItem<T>(
    override val sharedPrefKey: String,
    override val isBackedUp: Boolean,
    private val defaultSupplier: (c: Context) -> T,
    override val encryptionType: EncryptionType,
    override val type: Class<out T>,
) : Item() {
    private var default: T? = null

    fun defaultValueFromContext(context: Context): T {
        if (default == null) {
            default = defaultSupplier(context)
        }
        return default!!
    }

    fun get(c: Context): T = LauncherPrefs.get(c).get(this)
}

enum class EncryptionType {
    ENCRYPTED,
    DEVICE_PROTECTED,
}

/**
 * LauncherPrefs which delegates all lookup to [prefs] but uses the real prefs for initial values
 */
class ProxyPrefs(context: Context, private val prefs: SharedPreferences) : LauncherPrefs(context) {

    private val copiedPrefs = ConcurrentHashMap<SharedPreferences, Boolean>()

    override fun getSharedPrefs(item: Item): SharedPreferences {
        val originalPrefs = super.getSharedPrefs(item)
        // Copy all existing values, when the pref is accessed for the first time
        copiedPrefs.computeIfAbsent(originalPrefs) { op ->
            val editor = prefs.edit()
            op.all.forEach { (key, value) ->
                if (value != null) {
                    editor.putValue(backedUpItem(key, value), value)
                }
            }
            editor.commit()
        }
        return prefs
    }
}
