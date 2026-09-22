/*
 * Copyright (C) 2026 The LegacyDroid Project
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
package com.android.launcher3.anim;

import static com.android.launcher3.LauncherAnimUtils.SCALE_PROPERTY;
import static com.android.launcher3.LauncherAnimUtils.VIEW_ALPHA;
import static com.android.launcher3.LauncherAnimUtils.VIEW_TRANSLATE_X;
import static com.android.launcher3.LauncherAnimUtils.VIEW_TRANSLATE_Y;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.KeyguardManager;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.util.FloatProperty;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

import com.android.launcher3.CellLayout;
import com.android.launcher3.Hotseat;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.Workspace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Unlock fan-in for the workspace, triggered by ACTION_USER_PRESENT.
 *
 * Screen off (or resume behind keyguard, for the first unlock after boot) wipes icons and
 * dock. Unlock fans them back in: icons fly from collapsed center with fade and blur,
 * staggered by distance; dock slides up as one unit.
 */
public class UnlockIconAnimator {

    private static final long MOVE_DURATION = 750;
    private static final long FADE_DURATION = 500;
    private static final long DOCK_DURATION = 700;
    private static final long DOCK_FADE_DURATION = 400;
    private static final long DOCK_DELAY = 120;

    /** Share of the distance to the center the icons travel back (start at the remaining 0.15). */
    private static final float CENTER_COLLAPSE = 0.85f;
    private static final float START_SCALE = 0.35f;
    /** Stagger delay per cell of distance from the center. */
    private static final float STAGGER_PER_CELL_MS = 25f;
    private static final float BLUR_RADIUS_DP = 6f;

    private static final Interpolator MOVE_INTERP = new PathInterpolator(0.16f, 1f, 0.3f, 1f);
    private static final Interpolator EASE_INTERP = new PathInterpolator(0.25f, 0.1f, 0.25f, 1f);
    private static final Interpolator DOCK_INTERP =
            new PathInterpolator(0.175f, 0.885f, 0.32f, 1.15f);

    /** Hotseat.setTranslationY() drops the value, so drive the slide via forced translation. */
    private static final FloatProperty<Hotseat> DOCK_TRANSLATE_Y =
            new FloatProperty<Hotseat>("unlockDockTranslateY") {
                @Override
                public void setValue(Hotseat hotseat, float value) {
                    hotseat.setForcedTranslationXY(hotseat.getTranslationX(), value);
                }

                @Override
                public Float get(Hotseat hotseat) {
                    return hotseat.getTranslationY();
                }
            };

    private final Launcher mLauncher;
    private final List<AnimatorSet> mRunning = new ArrayList<>();
    private final Set<View> mBlurred =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private ViewTreeObserver.OnPreDrawListener mEnforceHideListener;
    private boolean mPendingPlay;
    private boolean mArmed;

    /** Re-hides on every pre-draw until unlock, catching items that bind late. */
    private void startEnforceHide() {
        if (mEnforceHideListener != null) {
            return;
        }
        View root = mLauncher.getRootView();
        if (root == null) {
            return;
        }
        mEnforceHideListener = () -> {
            if (mArmed) {
                hideAll();
            }
            return true;
        };
        root.getViewTreeObserver().addOnPreDrawListener(mEnforceHideListener);
    }

    private void stopEnforceHide() {
        if (mEnforceHideListener == null) {
            return;
        }
        View root = mLauncher.getRootView();
        if (root != null) {
            root.getViewTreeObserver().removeOnPreDrawListener(mEnforceHideListener);
        }
        mEnforceHideListener = null;
    }

    public UnlockIconAnimator(Launcher launcher) {
        mLauncher = launcher;
    }

    /** Screen off: wipe icons and dock so unlock can fan them in. */
    public void onScreenOff() {
        mPendingPlay = false;
        resetRunning();
        arm();
        startEnforceHide();
    }

    /** Called on ACTION_USER_PRESENT. */
    public void onUserPresent() {
        if (canPlayNow()) {
            play();
        } else if (!mLauncher.hasBeenResumed() && mLauncher.isStarted()) {
            // Not resumed yet; play on next resume.
            mPendingPlay = true;
        } else {
            // Cannot animate now; show normal state instead of staying hidden.
            disarmAndRestore();
        }
    }

    /** Called from {@link Launcher#onDeferredResumed()}. */
    public void onDeferredResumed() {
        if (mPendingPlay) {
            mPendingPlay = false;
            if (canPlayNow()) {
                play();
            } else {
                disarmAndRestore();
            }
            return;
        }
        if (isKeyguardLocked()) {
            // Cold start behind keyguard: no screen-off was seen, arm here.
            arm();
            startEnforceHide();
            return;
        }
        if (mArmed) {
            // Woke straight to home without keyguard: that wake is the unlock.
            if (canPlayNow()) {
                play();
            } else {
                disarmAndRestore();
            }
        }
    }

    private void arm() {
        if (!mArmed) {
            mArmed = true;
            hideAll();
        }
    }

    private void disarmAndRestore() {
        mArmed = false;
        mPendingPlay = false;
        restoreAll();
        stopEnforceHide();
    }

