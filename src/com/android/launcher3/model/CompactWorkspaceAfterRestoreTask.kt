/*
 * Copyright (C) 2026 e Foundation
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
package com.android.launcher3.model

import com.android.launcher3.InvariantDeviceProfile
import com.android.launcher3.LauncherPrefs
import com.android.launcher3.LauncherSettings.Favorites.CONTAINER_DESKTOP
import com.android.launcher3.LauncherSettings.Favorites.CONTAINER_HOTSEAT
import com.android.launcher3.ModelUpdateTask
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.util.IntSet
import com.android.launcher3.Utilities.SHOULD_SHOW_FIRST_PAGE_WIDGET
import com.android.launcher3.WorkspaceLayoutManager.FIRST_SCREEN_ID
import com.android.launcher3.config.FeatureFlags
import com.android.launcher3.util.GridOccupancy

class CompactWorkspaceAfterRestoreTask : ModelUpdateTask {

    override fun isIgnoreLoaded() = true

    override fun execute(taskController: ModelTaskController, dataModel: BgDataModel, apps: AllAppsList) {
        val prefs = LauncherPrefs.get(taskController.context)
        if (!prefs.get(LauncherPrefs.NEEDS_WORKSPACE_REORDER_AFTER_RESTORE)) return

        val idp = InvariantDeviceProfile.INSTANCE.get(taskController.context)
        val columns = idp.numColumnsFixed
        val rows = idp.numRowsFixed
        val hotseatCapacity = idp.numDatabaseHotseatIcons

        val screenIds = mutableListOf<Int>()
        val fixedItems = ArrayList<ItemInfo>()
        val movableItems = ArrayList<ItemInfo>()
        val hotseatItems = ArrayList<ItemInfo>()

        synchronized(dataModel) {
            val screens = dataModel.collectWorkspaceScreens()
            for (i in 0 until screens.size()) {
                screenIds.add(screens[i])
            }
            screenIds.sort()

            for (item in dataModel.itemsIdMap) {
                if (item.container == CONTAINER_HOTSEAT) {
                    hotseatItems.add(item)
                    continue
                }
                if (item.container != CONTAINER_DESKTOP) continue
                val isFixed = item.spanX > 1 || item.spanY > 1
                if (isFixed) {
                    fixedItems.add(item)
                } else if (item.spanX == 1 && item.spanY == 1) {
                    movableItems.add(item)
                }
            }
        }

        if (hotseatItems.isEmpty() && movableItems.isEmpty() && fixedItems.isEmpty()) return

        val updated = ArrayList<ItemInfo>()

        val overflowedFromHotseat = ArrayList<ItemInfo>()
        hotseatItems.sortBy { it.screenId }
        hotseatItems.forEachIndexed { rank, item ->
            if (rank >= hotseatCapacity) {
                item.container = CONTAINER_DESKTOP
                overflowedFromHotseat.add(item)
            } else if (item.screenId != rank) {
                item.screenId = rank
                updated.add(item)
            }
        }

        movableItems.sortWith(compareBy({ it.screenId }, { it.cellY }, { it.cellX }))
        movableItems.addAll(overflowedFromHotseat)

        val screensToExclude = IntSet()
        if (FeatureFlags.QSB_ON_FIRST_SCREEN.get() && !SHOULD_SHOW_FIRST_PAGE_WIDGET) {
            screensToExclude.add(FIRST_SCREEN_ID)
        }

        val occupiedByScreen = HashMap<Int, GridOccupancy>(screenIds.size)
        fun occupancyFor(screenId: Int) =
            occupiedByScreen.getOrPut(screenId) { GridOccupancy(columns, rows) }

        fixedItems.forEach { occupancyFor(it.screenId).markCells(it, true) }

        val forcedIds = overflowedFromHotseat.mapTo(HashSet()) { it.id }
        val xy = IntArray(2)
        movableItems.forEach { item ->
            val forced = item.id in forcedIds
            var placed = false
            for (screenId in screenIds) {
                if (screensToExclude.contains(screenId)) continue
                val occupancy = occupancyFor(screenId)
                if (!occupancy.findVacantCell(xy, item.spanX, item.spanY)) continue
                placed = true
                updatedIfChanged(item, screenId, xy[0], xy[1], columns, updated, forced)
                occupancy.markCells(xy[0], xy[1], item.spanX, item.spanY, true)
                break
            }
            if (!placed) {
                val newScreenId = taskController.model.modelDbController.getNewScreenId()
                screenIds.add(newScreenId)
                val occupancy = occupancyFor(newScreenId)
                if (occupancy.findVacantCell(xy, item.spanX, item.spanY)) {
                    updatedIfChanged(item, newScreenId, xy[0], xy[1], columns, updated, forced)
                    occupancy.markCells(xy[0], xy[1], item.spanX, item.spanY, true)
                }
            }
        }

        prefs.putSync(LauncherPrefs.NEEDS_WORKSPACE_REORDER_AFTER_RESTORE.to(false))

        if (updated.isEmpty()) {
            return
        }
        val writer = taskController.getModelWriter()
        updated.forEach { writer.updateItemInDatabase(it) }
        taskController.model.forceReload()
    }

    private fun updatedIfChanged(
        item: ItemInfo,
        screenId: Int,
        cellX: Int,
        cellY: Int,
        columns: Int,
        updated: MutableList<ItemInfo>,
        force: Boolean = false,
    ) {
        if (!force && item.screenId == screenId && item.cellX == cellX && item.cellY == cellY) {
            return
        }
        item.screenId = screenId
        item.cellX = cellX
        item.cellY = cellY
        item.rank = cellX + (cellY * columns)
        updated.add(item)
    }
}
