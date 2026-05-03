/*
 * Copyright (C) 2017 The Android Open Source Project
 * Copyright (C) 2024 Lawnchair developers (Apache-2.0)
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
package foundation.e.bliss.icons;

import android.content.pm.ActivityInfo.Config;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.graphics.PathParser;

/**
 * AdaptiveIconDrawable that uses a launcher-controlled mask path
 * ({@link #sMask}) instead of the system mask. Wrap any drawable via
 * {@link #wrap(Drawable)} to render it with the user-selected icon shape.
 */
public class CustomAdaptiveIconDrawable extends AdaptiveIconDrawable implements Drawable.Callback {

    public static final float MASK_SIZE = 100f;

    private static final float EXTRA_INSET_PERCENTAGE = 1 / 4f;
    private static final float DEFAULT_VIEW_PORT_SCALE = 1f / (1 + 2 * EXTRA_INSET_PERCENTAGE);

    private static final String MASK_PATH = "M50 0C77.6 0 100 22.4 100 50C100 77.6 77.6 100 50 100C22.4 100 0 77.6 0 50C0 22.4 22.4 0 50 0Z";

    public static boolean sInitialized = false;
    public static String sMaskId = "";
    public static Path sMask;

    private final Path mMask;
    private final Path mMaskScaleOnly;
    private final Matrix mMaskMatrix;
    private final Region mTransparentRegion;

    private static final int BACKGROUND_ID = 0;
    private static final int FOREGROUND_ID = 1;
    private static final int MONOCHROME_ID = 2;

    LayerState mLayerState;

    private Shader mLayersShader;
    private Bitmap mLayersBitmap;

    private final Rect mTmpOutRect = new Rect();
    private Rect mHotspotBounds;
    private boolean mMutated;

