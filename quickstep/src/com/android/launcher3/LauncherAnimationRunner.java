/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.launcher3;

import static com.android.launcher3.Utilities.postAsyncCallback;
import static com.android.launcher3.util.Executors.MAIN_EXECUTOR;
import static com.android.launcher3.util.Executors.UI_HELPER_EXECUTOR;
import static com.android.launcher3.util.window.RefreshRateTracker.getSingleFrameMs;
import static com.android.systemui.shared.recents.utilities.Utilities.postAtFrontOfQueueAsynchronously;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.Context;
import android.os.Handler;
import android.os.RemoteException;
import android.view.IRemoteAnimationFinishedCallback;
import android.view.RemoteAnimationTarget;
import android.window.TransitionInfo;

import androidx.annotation.BinderThread;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.android.systemui.animation.RemoteAnimationDelegate;
import com.android.systemui.animation.RemoteAnimationRunnerCompat;

import java.lang.ref.WeakReference;

/**
 * This class is needed to wrap any animation runner that is a part of the
 * RemoteAnimationDefinition:
 * - Launcher creates a new instance of the LauncherAppTransitionManagerImpl whenever it is
 *   created, which in turn registers a new definition
 * - When the definition is registered, window manager retains a strong binder reference to the
 *   runner passed in
 * - If the Launcher activity is recreated, the new definition registered will replace the old
 *   reference in the system's activity record, but until the system server is GC'd, the binder
 *   reference will still exist, which references the runner in the Launcher process, which
 *   references the (old) Launcher activity through this class
 *
 * Instead we make the runner provided to the definition static only holding a weak reference to
 * the runner implementation.  When this animation manager is destroyed, we remove the Launcher
 * reference to the runner, leaving only the weak ref from the runner.
 */
public class LauncherAnimationRunner extends RemoteAnimationRunnerCompat {

    private static final RemoteAnimationFactory DEFAULT_FACTORY =
            (transit, appTargets, wallpaperTargets, nonAppTargets, result) ->
                    result.setAnimation(null, null);

    private final Handler mHandler;
    private final boolean mStartAtFrontOfQueue;
    private final WeakReference<RemoteAnimationFactory> mFactory;

    private volatile AnimationResult mAnimationResult;

    /**
     * @param startAtFrontOfQueue If true, the animation start will be posted at the front of the
     *                            queue to minimize latency.
     */
    public LauncherAnimationRunner(Handler handler, RemoteAnimationFactory factory,
            boolean startAtFrontOfQueue) {
        mHandler = handler;
        mFactory = new WeakReference<>(factory);
        mStartAtFrontOfQueue = startAtFrontOfQueue;
    }

    // Called only in S+ platform
    @BinderThread
    public void onAnimationStart(
            int transit,
            RemoteAnimationTarget[] appTargets,
            RemoteAnimationTarget[] wallpaperTargets,
            RemoteAnimationTarget[] nonAppTargets,
            Runnable runnable) {
        Runnable r = () -> {
            finishExistingAnimation();
            mAnimationResult = new AnimationResult(() -> mAnimationResult = null, runnable);
            getFactory().onAnimationStart(transit, appTargets, wallpaperTargets, nonAppTargets,
                    mAnimationResult);
        };
        if (mStartAtFrontOfQueue) {
            postAtFrontOfQueueAsynchronously(mHandler, r);
        } else {
            postAsyncCallback(mHandler, r);
        }
    }

    private RemoteAnimationFactory getFactory() {
        RemoteAnimationFactory factory = mFactory.get();
        return factory != null ? factory : DEFAULT_FACTORY;
    }

    /**
     * System calls this when another transition wants to merge into the animation we are
     * playing. Accepting keeps it alive to reverse in place; cancelling would replay it
     * from scratch.
     */
    @BinderThread
    @Override
    public boolean onAnimationMerge(TransitionInfo info) {
        final AnimationResult result = mAnimationResult;
        // Hold before the predicate: an animator ending during the check must not finish
        // the original transition before an accepted reversal takes over.
        if (result == null || !result.holdFinish()) {
            return false;
        }
        if (getFactory().onAnimationMerge(info, result)) {
            return true;
        }
        // Declined: drop the hold; releaseHold is @UiThread so it must be posted.
        mHandler.post(result::releaseHold);
        return false;
    }

    @UiThread
    private void finishExistingAnimation() {
        if (mAnimationResult != null) {
            // A replacement animation must land even if a merge reversal holds the finish open.
            mAnimationResult.releaseHold();
            mAnimationResult.finish();
            mAnimationResult = null;
        }
    }

    /**
     * Called by the system
     */
    @BinderThread
    @Override
    public void onAnimationCancelled() {
        postAsyncCallback(mHandler, () -> {
            finishExistingAnimation();
            getFactory().onAnimationCancelled();
        });
    }

    /**
     * Used by RemoteAnimationFactory implementations to run the actual animation and its lifecycle
     * callbacks.
     */
    public static final class AnimationResult extends IRemoteAnimationFinishedCallback.Stub {

