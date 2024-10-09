package com.outgoer.base.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

import com.outgoer.R;

public class RectangleImageView extends AppCompatImageView {

    private static final int FIXED_DIMENSION_WIDTH = 0;
    private static final int FIXED_DIMENSION_HEIGHT = 1;

    private int mFixedDimension = -1;
    private float mWidthRatio = 1;
    private float mHeightRatio = 1;

    public RectangleImageView(Context context) {
        super(context);
    }

    public RectangleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RectangleImageView, 0, 0);
        try {
            mFixedDimension = a.getInteger(R.styleable.RectangleImageView_fixedDimension, FIXED_DIMENSION_WIDTH);
            mWidthRatio = a.getFloat(R.styleable.RectangleImageView_widthRatio, 1f);
            mHeightRatio = a.getFloat(R.styleable.RectangleImageView_heightRatio, 1f);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        try {
            if (mFixedDimension == FIXED_DIMENSION_WIDTH) {
                float height = getMeasuredWidth() / mWidthRatio * mHeightRatio;
                setMeasuredDimension(getMeasuredWidth(), (int) height);
            } else if (mFixedDimension == FIXED_DIMENSION_HEIGHT) {
                float width = getMeasuredHeight() / mHeightRatio * mWidthRatio;
                setMeasuredDimension((int) width, getMeasuredHeight());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}