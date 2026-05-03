/*
 * Copyright (C) 2025 The Android Open Source Project
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
 * File:    bliss/src/com/android/launcher3/folder/FolderAnimationData.kt
 * Module:  bliss source-set  (com.android.launcher3.folder)
 * Role:    REFACTORED
 *
 * Tree (com/android/launcher3/folder/):
 *   ├── ClipRevealData.kt                       — clip-rect data for the reveal animator
 *   ├── FolderAnimationCreator.kt               — factory entrypoint used by Folder.java
 *   ├── FolderAnimationData.kt                  — folder open/close geometry + scaling  ← THIS FILE
 *   ├── FolderAnimationSpringBuilderManager.kt  — wires data + AnimatorSet construction
 *   ├── FolderOpenCloseAnimationListener.kt     — handles state on animation start/end
 *   ├── FolderScrimAnimationListener.kt         — scrim alpha listener
 *   ├── FolderSpringAnimatorSet.kt              — owns the AnimatorSet built per open/close
 *   └── IconAnimationData.kt                    — per-preview-icon animation tuple
 *
 * Purpose:
 *   Same-package Bliss helper that computes the geometry/scale tuples consumed
 *   by FolderSpringAnimatorSet for the spring-physics open/close animation.
 *   Migration04 §06 pass: the hand-rolled NaN/zero-divide guards added in
 *   Migration03 §1 are now consolidated behind FloatGuard so every animator
 *   port shares one validation policy. Behaviour is byte-identical: a
 *   non-finite or zero-denominator state still throws → Folder's
 *   AnimatorFallback catches → AOSP FolderAnimationManager runs.
 *
 * Consumed by:
 *   - com.android.launcher3.folder.FolderAnimationSpringBuilderManager
 *
 * Calls into:
 *   - foundation.e.bliss.animations.safety.FloatGuard
 *
 * Plan reference: Plans/Migration04/06-animation-safety.md §5.1
 */
// PLAN-DRIFT-M02: see ClipRevealData.kt header — same-package layout.
package com.android.launcher3.folder

import android.graphics.Rect
import android.view.View
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.android.launcher3.folder.FolderAnimationSpringBuilderManager.Companion.getBubbleTextView
import com.android.launcher3.folder.FolderAnimationSpringBuilderManager.Companion.getPreviewIconsOnPage
import com.android.launcher3.views.BaseDragLayer
import foundation.e.bliss.animations.safety.FloatGuard

