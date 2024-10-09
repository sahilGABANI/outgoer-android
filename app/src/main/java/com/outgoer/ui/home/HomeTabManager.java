package com.outgoer.ui.home;


import static com.outgoer.ui.home.FragmentHomeContainer.BACK_STACK_ROOT_NAME;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.outgoer.R;
import com.outgoer.api.authentication.model.OutgoerUser;
import com.outgoer.application.OutgoerApplication;
import com.outgoer.base.RxBus;
import com.outgoer.base.RxEvent;
import com.outgoer.ui.home.helper.OnBackPressListener;
import com.outgoer.ui.home.view.OutgoerTabBarView;
import com.outgoer.utils.Utility.TabItemClickListener;

import java.util.HashMap;

import timber.log.Timber;

public class HomeTabManager {

    private final HomeActivity homeActivity;
    private final OutgoerTabBarView tabBar;
    private final ViewPager2 viewPager;
    private final FragmentStateAdapter viewPagerAdapter;
    private OutgoerUser loggedInuser = null;
    private Boolean isSponty = false;
    private TabItemClickListener tabItemClickListener;
    private Fragment currentSelectedFragmentContainer;

    public HomeTabManager(HomeActivity activity, OutgoerUser loggedInUser, Boolean isFromSponty, TabItemClickListener listener) {
        homeActivity = activity;
        loggedInuser = loggedInUser;
        this.tabItemClickListener = listener;
        OutgoerApplication.component.inject(this);
        tabBar = homeActivity.findViewById(R.id.tabBar);
        tabBar.setOnTabItemClickListener(tabType -> {
            selectTab(tabType, true);
        });
        viewPager = homeActivity.findViewById(R.id.viewPager);
        viewPager.setUserInputEnabled(false);
        viewPager.setOffscreenPageLimit(2);

        isSponty = isFromSponty;
        if(loggedInUser !=null && loggedInUser.getUserType().equals("venue_owner")) {
            AppCompatImageView ivMyProfile = tabBar.findViewById(R.id.ivMyProfile);
            ivMyProfile.setImageDrawable(homeActivity.getResources().getDrawable(R.drawable.new_home_footer_location, null));
        }

        viewPagerAdapter = new FragmentStateAdapter(homeActivity.getSupportFragmentManager(), homeActivity.getLifecycle()) {
            @Override
            public int getItemCount() {
                return 5;
            }

            private final HashMap<Integer, Fragment> fragments = new HashMap<>();

            @NonNull
            @Override
            public Fragment createFragment(int position) {
                if (!fragments.containsKey(position) || fragments.get(position) == null) {
                    FragmentHomeContainer homeContainer = FragmentHomeContainer.create(position, loggedInUser);

                    homeContainer.updateIsSponty(isSponty);
                    fragments.put(position, homeContainer);
                }
                return fragments.get(position);
            }
        };
        viewPager.setAdapter(viewPagerAdapter);
        tabBar.setActivatedTab(OutgoerTabBarView.TAB_HOME);
    }

    public void selectTab(@OutgoerTabBarView.TabType int tabType, boolean actionByUser) {
        if (tabType == OutgoerTabBarView.TAB_CREATE) {
            homeActivity.addPostViewClick();
            return;
        }

        if (tabType == OutgoerTabBarView.TAB_BOTTOM_SHEET) {
            homeActivity.openSwitchAccountDialog();
            return;
        }

        tabBar.setActivatedTab(tabType);

        if(!((tabType == 2 || tabType == 6) && loggedInuser == null)) {
            if (actionByUser && tabType == viewPager.getCurrentItem()) {
                // Tap the one which has been selected

                currentSelectedFragmentContainer = viewPagerAdapter.createFragment(tabType);
                if (currentSelectedFragmentContainer.isAdded()) {
                    // try to pop up all the top fragments
                    if (currentSelectedFragmentContainer.getChildFragmentManager().getBackStackEntryCount() > 1) {
                        currentSelectedFragmentContainer.getChildFragmentManager().popBackStack(BACK_STACK_ROOT_NAME, 0);
                    }
                }
                if (tabItemClickListener != null) {
                    assert currentSelectedFragmentContainer.getTag() != null;
                    tabItemClickListener.onTabItemClicked(currentSelectedFragmentContainer.getTag());
                }
            } else {
                currentSelectedFragmentContainer = viewPagerAdapter.createFragment(tabType);
                if(tabType == 3) {
                    tabItemClickListener.onTabItemClicked("f3");
                } else if(tabType == 1) {
                    tabItemClickListener.onTabItemClicked("f1");
                }

                viewPager.setCurrentItem(tabType, false);
            }
            RxBus.INSTANCE.publish(new RxEvent.HomeTabChangeEvent(tabType));
        }
    }

    public @OutgoerTabBarView.TabType
    int getActivatedTab() {
        return tabBar.getActivatedTab();
    }

    public void addChildFragment(@OutgoerTabBarView.TabType int tabType, final Fragment targetFragment, boolean withAnimation) {
        Fragment fragment = viewPagerAdapter.createFragment(tabType);

        AppCompatImageView ivMyProfile = tabBar.findViewById(R.id.ivMyProfile);
        ivMyProfile.setImageDrawable(homeActivity.getResources().getDrawable(R.drawable.new_home_footer_location, null));

        if (fragment instanceof FragmentHomeContainer) {
            FragmentHomeContainer homeContainer = (FragmentHomeContainer) fragment;
            homeContainer.addChildFragment(targetFragment, withAnimation);

            homeContainer.updateIsSponty(isSponty);
        } else {
            Timber.e("Something wrong with the base container fragment in viewpager");
        }
        tabBar.setActivatedTab(tabType);
    }

    public void addFragmentToCurrentTab(Fragment fragment, boolean withAnimation) {
        addChildFragment(viewPager.getCurrentItem(), fragment, withAnimation);
    }

    public boolean onBackPressed() {
        Fragment fragment = viewPagerAdapter.createFragment(viewPager.getCurrentItem());
        if (fragment != null && fragment instanceof OnBackPressListener) {
            return ((OnBackPressListener) fragment).onBackPressed();
        } else {
            return false;
        }
    }
}