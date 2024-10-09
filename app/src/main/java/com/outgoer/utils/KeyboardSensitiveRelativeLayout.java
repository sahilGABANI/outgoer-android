package com.outgoer.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

public class KeyboardSensitiveRelativeLayout extends RelativeLayout {

    private OnKeyboardShowHideListener listener;

    public KeyboardSensitiveRelativeLayout(Context context) {
        super(context);
    }

    public KeyboardSensitiveRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public KeyboardSensitiveRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public KeyboardSensitiveRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setKeyboardListener(OnKeyboardShowHideListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int proposedHeight = View.MeasureSpec.getSize(heightMeasureSpec);
        final int actualHeight = getHeight();
        if (actualHeight != proposedHeight && listener != null) {
            if (actualHeight > proposedHeight) {
                listener.onKeyboardShowHide(true);
            } else {
                listener.onKeyboardShowHide(false);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public interface OnKeyboardShowHideListener {
        void onKeyboardShowHide(boolean visible);
    }
}
