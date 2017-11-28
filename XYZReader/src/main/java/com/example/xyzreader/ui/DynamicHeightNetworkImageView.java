package com.example.xyzreader.ui;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;

import com.android.volley.toolbox.NetworkImageView;
import com.example.xyzreader.R;

// https://stackoverflow.com/a/34266244/1615055
public class DynamicHeightNetworkImageView extends NetworkImageView {
    private Float mAspectRatio = null; //1.5f;
    private int mAnimDuration = 600;
    private boolean shouldAnimate = true;

    public DynamicHeightNetworkImageView(Context context) {
        super(context);
        init();
    }

    public DynamicHeightNetworkImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DynamicHeightNetworkImageView(Context context, AttributeSet attrs,
                                         int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mAspectRatio = null;
        shouldAnimate = true;
        mAnimDuration = getContext().getResources().getInteger(R.integer
            .anim_duration_slow);
    }

    public void setAspectRatio(float aspectRatio) {
        mAspectRatio = aspectRatio;
        requestLayout();
    }

    public void setShouldAnimation(boolean value) {
        shouldAnimate = value;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mAspectRatio != null) {
            int measuredWidth = getMeasuredWidth();
            setMeasuredDimension(measuredWidth, (int) (measuredWidth /
                mAspectRatio));
        }
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        if (shouldAnimate) {
            ObjectAnimator.ofFloat(this, "alpha", 0, 1).setDuration
                (mAnimDuration).start();
        }
    }

}
