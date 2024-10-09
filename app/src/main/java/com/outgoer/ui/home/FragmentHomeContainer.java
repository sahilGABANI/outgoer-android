package com.outgoer.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.outgoer.R;
import com.outgoer.api.authentication.model.OutgoerUser;
import com.outgoer.base.BaseFragment;
import com.outgoer.ui.home.helper.OnBackPressListener;
import com.outgoer.ui.home.newReels.DiscoverReelsFragment;
import com.outgoer.ui.home.newReels.NewReelsFragment;
import com.outgoer.ui.home.newmap.NewMapFragment;
import com.outgoer.ui.home.profile.newprofile.NewMyProfileFragment;
import com.outgoer.ui.home.profile.venue_profile.VenueProfileFragment;
import com.outgoer.ui.home.search.SearchFragment;
import com.outgoer.ui.home.view.OutgoerTabBarView;
import com.outgoer.utils.UiUtils;

import java.util.List;

import timber.log.Timber;

public class FragmentHomeContainer extends BaseFragment implements OnBackPressListener {

    public static final String BACK_STACK_ROOT_NAME = "com.outgoer.ui" + ".HOME_CONTAINER_BACK_STACK_ROOT_NAME";
    private static final String BUNDLE_KEY_TAB_TYPE = "BUNDLE_KEY_TAB_TYPE";
    private static final String BUNDLE_KEY_LOGGED_IN_USER = "BUNDLE_KEY_LOGGED_IN_USER";

    //Now this is only used for launching ActivityHome by notification and we need to start another
    //fragment on top of root fragment.
    private Fragment mPendingFragment;
    private Boolean isSponty = false;
    public static String tagName;

    void updateIsSponty(Boolean isFromSponty) {
        isSponty = isFromSponty;
    }

    public static FragmentHomeContainer create(@OutgoerTabBarView.TabType int homeTabType, OutgoerUser loggedInUser) {
        Bundle bundle = new Bundle();
        bundle.putInt(BUNDLE_KEY_TAB_TYPE, homeTabType);
        bundle.putParcelable(BUNDLE_KEY_LOGGED_IN_USER, loggedInUser);
        FragmentHomeContainer fragmentHomeContainer = new FragmentHomeContainer();
        fragmentHomeContainer.setArguments(bundle);
        return fragmentHomeContainer;
    }

    @Override
    public void onCreate(@org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.it_fragment_container, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getArguments() != null) {
            @OutgoerTabBarView.TabType int tabType = getArguments().getInt(BUNDLE_KEY_TAB_TYPE);
            OutgoerUser loggedInUser = getArguments().getParcelable(BUNDLE_KEY_LOGGED_IN_USER);
            Fragment defaultRootFragment = null;
            switch (tabType) {
                case OutgoerTabBarView.TAB_HOME:
                    defaultRootFragment = NewReelsFragment.newInstance(isSponty);
                    break;
                case OutgoerTabBarView.TAB_CREATE:
                    defaultRootFragment = null;
                    break;
                case OutgoerTabBarView.TAB_MAP:
                    defaultRootFragment = NewMapFragment.newInstance();
                    break;
                case OutgoerTabBarView.TAB_MESSAGE:
                    defaultRootFragment = DiscoverReelsFragment.newInstance();
                    break;
                case OutgoerTabBarView.TAB_MY_PROFILE:
                    tagName = loggedInUser != null && "venue_owner".equals(loggedInUser.getUserType()) ? "VenueProfileFragmentTag" : "NewMyProfileFragmentTag";
                    defaultRootFragment = (loggedInUser != null && "venue_owner".equals(loggedInUser.getUserType())) ? VenueProfileFragment.newInstance() : NewMyProfileFragment.newInstance() ;
                    break;
                case OutgoerTabBarView.TAB_REELS:
                    defaultRootFragment = SearchFragment.newInstance();
                    break;
            }


            if (defaultRootFragment != null) {
                getChildFragmentManager().beginTransaction()
                        .add(R.id.child_fragment_container, defaultRootFragment)
                        .addToBackStack(BACK_STACK_ROOT_NAME)
                        .commitAllowingStateLoss();
            }

            if (mPendingFragment != null) {
                addChildFragment(mPendingFragment, defaultRootFragment);
                mPendingFragment = null;
            }
        }
    }

    public void addChildFragment(Fragment fragment, boolean withAnimation) {
        addChildFragment(fragment, null, withAnimation);
    }

    public void addChildFragment(Fragment fragment, Fragment forceHiddenFragment) {
        addChildFragment(fragment, forceHiddenFragment, true);
    }

    public void addChildFragment(Fragment fragment, Fragment forceHiddenFragment, boolean withAnimation) {
        if (fragment == null) {
            return;
        }
        if (!isAdded()) {
            mPendingFragment = fragment;
            return;
        }
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();

        if (forceHiddenFragment == null) {
            //If force hidden fragment is null, we hide the currently shown fragment.
            forceHiddenFragment = getChildFragmentManager().findFragmentById(R.id.child_fragment_container);
        }

        if (forceHiddenFragment != null) {
            fragmentTransaction.hide(forceHiddenFragment);
        }
        fragmentTransaction
                .add(R.id.child_fragment_container, fragment)
                .addToBackStack(fragment.getClass().getName())
                .commitAllowingStateLoss();
        UiUtils.hideKeyboard(getContext());
    }

    /**
     * We need to manually call onHiddenChanged of child fragment. Otherwise, child fragments will
     * not be aware of its hidden state.
     *
     * @param hidden
     */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!isAdded()) {
            return;
        }
        List<Fragment> fragments = getChildFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                fragment.onHiddenChanged(hidden);
            }
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (!isAdded()) {
            return;
        }
        List<Fragment> fragments = getChildFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                fragment.setUserVisibleHint(isVisibleToUser);
            }
        }
    }

    @Override
    public boolean onBackPressed() {
        super.onBackPressed();
        if (!isAdded()) {
            return true;
        }

        FragmentManager fragmentManager = getChildFragmentManager();
        Fragment currentTopFragment = UiUtils.findTopFragment(fragmentManager, R.id.child_fragment_container);
        if (currentTopFragment != null && currentTopFragment instanceof OnBackPressListener) {
            if (((OnBackPressListener) currentTopFragment).onBackPressed()) {
                return true;
            }
        }

        if (fragmentManager.getBackStackEntryCount() > 1) {
            try {
                fragmentManager.popBackStack();
            } catch (IllegalStateException e) {
                Timber.e(e, "Failed to pop back stack");
            }
            return true;
        }
        return false;
    }
}