/** Position and Scale values for animating opened/closed [Folder] */
data class FolderAnimationData(
    /** is folder opening or closing */
    val isOpening: Boolean,
    /** scale to set folder to */
    val startScale: Float,
    /** ratio of folder scale to drag layer scale */
    val folderScale: Float,
    /** x distance to translate folder */
    val xDistance: Float,
    /** y distance to translate folder */
    val yDistance: Float,
    /** change in content area height from scaling */
    val contentHeightDifference: Float,
    /** change in preview background radius from scaling */
    val folderRadiusDifference: Int,
    /** initial scale of folder icon before animation */
    val initialFolderScale: Float,
    /** initial size of preview background before animation */
    val initialFolderSize: Float,
    /** initial x offset of folder content */
    val previewOffsetX: Int,
    /** scaled offset X for folder preview */
    val scaledPreviewOffsetX: Int,
    /** initial y padding of folder content */
    val contentOffsetY: Int,
    /** default duration */
    val defaultDuration: Int,
) {

    companion object Factory {
        fun Folder.getAnimationData(isOpening: Boolean): FolderAnimationData {
            /** Calculates all values required for Folder Animators. */
            // Position and Scale values
            val layoutParams = layoutParams as BaseDragLayer.LayoutParams
            val previewBackground = mFolderIcon.mBackground

            // Get items in Preview and their scaling
            val itemsInPreview: List<View> = getPreviewIconsOnPage(this, 0)
            // PLAN-DRIFT-M02: Bliss ClippedFolderIconLayoutRule has only scaleForItem(int);
            // a 2-arg overload was added to the AOSP file specifically for the spring path.
            val previewScale: Float = mFolderIcon.layoutRule.scaleForItem(itemsInPreview.size, 0)
            val previewSize: Float = mFolderIcon.layoutRule.iconSize * previewScale

            // Get scale and position of FolderIcon relative to DragLayer
            val folderIconWorkspacePosition = Rect()
            val scaleRelativeToDragLayer: Float =
                mActivityContext.dragLayer.getDescendantRectRelativeToSelf(
                    mFolderIcon,
                    folderIconWorkspacePosition,
                )
            val scaledFolderRadius: Int = previewBackground.scaledRadius
            val baseIconSize: Float = getBubbleTextView(itemsInPreview[0]).iconSize.toFloat()
            val initialFolderSize = (scaledFolderRadius * 2) * scaleRelativeToDragLayer
            // Guard against pre-measure / partially-loaded folders. FloatGuard.safeDivide
            // covers baseIconSize <= 0 / NaN (debug: throws; release: logs + fallback 1f).
            // requireAllFinite covers scaleRelativeToDragLayer; if non-finite or non-positive,
            // throw so Folder.buildOpenCloseAnimator's AnimatorFallback catches it and runs
            // the AOSP FolderAnimationManager. (Migration04 §06 — was an inline check block.)
            val initialFolderScale =
                FloatGuard.safeDivide(
                    "FolderAnimationData.initialFolderScale",
                    previewSize,
                    baseIconSize,
                    fallback = 1f,
                ) * scaleRelativeToDragLayer
            check(
                FloatGuard.requireAllFinite(
                    "FolderAnimationData.scaleRelativeToDragLayer",
                    scaleRelativeToDragLayer,
                ) && scaleRelativeToDragLayer > 0f
            ) {
                "FolderAnimationData not ready: baseIconSize=$baseIconSize " +
                    "scaleRelativeToDragLayer=$scaleRelativeToDragLayer"
            }

            // Get offsets for Previews and Content
            val initialPreviewItemOffsetX =
                if (Utilities.isRtl(context.resources)) {
                    (layoutParams.width * initialFolderScale - initialFolderSize).toInt()
                } else 0
            val contentOffsetX = (content.paddingLeft * initialFolderScale).toInt()
            val contentOffsetY = (content.paddingTop * initialFolderScale).toInt()

            // Get initial position of folder
            val initialX =
                ((folderIconWorkspacePosition.left +
                    paddingLeft +
                    Math.round(previewBackground.offsetX * scaleRelativeToDragLayer)) -
                    contentOffsetX -
                    initialPreviewItemOffsetX)
            val initialY =
                ((folderIconWorkspacePosition.top +
                    paddingTop +
                    Math.round(previewBackground.offsetY * scaleRelativeToDragLayer)) -
                    contentOffsetY)

            // Get scaled height of content and radius of background
            val scaledContentHeight = contentAreaHeight.toFloat() * initialFolderScale
            val contentHeightDifference = contentAreaHeight.toFloat() - scaledContentHeight
            val folderRadiusDifference = previewBackground.scaledRadius - previewBackground.radius
            /**
             * Background can have a scaled radius in drag and drop mode, so we need to add the
             * difference to keep the preview items centered.
             */
            val data =
                FolderAnimationData(
                    isOpening = isOpening,
                    startScale = if (isOpening) initialFolderScale else 1f,
                    folderScale = initialFolderScale / scaleRelativeToDragLayer,
                    xDistance = (initialX - layoutParams.x).toFloat(),
                    yDistance = (initialY - layoutParams.y).toFloat(),
                    contentHeightDifference = contentHeightDifference,
                    folderRadiusDifference = folderRadiusDifference,
                    initialFolderScale = initialFolderScale,
                    initialFolderSize = initialFolderSize,
                    previewOffsetX = initialPreviewItemOffsetX + contentOffsetX,
                    scaledPreviewOffsetX =
                        (initialPreviewItemOffsetX / scaleRelativeToDragLayer).toInt() +
                            folderRadiusDifference,
                    contentOffsetY = contentOffsetY,
                    defaultDuration =
                        content.resources.getInteger(R.integer.config_materialFolderExpandDuration),
                )
            // Final NaN/Inf scan via FloatGuard — anything non-finite would propagate
            // through SpringAnimationBuilder.getInterpolatedValue into setScaleX(NaN) /
            // setTranslationX(NaN) and crash the launcher. Throwing triggers AnimatorFallback.
            check(
                FloatGuard.requireAllFinite(
                    "FolderAnimationData.output",
                    data.startScale,
                    data.folderScale,
                    data.xDistance,
                    data.yDistance,
                    data.contentHeightDifference,
                    data.initialFolderScale,
                    data.initialFolderSize,
                )
            ) {
                "FolderAnimationData has non-finite value: $data"
            }
            return data
        }
    }
}
