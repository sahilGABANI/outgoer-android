package com.outgoer.ui.home.view;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.AttrRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StyleRes;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.outgoer.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import timber.log.Timber;

public class OutgoerTabBarView extends FrameLayout {

    public static final String INTENT_KEY_DEFAULT_TAB = "INTENT_KEY_DEFAULT_TAB";
    public static final int TAB_HOME = 0;
    public static final int TAB_MAP = 1;
    public static final int TAB_CREATE = 2;
    public static final int TAB_MESSAGE = 3;
    public static final int TAB_MY_PROFILE = 4;
    public static final int TAB_REELS = 5;
    public static final int TAB_BOTTOM_SHEET = 6;


    private View llHome;
    private AppCompatTextView tvHome;
    private AppCompatImageView ivHomeNavigation;


    private View llSearch;
    private AppCompatTextView tvSearch;

    private View llMap;
    private AppCompatTextView tvMap;
    private AppCompatImageView ivMapNavigation;


    private View llMessage;
    private AppCompatTextView tvMessage;
    private AppCompatImageView ivMessageNavigation;


    private View llMyProfile;
    private AppCompatTextView tvMyProfile;
    private AppCompatImageView ivProfileNavigation;

    private View llCreate;



    private int currentTab;

    private TabBarItemClickListener mListener;

    private int typeFaceNormal;
    private int typeFaceBold;

    public OutgoerTabBarView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public OutgoerTabBarView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public OutgoerTabBarView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public OutgoerTabBarView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.it_ui_view_tabbar, this);

        typeFaceNormal = Typeface.NORMAL;
        typeFaceBold = Typeface.BOLD;

        llHome = findViewById(R.id.llHome);
        tvHome = findViewById(R.id.tvHome);
        ivHomeNavigation = findViewById(R.id.ivHomeNavigation);

        llSearch = findViewById(R.id.llSearch);
        tvSearch = findViewById(R.id.tvSearch);

        llMap = findViewById(R.id.llMap);
        tvMap = findViewById(R.id.tvMap);
        ivMapNavigation = findViewById(R.id.ivMapNavigation);


        llMessage = findViewById(R.id.llMessage);
        tvMessage = findViewById(R.id.tvMessage);
        ivMessageNavigation = findViewById(R.id.ivChatNavigation);


        llMyProfile = findViewById(R.id.llMyProfile);
        tvMyProfile = findViewById(R.id.tvMyProfile);
        ivProfileNavigation = findViewById(R.id.ivProfileNavigation);

        llCreate = findViewById(R.id.llCreate);


        llHome.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onTabBarItemClicked(TAB_HOME);
            }
        });

        llSearch.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onTabBarItemClicked(TAB_REELS);
            }
        });

        llMap.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onTabBarItemClicked(TAB_MAP);
            }
        });

        llMessage.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onTabBarItemClicked(TAB_MESSAGE);
            }
        });

        llMyProfile.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onTabBarItemClicked(TAB_MY_PROFILE);
            }
        });

        llCreate.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onTabBarItemClicked(TAB_CREATE);
            }
        });

        llMyProfile.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (mListener != null) {
                    mListener.onTabBarItemClicked(OutgoerTabBarView.TAB_BOTTOM_SHEET);
                }
                return true;
            }
        });
    }

    public void setOnTabItemClickListener(TabBarItemClickListener listener) {
        mListener = listener;
    }

    public @TabType
    int getActivatedTab() {
        return currentTab;
    }

    public void setActivatedTab(@TabType int tabType) {
        currentTab = tabType;
        llHome.setActivated(tabType == TAB_HOME);
        llSearch.setActivated(tabType == TAB_REELS);
        llMap.setActivated(tabType == TAB_MAP);
        llMessage.setActivated(tabType == TAB_MESSAGE);
        llMyProfile.setActivated(tabType == TAB_MY_PROFILE);
        llCreate.setActivated(tabType == TAB_CREATE);

        tvHome.setTypeface(null, typeFaceNormal);
        tvSearch.setTypeface(null, typeFaceNormal);
        tvMap.setTypeface(null, typeFaceNormal);
        tvMessage.setTypeface(null, typeFaceNormal);
        tvMyProfile.setTypeface(null, typeFaceNormal);
//        ivHomeNavigation.setVisibility(View.INVISIBLE);
//        ivMapNavigation.setVisibility(View.INVISIBLE);
//        ivMessageNavigation.setVisibility(View.INVISIBLE);
//        ivProfileNavigation.setVisibility(View.INVISIBLE);



        switch (tabType) {
            case OutgoerTabBarView.TAB_HOME:
                tvHome.setTypeface(null, typeFaceBold);
//                ivHomeNavigation.setVisibility(View.VISIBLE);
                break;
            case OutgoerTabBarView.TAB_REELS:
                tvSearch.setTypeface(null, typeFaceBold);
                break;
            case OutgoerTabBarView.TAB_MAP:
                tvMap.setTypeface(null, typeFaceBold);
//                ivMapNavigation.setVisibility(View.VISIBLE);
                break;
            case OutgoerTabBarView.TAB_MESSAGE:
                tvMessage.setTypeface(null, typeFaceBold);
//                ivMessageNavigation.setVisibility(View.VISIBLE);
                break;
            case OutgoerTabBarView.TAB_MY_PROFILE:
                tvMyProfile.setTypeface(null, typeFaceBold);
//                ivProfileNavigation.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState newState = new SavedState(superState);
        newState.currentPosition = currentTab;
        return newState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        try {
            super.onRestoreInstanceState(state);
        } catch (Exception e) {
            Timber.w(e, "Different Android OS codes has different codes at this place which might cause crash.");
        }
        currentTab = savedState.currentPosition;
        setActivatedTab(currentTab);
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TAB_HOME, TAB_REELS, TAB_MAP,TAB_CREATE, TAB_MESSAGE, TAB_MY_PROFILE, TAB_BOTTOM_SHEET
    })
    public @interface TabType {
    }

    public interface TabBarItemClickListener {
        void onTabBarItemClicked(@TabType int tabType);
    }

    static class SavedState extends BaseSavedState {

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {

            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        int currentPosition;

        public SavedState(Parcel source) {
            super(source);
            currentPosition = source.readInt();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(currentPosition);
        }
    }
}
