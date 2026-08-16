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

package com.android.quickstep.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.widget.Button;

public class ClearAllButton extends Button {

    public static final FloatProperty<ClearAllButton> VISIBILITY_ALPHA =
            new FloatProperty<ClearAllButton>("visibilityAlpha") {
                @Override
                public Float get(ClearAllButton view) {
                    return view.mVisibilityAlpha;
                }

                @Override
                public void setValue(ClearAllButton view, float v) {
                    view.setVisibilityAlpha(v);
                }
            };

    public static final FloatProperty<ClearAllButton> DISMISS_ALPHA =
            new FloatProperty<ClearAllButton>("dismissAlpha") {
                @Override
                public Float get(ClearAllButton view) {
                    return view.mDismissAlpha;
                }

                @Override
                public void setValue(ClearAllButton view, float v) {
                    view.setDismissAlpha(v);
                }
            };

    private float mContentAlpha = 1;
    private float mVisibilityAlpha = 1;
    private float mDismissAlpha = 1;

    public ClearAllButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void setContentAlpha(float alpha) {
        if (mContentAlpha != alpha) {
            mContentAlpha = alpha;
            updateAlpha();
        }
    }

    public void setVisibilityAlpha(float alpha) {
        if (mVisibilityAlpha != alpha) {
            mVisibilityAlpha = alpha;
            updateAlpha();
        }
    }

    public void setDismissAlpha(float alpha) {
        if (mDismissAlpha != alpha) {
            mDismissAlpha = alpha;
            updateAlpha();
        }
    }

    private void updateAlpha() {
        final float alpha = mContentAlpha * mVisibilityAlpha * mDismissAlpha;
        setAlpha(alpha);
        setClickable(Math.min(alpha, 1) == 1);
    }
}
