package com.example.studentfood.presentation.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.studentfood.R;
import com.example.studentfood.presentation.ui.activity.LoginActivity;
import com.example.studentfood.presentation.ui.delegate.CommunityDrawerDelegate;
import com.example.studentfood.presentation.ui.delegate.CommunitySearchDelegate;
import com.example.studentfood.presentation.viewmodel.CommunityViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * CommunityFragment - Chì làm nhiêm vu "gân kêt" (Binding) các view và khôi tao ViewPager
 * Logic dã duoc tách ra thành các delegates và ViewModel
 */
public class CommunityFragment extends Fragment {

    private CommunityViewModel viewModel;
    
    // UI Components - Chì binding
    private DrawerLayout drawerLayout;
    private View communityHeader;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private FloatingActionButton fabScrollTop;
    private View layoutLoggedIn;
    private View layoutGuest;
    private View btnGoToLogin;

    // Delegates
    private CommunitySearchDelegate searchDelegate;
    private CommunityDrawerDelegate drawerDelegate;
    
    // ViewPager adapter
    private CommunityPagerAdapter pagerAdapter;
    private PostFeedFragment postFeedFragment;

    public CommunityFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_community, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // ViewModel
        viewModel = new ViewModelProvider(this).get(CommunityViewModel.class);
        
        // Binding views
        bindViews(view);
        
        // Setup observers
        setupObservers();
        