    private boolean canPlayNow() {
        return mLauncher.hasBeenResumed()
                && mLauncher.isInState(LauncherState.NORMAL)
                && !mLauncher.getDragController().isDragging();
    }

    private boolean isKeyguardLocked() {
        KeyguardManager keyguardManager = mLauncher.getSystemService(KeyguardManager.class);
        return keyguardManager != null && keyguardManager.isKeyguardLocked();
    }

    /** Hides every workspace icon on every page plus the dock unit. */
    private void hideAll() {
        Workspace<?> workspace = mLauncher.getWorkspace();
        if (workspace != null) {
            for (int pageIndex = 0; pageIndex < workspace.getChildCount(); pageIndex++) {
                View page = workspace.getChildAt(pageIndex);
                if (!(page instanceof CellLayout)) {
                    continue;
                }
                CellLayout layout = (CellLayout) page;
                ShortcutAndWidgetContainer container = layout.getShortcutsAndWidgets();
                for (int i = 0; i < container.getChildCount(); i++) {
                    View icon = container.getChildAt(i);
                    if (container.getWidth() == 0 || icon.getWidth() == 0) {
                        icon.setAlpha(0f);
                        continue;
                    }
                    applyFanOutStartState(icon,
                            computeCollapseX(container, icon),
                            computeCollapseY(container, icon));
                }
            }
        }

        Hotseat hotseat = mLauncher.getHotseat();
        if (hotseat != null) {
            if (hotseat.getAlpha() != 0f) {
                hotseat.setAlpha(0f);
            }
            if (hotseat.getHeight() > 0 && hotseat.getTranslationY() != hotseat.getHeight()) {
                hotseat.setForcedTranslationXY(hotseat.getTranslationX(), hotseat.getHeight());
            }
        }
    }

    /** Restores every view touched by hideAll()/play(). */
    private void restoreAll() {
        Workspace<?> workspace = mLauncher.getWorkspace();
        if (workspace != null) {
            for (int pageIndex = 0; pageIndex < workspace.getChildCount(); pageIndex++) {
                View page = workspace.getChildAt(pageIndex);
                if (!(page instanceof CellLayout)) {
                    continue;
                }
                ShortcutAndWidgetContainer container =
                        ((CellLayout) page).getShortcutsAndWidgets();
                for (int i = 0; i < container.getChildCount(); i++) {
                    restoreView(container.getChildAt(i));
                }
            }
        }

        Hotseat hotseat = mLauncher.getHotseat();
        if (hotseat != null) {
            hotseat.setAlpha(1f);
            hotseat.setForcedTranslationXY(hotseat.getTranslationX(), 0f);
        }

        mBlurred.clear();
    }

    private void play() {
        mArmed = false;
        mPendingPlay = false;
        stopEnforceHide();
        resetRunning();
        restoreAll();

        Workspace<?> workspace = mLauncher.getWorkspace();
        if (workspace == null || workspace.getWidth() == 0) {
            return;
        }
        int currentPage = workspace.getCurrentPage();
        if (currentPage < 0 || currentPage >= workspace.getChildCount()) {
            return;
        }
        View page = workspace.getChildAt(currentPage);
        if (!(page instanceof CellLayout)) {
            return;
        }

        Hotseat hotseat = mLauncher.getHotseat();
        if (hotseat != null && hotseat.getHeight() > 0) {
            animateDock(hotseat);
        }

        CellLayout layout = (CellLayout) page;
        ShortcutAndWidgetContainer container = layout.getShortcutsAndWidgets();
        if (container.getWidth() == 0 || container.getHeight() == 0) {
            return;
        }

        for (int i = 0; i < container.getChildCount(); i++) {
            View icon = container.getChildAt(i);
            if (icon.getWidth() == 0) {
                continue;
            }
            float dx = computeCollapseX(container, icon);
            float dy = computeCollapseY(container, icon);
            float distCells = distanceInCells(layout, container, icon);
            animateFanOut(icon, dx, dy, (long) (distCells * STAGGER_PER_CELL_MS));
        }
    }

    private float pageCenterX(ShortcutAndWidgetContainer container) {
        return container.getLeft() + container.getWidth() / 2f;
    }

    private float pageCenterY(ShortcutAndWidgetContainer container) {
        return container.getTop() + container.getHeight() / 2f;
    }

    private float iconCenterX(ShortcutAndWidgetContainer container, View icon) {
        return container.getLeft() + icon.getLeft() + icon.getWidth() / 2f;
    }

    private float iconCenterY(ShortcutAndWidgetContainer container, View icon) {
        return container.getTop() + icon.getTop() + icon.getHeight() / 2f;
    }

    private float computeCollapseX(ShortcutAndWidgetContainer container, View icon) {
        return -(iconCenterX(container, icon) - pageCenterX(container)) * CENTER_COLLAPSE;
    }

    private float computeCollapseY(ShortcutAndWidgetContainer container, View icon) {
        return -(iconCenterY(container, icon) - pageCenterY(container)) * CENTER_COLLAPSE;
    }

