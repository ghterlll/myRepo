package com.aura.starter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ProfilePagerAdapter extends FragmentStateAdapter {

    public ProfilePagerAdapter(@NonNull Fragment fragment){
        super(fragment);
    }

    @NonNull @Override
    public Fragment createFragment(int position) {
        if (position == 0) return new MyPostsFragment();
        return new BookmarksFragment();
    }

    @Override
    public int getItemCount() { return 2; }
}