        private final Runnable mSyncFinishRunnable;
        private final Runnable mASyncFinishRunnable;

        private AnimatorSet mAnimator;
        private Runnable mOnCompleteCallback;
        private boolean mFinished = false;
        private boolean mInitialized = false;
        // Set on the binder thread at merge accept, cleared on UI when the reversal ends.
        private volatile boolean mHoldFinish;
        private boolean mFinishWhileHeld;
        // Serializes holdFinish (binder) against finish (UI) so a finish either lands
        // completely before the hold or is deferred by it, never slips between.
        private final Object mFinishLock = new Object();

        private AnimationResult(Runnable syncFinishRunnable, Runnable asyncFinishRunnable) {
            mSyncFinishRunnable = syncFinishRunnable;
            mASyncFinishRunnable = asyncFinishRunnable;
        }

        /**
         * Defers the transition finish on the binder thread so shell keeps the original
         * transition and its leashes alive while a merge reversal runs on them. Returns
         * false when the result already finished, which must not be held back.
         */
        public boolean holdFinish() {
            synchronized (mFinishLock) {
                if (mFinished) {
                    return false;
                }
                mHoldFinish = true;
                return true;
            }
        }

        /** Ends a hold; runs the finish that landed while held, if any. */
        @UiThread
        public void releaseHold() {
            boolean pending;
            synchronized (mFinishLock) {
                if (!mHoldFinish) {
                    return;
                }
                mHoldFinish = false;
                pending = mFinishWhileHeld;
                mFinishWhileHeld = false;
            }
            if (pending) {
                finish();
            }
        }

        @UiThread
        private void finish() {
            synchronized (mFinishLock) {
                if (mHoldFinish) {
                    mFinishWhileHeld = true;
                    return;
                }
                if (mFinished) {
                    return;
                }
                // Flagged before callbacks run so a racing binder holdFinish finds the
                // result already finished and declines.
                mFinished = true;
            }
            mSyncFinishRunnable.run();
            UI_HELPER_EXECUTOR.execute(() -> {
                mASyncFinishRunnable.run();
                if (mOnCompleteCallback != null) {
                    MAIN_EXECUTOR.execute(mOnCompleteCallback);
                }
            });
        }

        @UiThread
        public void setAnimation(AnimatorSet animation, Context context) {
            setAnimation(animation, context, null, true);
        }

        /**
         * Sets the animation to play for this app launch
         * @param skipFirstFrame Iff true, we skip the first frame of the animation.
         *                       We set to false when skipping first frame causes jank.
         */
        @UiThread
        public void setAnimation(AnimatorSet animation, Context context,
                @Nullable Runnable onCompleteCallback, boolean skipFirstFrame) {
            if (mInitialized) {
                throw new IllegalStateException("Animation already initialized");
            }
            mInitialized = true;
            mAnimator = animation;
            mOnCompleteCallback = onCompleteCallback;
            if (mAnimator == null) {
                finish();
            } else if (mFinished) {
                // Animation callback was already finished, skip the animation.
                mAnimator.start();
                mAnimator.end();
                if (mOnCompleteCallback != null) {
                    mOnCompleteCallback.run();
                }
            } else {
                // Start the animation
                mAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        finish();
                    }
                });
                if (skipFirstFrame) {
                    // Because t=0 has the app icon in its original spot, we can skip the
                    // first frame and have the same movement one frame earlier.
                    mAnimator.setCurrentPlayTime(
                            Math.min(getSingleFrameMs(context), mAnimator.getTotalDuration()));
                }
                mAnimator.start();
            }
        }

        /**
         * When used as a simple IRemoteAnimationFinishedCallback, this method is used to run the
         * animation finished runnable.
         */
        @Override
        public void onAnimationFinished() throws RemoteException {
            mASyncFinishRunnable.run();
        }
    }

    /**
     * Used with LauncherAnimationRunner as an interface for the runner to call back to the
     * implementation.
     */
    public interface RemoteAnimationFactory extends RemoteAnimationDelegate<AnimationResult> {

        /**
         * Called on the UI thread when the animation targets are received. The implementation must
         * call {@link AnimationResult#setAnimation} with the target animation to be run.
         */
        @Override
        @UiThread
        void onAnimationStart(int transit,
                RemoteAnimationTarget[] appTargets,
                RemoteAnimationTarget[] wallpaperTargets,
                RemoteAnimationTarget[] nonAppTargets,
                LauncherAnimationRunner.AnimationResult result);

        /**
         * Called when the animation is cancelled. This can happen with or without
         * the create being called.
         */
        @Override
        @UiThread
        default void onAnimationCancelled() {}

        /**
         * Runs on a binder thread when a transition wants to merge into the animation this
         * runner is playing. Return true to take over: shell applies the merged start state
         * and finishes the merged transition while the running animation reverses toward
         * the new state. Implementations must not block; post the reversal to the UI thread.
         */
        @BinderThread
        default boolean onAnimationMerge(TransitionInfo info, AnimationResult result) {
            return false;
        }
    }
}