    private boolean mSuspendChildInvalidation;
    private boolean mChildRequestedInvalidation;
    private final Canvas mCanvas;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);

    CustomAdaptiveIconDrawable() {
        this((LayerState) null, null);
    }

    CustomAdaptiveIconDrawable(@Nullable LayerState state, @Nullable Resources res) {
        super(null, null);
        if (!sInitialized) {
            Log.w("CustomAdaptiveIconDrawable", "shape not initialized; using default circle");
        }
        mLayerState = createConstantState(state, res);

        if (sMask == null) {
            sMask = PathParser.createPathFromPathData(MASK_PATH);
        }
        mMask = new Path(sMask);
        mMaskScaleOnly = new Path(mMask);
        mMaskMatrix = new Matrix();
        mCanvas = new Canvas();
        mTransparentRegion = new Region();
    }

    public static @Nullable Drawable wrap(@Nullable Drawable icon) {
        if (icon == null)
            return null;
        return wrapNonNull(icon);
    }

    public static @NonNull Drawable wrapNonNull(@NonNull Drawable icon) {
        if (icon.getClass() == AdaptiveIconDrawable.class) {
            return safeCustomAdaptiveIconDrawable((AdaptiveIconDrawable) icon);
        }
        return icon;
    }

    private ChildDrawable createChildDrawable(Drawable drawable) {
        final ChildDrawable layer = new ChildDrawable(mLayerState.mDensity);
        layer.mDrawable = drawable;
        layer.mDrawable.setCallback(this);
        mLayerState.mChildrenChangingConfigurations |= layer.mDrawable.getChangingConfigurations();
        return layer;
    }

    LayerState createConstantState(@Nullable LayerState state, @Nullable Resources res) {
        return new LayerState(state, this, res);
    }

    public CustomAdaptiveIconDrawable(Drawable backgroundDrawable, Drawable foregroundDrawable) {
        this(backgroundDrawable, foregroundDrawable, null);
    }

    public CustomAdaptiveIconDrawable(@Nullable Drawable backgroundDrawable, @Nullable Drawable foregroundDrawable,
            @Nullable Drawable monochromeDrawable) {
        this((LayerState) null, null);
        if (backgroundDrawable != null) {
            addLayer(BACKGROUND_ID, createChildDrawable(backgroundDrawable));
        }
        if (foregroundDrawable != null) {
            addLayer(FOREGROUND_ID, createChildDrawable(foregroundDrawable));
        }
        if (monochromeDrawable != null) {
            addLayer(MONOCHROME_ID, createChildDrawable(monochromeDrawable));
        }
    }

    @RequiresApi(api = VERSION_CODES.TIRAMISU)
    private CustomAdaptiveIconDrawable(AdaptiveIconDrawable drawable) {
        this(drawable.getBackground(), drawable.getForeground(), drawable.getMonochrome());
    }

    private static Drawable safeCustomAdaptiveIconDrawable(AdaptiveIconDrawable drawable) {
        if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            return new CustomAdaptiveIconDrawable(drawable);
        } else {
            return new CustomAdaptiveIconDrawable(drawable.getBackground(), drawable.getForeground(), null);
        }
    }

    private void addLayer(int index, @NonNull ChildDrawable layer) {
        mLayerState.mChildren[index] = layer;
        mLayerState.invalidateCache();
    }

    static int resolveDensity(@Nullable Resources r, int parentDensity) {
        final int densityDpi = r == null ? parentDensity : r.getDisplayMetrics().densityDpi;
        return densityDpi == 0 ? DisplayMetrics.DENSITY_DEFAULT : densityDpi;
    }

    public static float getExtraInsetFraction() {
        return EXTRA_INSET_PERCENTAGE;
    }

    public static float getExtraInsetPercentage() {
        return EXTRA_INSET_PERCENTAGE;
    }

    public Path getIconMask() {
        return mMask;
    }

    @Override
    public Drawable getForeground() {
        return mLayerState.mChildren[FOREGROUND_ID].mDrawable;
    }

    @Override
    public Drawable getBackground() {
        return mLayerState.mChildren[BACKGROUND_ID].mDrawable;
    }

    @Override
    @Nullable
    public Drawable getMonochrome() {
        return mLayerState.mChildren[MONOCHROME_ID].mDrawable;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        if (bounds.isEmpty())
            return;
        updateLayerBounds(bounds);
    }

    private void updateLayerBounds(Rect bounds) {
        if (bounds.isEmpty())
            return;
        try {
            suspendChildInvalidation();
            updateLayerBoundsInternal(bounds);
            updateMaskBoundsInternal(bounds);
        } finally {
            resumeChildInvalidation();
        }
    }

    private void updateLayerBoundsInternal(Rect bounds) {
        int cX = bounds.width() / 2;
        int cY = bounds.height() / 2;
        for (int i = 0, count = LayerState.N_CHILDREN; i < count; i++) {
            final ChildDrawable r = mLayerState.mChildren[i];
            final Drawable d = r.mDrawable;
            if (d == null)
                continue;
            int insetWidth = (int) (bounds.width() / (DEFAULT_VIEW_PORT_SCALE * 2));
            int insetHeight = (int) (bounds.height() / (DEFAULT_VIEW_PORT_SCALE * 2));
            final Rect outRect = mTmpOutRect;
            outRect.set(cX - insetWidth, cY - insetHeight, cX + insetWidth, cY + insetHeight);
            d.setBounds(outRect);
        }
    }

    private void updateMaskBoundsInternal(Rect b) {
        mMaskMatrix.setScale(b.width() / MASK_SIZE, b.height() / MASK_SIZE);
        sMask.transform(mMaskMatrix, mMaskScaleOnly);

        mMaskMatrix.postTranslate(b.left, b.top);
        sMask.transform(mMaskMatrix, mMask);

        if (mLayersBitmap == null || mLayersBitmap.getWidth() != b.width() || mLayersBitmap.getHeight() != b.height()) {
            mLayersBitmap = Bitmap.createBitmap(b.width(), b.height(), Bitmap.Config.ARGB_8888);
        }

        mPaint.setShader(null);
        mTransparentRegion.setEmpty();
        mLayersShader = null;
    }

    @Override
    public void draw(Canvas canvas) {
        if (mLayersBitmap == null)
            return;
        if (mLayersShader == null) {
            mCanvas.setBitmap(mLayersBitmap);
            mCanvas.drawColor(Color.BLACK);
            if (mLayerState.mChildren[BACKGROUND_ID].mDrawable != null) {
                mLayerState.mChildren[BACKGROUND_ID].mDrawable.draw(mCanvas);
            }
            if (mLayerState.mChildren[FOREGROUND_ID].mDrawable != null) {
                mLayerState.mChildren[FOREGROUND_ID].mDrawable.draw(mCanvas);
            }
            mLayersShader = new BitmapShader(mLayersBitmap, TileMode.CLAMP, TileMode.CLAMP);
            mPaint.setShader(mLayersShader);
        }
        if (mMaskScaleOnly != null) {
            Rect bounds = getBounds();
            canvas.translate(bounds.left, bounds.top);
            canvas.drawPath(mMaskScaleOnly, mPaint);
            canvas.translate(-bounds.left, -bounds.top);
        }
    }

    @Override
    public void invalidateSelf() {
        mLayersShader = null;
        super.invalidateSelf();
    }

    @Override
    public void getOutline(@NonNull Outline outline) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            outline.setPath(mMask);
        } else {
            outline.setConvexPath(mMask);
        }
    }

    @Override
    public @Nullable Region getTransparentRegion() {
        if (mTransparentRegion.isEmpty()) {
            mMask.toggleInverseFillType();
            mTransparentRegion.set(getBounds());
            mTransparentRegion.setPath(mMask, mTransparentRegion);
            mMask.toggleInverseFillType();
        }
        return mTransparentRegion;
    }

    @DrawableRes
    public int getSourceDrawableResId() {
        final LayerState state = mLayerState;
        return state == null ? 0 : state.mSourceDrawableId;
    }

    @Override
    public boolean canApplyTheme() {
        return (mLayerState != null && mLayerState.canApplyTheme()) || super.canApplyTheme();
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.Q)
    public boolean isProjected() {
        if (super.isProjected())
            return true;
        final ChildDrawable[] layers = mLayerState.mChildren;
        for (int i = 0; i < LayerState.N_CHILDREN; i++) {
            if (layers[i].mDrawable != null && layers[i].mDrawable.isProjected()) {
                return true;
            }
        }
        return false;
    }

    private void suspendChildInvalidation() {
        mSuspendChildInvalidation = true;
    }

    private void resumeChildInvalidation() {
        mSuspendChildInvalidation = false;
        if (mChildRequestedInvalidation) {
            mChildRequestedInvalidation = false;
            invalidateSelf();
        }
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable who) {
        if (mSuspendChildInvalidation) {
            mChildRequestedInvalidation = true;
        } else {
            invalidateSelf();
        }
    }

    @Override
    public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
        scheduleSelf(what, when);
    }

    @Override
    public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {
        unscheduleSelf(what);
    }

    @Override
    public @Config int getChangingConfigurations() {
        return super.getChangingConfigurations() | mLayerState.getChangingConfigurations();
    }

    @Override
    public void setHotspot(float x, float y) {
        for (int i = 0; i < LayerState.N_CHILDREN; i++) {
            final Drawable dr = mLayerState.mChildren[i].mDrawable;
            if (dr != null)
                dr.setHotspot(x, y);
        }
    }

    @Override
    public void setHotspotBounds(int left, int top, int right, int bottom) {
        for (int i = 0; i < LayerState.N_CHILDREN; i++) {
            final Drawable dr = mLayerState.mChildren[i].mDrawable;
            if (dr != null)
                dr.setHotspotBounds(left, top, right, bottom);
        }
        if (mHotspotBounds == null) {
            mHotspotBounds = new Rect(left, top, right, bottom);
        } else {
            mHotspotBounds.set(left, top, right, bottom);
        }
    }

    @Override
    public void getHotspotBounds(Rect outRect) {
        if (mHotspotBounds != null) {
            outRect.set(mHotspotBounds);
        } else {
            super.getHotspotBounds(outRect);
        }
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        final boolean changed = super.setVisible(visible, restart);
        for (int i = 0; i < LayerState.N_CHILDREN; i++) {
            final Drawable dr = mLayerState.mChildren[i].mDrawable;
            if (dr != null)
                dr.setVisible(visible, restart);
        }
        return changed;
    }

    @Override
    public void setDither(boolean dither) {
        for (int i = 0; i < LayerState.N_CHILDREN; i++) {
            final Drawable dr = mLayerState.mChildren[i].mDrawable;
            if (dr != null)
                dr.setDither(dither);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public int getAlpha() {
        return mPaint.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        for (int i = 0; i < LayerState.N_CHILDREN; i++) {
            final Drawable dr = mLayerState.mChildren[i].mDrawable;
            if (dr != null)
                dr.setColorFilter(colorFilter);
        }
    }

    @Override
    public void setTintList(ColorStateList tint) {
        for (int i = 0; i < LayerState.N_CHILDREN; i++) {
            final Drawable dr = mLayerState.mChildren[i].mDrawable;
            if (dr != null)
                dr.setTintList(tint);
        }
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.Q)
    public void setTintBlendMode(@NonNull BlendMode blendMode) {
        for (int i = 0; i < LayerState.N_CHILDREN; i++) {
            final Drawable dr = mLayerState.mChildren[i].mDrawable;
            if (dr != null)
                dr.setTintBlendMode(blendMode);
        }
    }

    public void setOpacity(int opacity) {
        mLayerState.mOpacityOverride = opacity;
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAutoMirrored(boolean mirrored) {
        mLayerState.mAutoMirrored = mirrored;
        for (int i = 0; i < LayerState.N_CHILDREN; i++) {
            final Drawable dr = mLayerState.mChildren[i].mDrawable;
            if (dr != null)
                dr.setAutoMirrored(mirrored);
        }
    }

    @Override
    public boolean isAutoMirrored() {
        return mLayerState.mAutoMirrored;
    }

    @Override
    public void jumpToCurrentState() {
        for (int i = 0; i < LayerState.N_CHILDREN; i++) {
            final Drawable dr = mLayerState.mChildren[i].mDrawable;
            if (dr != null)
                dr.jumpToCurrentState();
        }
    }

    @Override
    public boolean isStateful() {
        return mLayerState.isStateful();
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.S)
    public boolean hasFocusStateSpecified() {
        return mLayerState.hasFocusStateSpecified();
    }

    @Override
    protected boolean onStateChange(int[] state) {
        boolean changed = false;
        for (int i = 0; i < LayerState.N_CHILDREN; i++) {
            final Drawable dr = mLayerState.mChildren[i].mDrawable;
            if (dr != null && dr.isStateful() && dr.setState(state)) {
                changed = true;
            }
        }
        if (changed)
            updateLayerBounds(getBounds());
        return changed;
    }

    @Override
    protected boolean onLevelChange(int level) {
        boolean changed = false;
        for (int i = 0; i < LayerState.N_CHILDREN; i++) {
            final Drawable dr = mLayerState.mChildren[i].mDrawable;
            if (dr != null && dr.setLevel(level)) {
                changed = true;
            }
        }
        if (changed)
            updateLayerBounds(getBounds());
        return changed;
    }

    @Override
    public int getIntrinsicWidth() {
        return (int) (getMaxIntrinsicWidth() * DEFAULT_VIEW_PORT_SCALE);
    }

    private int getMaxIntrinsicWidth() {
        int width = -1;
        for (int i = 0; i < LayerState.N_CHILDREN; i++) {
            final ChildDrawable r = mLayerState.mChildren[i];
            if (r.mDrawable == null)
                continue;
            final int w = r.mDrawable.getIntrinsicWidth();
            if (w > width)
                width = w;
        }
        return width;
    }

    @Override
    public int getIntrinsicHeight() {
        return (int) (getMaxIntrinsicHeight() * DEFAULT_VIEW_PORT_SCALE);
    }

    private int getMaxIntrinsicHeight() {
        int height = -1;
        for (int i = 0; i < LayerState.N_CHILDREN; i++) {
            final ChildDrawable r = mLayerState.mChildren[i];
            if (r.mDrawable == null)
                continue;
            final int h = r.mDrawable.getIntrinsicHeight();
            if (h > height)
                height = h;
        }
        return height;
    }

    @Override
    public ConstantState getConstantState() {
        if (mLayerState.canConstantState()) {
            mLayerState.mChangingConfigurations = getChangingConfigurations();
            return mLayerState;
        }
        return null;
    }

    @Override
    @NonNull
    public Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mLayerState = createConstantState(mLayerState, null);
            for (int i = 0; i < LayerState.N_CHILDREN; i++) {
                final Drawable dr = mLayerState.mChildren[i].mDrawable;
                if (dr != null)
                    dr.mutate();
            }
            mMutated = true;
        }
        return this;
    }

    public void clearMutated() {
        super.clearMutated();
        for (int i = 0; i < LayerState.N_CHILDREN; i++) {
            final Drawable dr = mLayerState.mChildren[i].mDrawable;
            if (dr != null)
                dr.clearMutated();
        }
        mMutated = false;
    }

    static class ChildDrawable {
        public Drawable mDrawable;
        public int[] mThemeAttrs;
        public int mDensity = DisplayMetrics.DENSITY_DEFAULT;

        ChildDrawable(int density) {
            mDensity = density;
        }

        ChildDrawable(@NonNull ChildDrawable orig, @NonNull AdaptiveIconDrawable owner, @Nullable Resources res) {
            final Drawable dr = orig.mDrawable;
            final Drawable clone;
            if (dr != null) {
                final ConstantState cs = dr.getConstantState();
                if (cs == null) {
                    clone = dr;
                } else if (res != null) {
                    clone = cs.newDrawable(res);
                } else {
                    clone = cs.newDrawable();
                }
                clone.setCallback(owner);
                clone.setBounds(dr.getBounds());
                clone.setLevel(dr.getLevel());
            } else {
                clone = null;
            }
            mDrawable = clone;
            mThemeAttrs = orig.mThemeAttrs;
            mDensity = resolveDensity(res, orig.mDensity);
        }

        public boolean canApplyTheme() {
            return mThemeAttrs != null || (mDrawable != null && mDrawable.canApplyTheme());
        }

        public final void setDensity(int targetDensity) {
            if (mDensity != targetDensity) {
                mDensity = targetDensity;
            }
        }
    }

    static class LayerState extends ConstantState {
        private int[] mThemeAttrs;

        static final int N_CHILDREN = 3;
        ChildDrawable[] mChildren;

        int mDensity;
        int mSrcDensityOverride = 0;
        int mOpacityOverride = PixelFormat.UNKNOWN;

        @Config
        int mChangingConfigurations;
        @Config
        int mChildrenChangingConfigurations;

        @DrawableRes
        int mSourceDrawableId = Resources.ID_NULL;

        private boolean mCheckedOpacity;
        private int mOpacity;

        private boolean mCheckedStateful;
        private boolean mIsStateful;
        private boolean mAutoMirrored = false;

        LayerState(@Nullable LayerState orig, @NonNull AdaptiveIconDrawable owner, @Nullable Resources res) {
            mDensity = resolveDensity(res, orig != null ? orig.mDensity : 0);
            mChildren = new ChildDrawable[N_CHILDREN];
            if (orig != null) {
                final ChildDrawable[] origChildDrawable = orig.mChildren;
                mChangingConfigurations = orig.mChangingConfigurations;
                mChildrenChangingConfigurations = orig.mChildrenChangingConfigurations;
                mSourceDrawableId = orig.mSourceDrawableId;
                for (int i = 0; i < N_CHILDREN; i++) {
                    final ChildDrawable or = origChildDrawable[i];
                    mChildren[i] = new ChildDrawable(or, owner, res);
                }
                mCheckedOpacity = orig.mCheckedOpacity;
                mOpacity = orig.mOpacity;
                mCheckedStateful = orig.mCheckedStateful;
                mIsStateful = orig.mIsStateful;
                mAutoMirrored = orig.mAutoMirrored;
                mThemeAttrs = orig.mThemeAttrs;
                mOpacityOverride = orig.mOpacityOverride;
                mSrcDensityOverride = orig.mSrcDensityOverride;
            } else {
                for (int i = 0; i < N_CHILDREN; i++) {
                    mChildren[i] = new ChildDrawable(mDensity);
                }
            }
        }

        public final void setDensity(int targetDensity) {
            if (mDensity != targetDensity) {
                mDensity = targetDensity;
            }
        }

        @Override
        public boolean canApplyTheme() {
            if (mThemeAttrs != null || super.canApplyTheme())
                return true;
            for (int i = 0; i < N_CHILDREN; i++) {
                if (mChildren[i].canApplyTheme())
                    return true;
            }
            return false;
        }

        @Override
        @NonNull
        public Drawable newDrawable() {
            return new CustomAdaptiveIconDrawable(this, null);
        }

        @Override
        @NonNull
        public Drawable newDrawable(@Nullable Resources res) {
            return new CustomAdaptiveIconDrawable(this, res);
        }

        @Override
        public @Config int getChangingConfigurations() {
            return mChangingConfigurations | mChildrenChangingConfigurations;
        }

        public final int getOpacity() {
            if (mCheckedOpacity)
                return mOpacity;
            int firstIndex = -1;
            for (int i = 0; i < N_CHILDREN; i++) {
                if (mChildren[i].mDrawable != null) {
                    firstIndex = i;
                    break;
                }
            }
            int op;
            if (firstIndex >= 0) {
                op = mChildren[firstIndex].mDrawable.getOpacity();
            } else {
                op = PixelFormat.TRANSPARENT;
            }
            for (int i = firstIndex + 1; i < N_CHILDREN; i++) {
                final Drawable dr = mChildren[i].mDrawable;
                if (dr != null) {
                    op = Drawable.resolveOpacity(op, dr.getOpacity());
                }
            }
            mOpacity = op;
            mCheckedOpacity = true;
            return op;
        }

        public final boolean isStateful() {
            if (mCheckedStateful)
                return mIsStateful;
            boolean isStateful = false;
            for (int i = 0; i < N_CHILDREN; i++) {
                final Drawable dr = mChildren[i].mDrawable;
                if (dr != null && dr.isStateful()) {
                    isStateful = true;
                    break;
                }
            }
            mIsStateful = isStateful;
            mCheckedStateful = true;
            return isStateful;
        }

        @RequiresApi(Build.VERSION_CODES.S)
        public final boolean hasFocusStateSpecified() {
            for (int i = 0; i < N_CHILDREN; i++) {
                final Drawable dr = mChildren[i].mDrawable;
                if (dr != null && dr.hasFocusStateSpecified())
                    return true;
            }
            return false;
        }

        public final boolean canConstantState() {
            for (int i = 0; i < N_CHILDREN; i++) {
                final Drawable dr = mChildren[i].mDrawable;
                if (dr != null && dr.getConstantState() == null)
                    return false;
            }
            return true;
        }

        public void invalidateCache() {
            mCheckedOpacity = false;
            mCheckedStateful = false;
        }
    }
}
