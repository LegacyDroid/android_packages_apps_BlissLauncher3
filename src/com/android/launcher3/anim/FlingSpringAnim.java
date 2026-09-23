/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.content.Context;

import androidx.dynamicanimation.animation.DynamicAnimation.OnAnimationEndListener;
import androidx.dynamicanimation.animation.FlingAnimation;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.android.launcher3.R;
import com.android.launcher3.util.DynamicResource;
import com.android.systemui.plugins.ResourceProvider;

/**
 * Given a property to animate and a target value and starting velocity, first apply friction to
 * the fling until we pass the target, then apply a spring force to pull towards the target.
 */
public class FlingSpringAnim {

    /** Builds a fresh spring around the same property; reversal needs this to revive a dead one. */
    private interface SpringBuilder {
        SpringAnimation create(float startValue, float startVelocity, float target);
    }

    /** Reads the property's live position; DynamicAnimation exposes no getValue(). */
    private interface ValueReader {
        float read();
    }

    private final FlingAnimation mFlingAnim;
    private final SpringBuilder mSpringBuilder;
    private final ValueReader mValueReader;
    private final OnAnimationEndListener mOnEndListener;
    private SpringAnimation mSpringAnim;
    private final boolean mSkipFlingAnim;

    private float mTargetPosition;

    public <K> FlingSpringAnim(K object, Context context, FloatPropertyCompat<K> property,
            float startPosition, float targetPosition, float startVelocityPxPerS,
            float minVisChange, float minValue, float maxValue, float damping, float stiffness,
            OnAnimationEndListener onEndListener) {
        ResourceProvider rp = DynamicResource.provider(context);
        float friction = rp.getFloat(R.dimen.swipe_up_rect_xy_fling_friction);

        mFlingAnim = new FlingAnimation(object, property)
                .setFriction(friction)
                // Have the spring pull towards the target if we've slowed down too much before
                // reaching it.
                .setMinimumVisibleChange(minVisChange)
                .setStartVelocity(startVelocityPxPerS)
                .setMinValue(minValue)
                .setMaxValue(maxValue);
        mTargetPosition = targetPosition;
        mOnEndListener = onEndListener;
        mValueReader = () -> property.getValue(object);
        mSpringBuilder = (value, velocity, target) -> new SpringAnimation(object, property)
                .setStartValue(value)
                .setStartVelocity(velocity)
                .setSpring(new SpringForce(target)
                        .setStiffness(stiffness)
                        .setDampingRatio(damping));

        // We are already past the fling target, so skip it to avoid losing a frame of the spring.
        mSkipFlingAnim = startPosition <= minValue && startVelocityPxPerS < 0
                || startPosition >= maxValue && startVelocityPxPerS > 0;

        mFlingAnim.addEndListener(((animation, canceled, value, velocity) -> {
            mSpringAnim = mSpringBuilder.create(value, velocity, mTargetPosition);
            mSpringAnim.addEndListener(mOnEndListener);
            mSpringAnim.animateToFinalPosition(mTargetPosition);
        }));
    }

    public float getTargetPosition() {
        return mTargetPosition;
    }

    public void updatePosition(float startPosition, float targetPosition) {
        mFlingAnim.setMinValue(Math.min(startPosition, targetPosition))
                .setMaxValue(Math.max(startPosition, targetPosition));
        mTargetPosition = targetPosition;
        if (mSpringAnim != null) {
            mSpringAnim.animateToFinalPosition(mTargetPosition);
        }
    }

    /**
     * Retargets even when the spring already settled: an ended androidx spring never restarts,
     * so the dead one is rebuilt from its last value while a live one just turns around.
     */
    public void restartTo(float startPosition, float targetPosition) {
        mTargetPosition = targetPosition;
        mFlingAnim.setMinValue(Math.min(startPosition, targetPosition))
                .setMaxValue(Math.max(startPosition, targetPosition));
        // Ends a live fling and hands its momentum to the spring; a settled fling does nothing.
        mFlingAnim.cancel();
        if (mSpringAnim != null && mSpringAnim.isRunning()) {
            mSpringAnim.animateToFinalPosition(targetPosition);
            return;
        }
        // Only reached once the spring settled, so momentum is gone: rebuild from the
        // property's current position at rest.
        mSpringAnim = mSpringBuilder.create(mValueReader.read(), 0f, targetPosition);
        mSpringAnim.addEndListener(mOnEndListener);
        mSpringAnim.animateToFinalPosition(targetPosition);
    }

    public void start() {
        mFlingAnim.start();
        if (mSkipFlingAnim) {
            mFlingAnim.cancel();
        }
    }

    public void end() {
        mFlingAnim.cancel();
        if (mSpringAnim.canSkipToEnd()) {
            mSpringAnim.skipToEnd();
        }
    }
}