    private float distanceInCells(CellLayout layout, ShortcutAndWidgetContainer container,
            View icon) {
        float pitchX = layout.getCountX() > 0
                ? (float) container.getWidth() / layout.getCountX() : container.getWidth();
        float pitchY = layout.getCountY() > 0
                ? (float) container.getHeight() / layout.getCountY() : container.getHeight();
        float dxCells = (iconCenterX(container, icon) - pageCenterX(container)) / pitchX;
        float dyCells = (iconCenterY(container, icon) - pageCenterY(container)) / pitchY;
        return (float) Math.hypot(dxCells, dyCells);
    }

    private void applyFanOutStartState(View icon, float dx, float dy) {
        icon.setAlpha(0f);
        icon.setScaleX(START_SCALE);
        icon.setScaleY(START_SCALE);
        icon.setTranslationX(dx);
        icon.setTranslationY(dy);
        // This View has no getRenderEffect(); track applied blur ourselves.
        if (Build.VERSION.SDK_INT >= 31 && mBlurred.add(icon)) {
            setBlur(icon, BLUR_RADIUS_DP);
        }
    }

    private void animateFanOut(View icon, float dx, float dy, long delay) {
        applyFanOutStartState(icon, dx, dy);

        ObjectAnimator moveX = ObjectAnimator.ofFloat(icon, VIEW_TRANSLATE_X, dx, 0f);
        ObjectAnimator moveY = ObjectAnimator.ofFloat(icon, VIEW_TRANSLATE_Y, dy, 0f);
        ObjectAnimator scale = ObjectAnimator.ofFloat(icon, SCALE_PROPERTY, START_SCALE, 1f);
        moveX.setDuration(MOVE_DURATION);
        moveY.setDuration(MOVE_DURATION);
        scale.setDuration(MOVE_DURATION);
        moveX.setInterpolator(MOVE_INTERP);
        moveY.setInterpolator(MOVE_INTERP);
        scale.setInterpolator(MOVE_INTERP);

        ObjectAnimator fade = ObjectAnimator.ofFloat(icon, VIEW_ALPHA, 0f, 1f);
        fade.setDuration(FADE_DURATION);
        fade.setInterpolator(EASE_INTERP);

        List<Animator> animators = new ArrayList<>();
        animators.add(moveX);
        animators.add(moveY);
        animators.add(scale);
        animators.add(fade);
        if (Build.VERSION.SDK_INT >= 31) {
            animators.add(createBlurAnimator(icon));
        }

        AnimatorSet set = new AnimatorSet();
        set.playTogether(animators);
        set.setStartDelay(delay);
        track(set, () -> restoreView(icon));
    }

    /** Slides dock unit (pill + icons) up from below the screen edge. */
    private void animateDock(Hotseat hotseat) {
        float dy = hotseat.getHeight();
        hotseat.setAlpha(0f);
        hotseat.setForcedTranslationXY(hotseat.getTranslationX(), dy);

        ObjectAnimator slide = ObjectAnimator.ofFloat(hotseat, DOCK_TRANSLATE_Y, dy, 0f);
        slide.setDuration(DOCK_DURATION);
        slide.setInterpolator(DOCK_INTERP);

        ObjectAnimator fade = ObjectAnimator.ofFloat(hotseat, VIEW_ALPHA, 0f, 1f);
        fade.setDuration(DOCK_FADE_DURATION);
        fade.setInterpolator(EASE_INTERP);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(slide, fade);
        set.setStartDelay(DOCK_DELAY);
        track(set, () -> {
            hotseat.setAlpha(1f);
            hotseat.setForcedTranslationXY(hotseat.getTranslationX(), 0f);
        });
    }

    private Animator createBlurAnimator(View icon) {
        ValueAnimator blur = ValueAnimator.ofFloat(BLUR_RADIUS_DP, 0f);
        blur.setDuration(FADE_DURATION);
        blur.setInterpolator(EASE_INTERP);
        blur.addUpdateListener(a -> setBlur(icon, (Float) a.getAnimatedValue()));
        return blur;
    }

    private void track(AnimatorSet set, Runnable onEnd) {
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mRunning.remove(set);
                onEnd.run();
            }
        });
        mRunning.add(set);
        set.start();
    }

    private void resetRunning() {
        if (mRunning.isEmpty()) {
            return;
        }
        List<AnimatorSet> running = new ArrayList<>(mRunning);
        mRunning.clear();
        for (AnimatorSet set : running) {
            // Cancel runs listeners synchronously, restoring touched views.
            set.cancel();
        }
    }

    private void restoreView(View icon) {
        icon.setAlpha(1f);
        icon.setScaleX(1f);
        icon.setScaleY(1f);
        icon.setTranslationX(0f);
        icon.setTranslationY(0f);
        mBlurred.remove(icon);
        setBlur(icon, 0f);
    }

    private void setBlur(View icon, float radiusDp) {
        if (Build.VERSION.SDK_INT < 31) {
            return;
        }
        if (radiusDp <= 0f) {
            icon.setRenderEffect(null);
        } else {
            float radiusPx = radiusDp * icon.getResources().getDisplayMetrics().density;
            icon.setRenderEffect(
                    RenderEffect.createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP));
        }
    }
}
