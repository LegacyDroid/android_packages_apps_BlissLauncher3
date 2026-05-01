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
/*
 * File:    bliss/src/foundation/e/bliss/folder/GridFolderIconLayoutRule.kt
 * Module:  bliss main source-set  (foundation.e.bliss.folder)
 * Role:    REFACTORED
 *
 * Tree (foundation/e/bliss/folder/):
 *   ├── GridFolderController.kt           — selects the layout rule per FolderIcon
 *   ├── GridFolderIcon.kt                 — single-layer-mode FolderIcon variant
 *   ├── GridFolderIconLayoutRule.kt       — preview-grid impl (this file)         ← THIS FILE
 *   ├── GridFolder.kt                     — opened-folder layout for grid mode
 *   ├── PreviewItem.kt                    — preview-item view
 *   └── PreviewItemDecoration.kt          — preview-item background decoration
 *
 * Purpose:
 *   Single-layer-mode subclass of ClippedFolderIconLayoutRule. Reads its
 *   2×2-vs-3×3 grid configuration from the Bliss-side FolderPreviewPolicy
 *   strategy (LauncherPolicy.folderPreview) instead of poking the
 *   pref-registry constant directly. Keeps Bliss-folder UI sized correctly
 *   when the user has the Lawnchair-style 2×2 preview on.
 *
 * Consumed by:
 *   - foundation.e.bliss.folder.GridFolderController             — per-FolderIcon instantiation
 *
 * Calls into:
 *   - foundation.e.bliss.policy.LauncherPolicy                   — folderPreview()
 *   - com.android.launcher3.folder.ClippedFolderIconLayoutRule   — superclass
 *
 * Plan reference: Plans/Migration04/05-launcher-policy-strategies.md §5.3
 */
package foundation.e.bliss.folder

import android.content.Context
import com.android.launcher3.folder.ClippedFolderIconLayoutRule
import com.android.launcher3.folder.PreviewItemDrawingParams
import foundation.e.bliss.policy.LauncherPolicy

class GridFolderIconLayoutRule(context: Context) : ClippedFolderIconLayoutRule(context) {
    private val mGridCountX: Int
    private val mGridCountY: Int
    private val mItemIconScale: Float
    private val maxNumItemsInPreview: Int

    init {
        val policy = LauncherPolicy.folderPreview(context)
        mGridCountX = policy.gridCountX
        mGridCountY = policy.gridCountY
        mItemIconScale = policy.itemIconScale
        maxNumItemsInPreview = policy.maxNumItemsInPreview
    }

    override fun computePreviewItemDrawingParams(
        index: Int,
        curNumItems: Int,
        params: PreviewItemDrawingParams?,
    ): PreviewItemDrawingParams {
        val transX: Float
        val transY: Float
        val scale = scaleForItem(index)
        val point = FloatArray(2)

        if (index < maxNumItemsInPreview) {
            var baseX = index % mGridCountX
            val baseY = index / mGridCountY
            if (mIsRtl) {
                baseX = (mGridCountX - 1) - baseX
            }
            var paddingX = (mAvailableSpace - (iconSize * scale) * mGridCountX) / (mGridCountX + 1)
            if (paddingX < 0) {
                paddingX = 0f
            }
            var paddingY = (mAvailableSpace - (iconSize * scale) * mGridCountY) / (mGridCountY + 1)
            if (paddingY < 0) {
                paddingY = 0f
            }
            point[0] = (baseX + 1) * paddingX + baseX * (iconSize * scale)
            point[1] = (baseY + 1) * paddingY + baseY * (iconSize * scale)
        } else {
            point[1] = mAvailableSpace / 2 - (iconSize * scale) / 2
            point[0] = point[1]
        }
        transX = point[0]
        transY = point[1]
        if (params == null) {
            return PreviewItemDrawingParams(transX, transY, scale)
        }

        params.update(transX, transY, scale)
        return params
    }

    override fun scaleForItem(numItems: Int): Float {
        return mItemIconScale * mBaselineIconScale
    }

    override fun getMaxNumItemsInPreview(): Int {
        return maxNumItemsInPreview
    }
}