        // Check login status (logic trong ViewModel)
        viewModel.checkLoginStatus();
    }

    private void bindViews(View view) {
        drawerLayout    = view.findViewById(R.id.drawerLayout);
        communityHeader = view.findViewById(R.id.communityHeader);
        tabLayout       = view.findViewById(R.id.tabLayout);
        viewPager       = view.findViewById(R.id.viewPager);
        fabScrollTop    = view.findViewById(R.id.fabScrollTop);
        layoutLoggedIn  = view.findViewById(R.id.layoutLoggedIn);
        layoutGuest     = view.findViewById(R.id.layoutGuest);
        btnGoToLogin    = view.findViewById(R.id.btnGoToLogin);

        // Khôi tao delegates
        searchDelegate = new CommunitySearchDelegate(
            this, 
            viewModel, 
            view.findViewById(R.id.layoutSearchBar),
            view.findViewById(R.id.edtSearch)
        );
        
        drawerDelegate = new CommunityDrawerDelegate(
            this,
            viewModel,
            drawerLayout,
            view.findViewById(R.id.navigationView)
        );
    }

    private void setupObservers() {
        // Observer cho login status
        viewModel.getIsLoggedIn().observe(getViewLifecycleOwner(), isLoggedIn -> {
            if (isLoggedIn) {
                showLoggedInUI();
            } else {
                showGuestUI();
            }
        });
    }

    private void showLoggedInUI() {
        layoutLoggedIn.setVisibility(View.VISIBLE);
        layoutGuest.setVisibility(View.GONE);

        // Setup các components
        setupTabs();
        searchDelegate.setupSearch(requireView());
        drawerDelegate.setupDrawer(requireView());
        setupFab();
    }

    private void showGuestUI() {
        layoutLoggedIn.setVisibility(View.GONE);
        layoutGuest.setVisibility(View.VISIBLE);

        btnGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });
    }

    // ===================== TABS (ViewPager) =====================

    private void setupTabs() {
        viewPager.setUserInputEnabled(false);
        pagerAdapter = new CommunityPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
            (tab, position) -> {
                if (position == 0) tab.setText("Bài viêc");
                else if (position == 1) tab.setText("Video");
                else tab.setText("Hôi nhóm");
            }
        ).attach();

        setupTabListeners();
        setupViewPagerAnimations();
    }

    private void setupTabListeners() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {}
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0 && postFeedFragment != null) {
                    postFeedFragment.scrollToTopAndRefresh();
                }
            }
        });
    }

    private void setupViewPagerAnimations() {
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                handlePageChange(position);
            }
        });
    }

    private void handlePageChange(int position) {
        View bottomNav = requireActivity().findViewById(R.id.bottomNavigation);
        View container = requireActivity().findViewById(R.id.nav_host_fragment_content_main);

        if (position == 1) {
            // Tab Video: m tab, n bottom nav
            setupVideoTabUI(tabLayout, bottomNav, container);
        } else {
            // Tab Bài viêc / Hôi nhóm: restore
            setupNormalTabUI(tabLayout, bottomNav, container);
            showHeaderAndNav();
        }
    }

    private void setupVideoTabUI(TabLayout tabLayout, View bottomNav, View container) {
        tabLayout.setBackgroundColor(0xCC000000);
        tabLayout.setTabTextColors(0xAAFFFFFF, 0xFFFFFFFF);
        tabLayout.setSelectedTabIndicatorColor(0xFFFFFFFF);
        
        if (bottomNav != null) {
            bottomNav.animate().translationY(bottomNav.getHeight()).alpha(0f).setDuration(200).start();
        }
        if (container != null) {
            container.setPadding(0, 0, 0, 0);
        }
    }

    private void setupNormalTabUI(TabLayout tabLayout, View bottomNav, View container) {
        tabLayout.setBackgroundColor(0xFFFFFFFF);
        tabLayout.setTabTextColors(0xFF888888, requireContext().getResources().getColor(R.color.light_blue_600));
        tabLayout.setSelectedTabIndicatorColor(requireContext().getResources().getColor(R.color.light_blue_600));
        
        if (bottomNav != null) {
            bottomNav.animate().translationY(0).alpha(1f).setDuration(200).start();
        }
        if (bottomNav != null && container != null) {
            int navH = bottomNav.getHeight() > 0 ? bottomNav.getHeight() : 160;
            container.setPadding(0, 0, 0, navH);
        }
    }

    // ===================== FAB =====================

    private void setupFab() {
        fabScrollTop.setOnClickListener(v -> {
            if (postFeedFragment != null) {
                postFeedFragment.scrollToTopAndRefresh();
            }
            showHeaderAndNav();
        });
    }

    // ===================== HEADER/NAV ANIMATIONS =====================

    public void hideHeaderAndNav() {
        if (communityHeader.getTranslationY() != 0) return;
        
        int h = communityHeader.getHeight();
        if (h == 0) return;

        // Animate header
        communityHeader.animate().translationY(-h).setDuration(250).start();

        // Animate ViewPager top margin
        animateViewPagerMargin(-h);

        // Animate bottom nav
        animateBottomNavHide();

        // Show FAB
        showFab();
    }

    public void showHeaderAndNav() {
        if (communityHeader.getTranslationY() == 0) return;

        // Animate header
        communityHeader.animate().translationY(0).setDuration(250).start();

        // Animate ViewPager top margin
        animateViewPagerMargin(0);

        // Animate bottom nav
        animateBottomNavShow();

        // Hide FAB
        hideFab();
    }

    private void animateViewPagerMargin(int targetMargin) {
        android.view.ViewGroup.MarginLayoutParams vp = 
            (android.view.ViewGroup.MarginLayoutParams) viewPager.getLayoutParams();
        int fromMargin = vp.topMargin;
        
        android.animation.ValueAnimator anim = android.animation.ValueAnimator.ofInt(fromMargin, targetMargin);
        anim.setDuration(250);
        anim.addUpdateListener(va -> {
            vp.topMargin = (int) va.getAnimatedValue();
            viewPager.setLayoutParams(vp);
        });
        anim.start();
    }

    private void animateBottomNavHide() {
        View bottomNav = requireActivity().findViewById(R.id.bottomNavigation);
        View container = requireActivity().findViewById(R.id.nav_host_fragment_content_main);
        
        if (bottomNav != null) {
            int navH = bottomNav.getHeight();
            bottomNav.animate()
                .translationY(navH).alpha(0f).setDuration(220)
                .withEndAction(() -> {
                    if (container != null) container.setPadding(0, 0, 0, 0);
                })
                .start();
        }
        
        fabScrollTop.setVisibility(View.VISIBLE);
        fabScrollTop.setAlpha(0f);
        fabScrollTop.animate().alpha(1f).setDuration(180).start();
    }

    private void animateBottomNavShow() {
        View bottomNav = requireActivity().findViewById(R.id.bottomNavigation);
        View container = requireActivity().findViewById(R.id.nav_host_fragment_content_main);
        
        if (bottomNav != null) {
            int navH = bottomNav.getHeight() > 0 ? bottomNav.getHeight() : 160;
            if (container != null) container.setPadding(0, 0, 0, navH);
            bottomNav.animate().translationY(0).alpha(1f).setDuration(220).start();
        }
        
        hideFab();
    }

    private void showFab() {
        fabScrollTop.setVisibility(View.VISIBLE);
        fabScrollTop.setAlpha(0f);
        fabScrollTop.animate().alpha(1f).setDuration(180).start();
    }

    private void hideFab() {
        fabScrollTop.setVisibility(View.GONE);
        fabScrollTop.setAlpha(0f);
    }

    // ===================== PAGER ADAPTER =====================

    private class CommunityPagerAdapter extends FragmentStateAdapter {
        CommunityPagerAdapter(Fragment f) { 
            super(f); 
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                postFeedFragment = new PostFeedFragment();
                return postFeedFragment;
            } else if (position == 1) {
                return new VideoFeedFragment();
            } else {
                return new GroupFeedFragment();
            }
        }

        @Override
        public int getItemCount() { 
            return 3; 
        }
    }

    // ===================== LIFECYCLE =====================

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Cleanup delegates
        if (searchDelegate != null) {
            searchDelegate.cleanup();
        }
        if (drawerDelegate != null) {
            drawerDelegate.cleanup();
        }
        
        // Clear references
        searchDelegate = null;
        drawerDelegate = null;
        postFeedFragment = null;
        pagerAdapter = null;
    }
}
