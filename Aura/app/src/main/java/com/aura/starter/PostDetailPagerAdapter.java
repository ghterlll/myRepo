package com.aura.starter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.aura.starter.model.Post;

import java.util.List;

/**
 * Adapter for ViewPager2 to display post details with vertical swipe navigation
 */
public class PostDetailPagerAdapter extends FragmentStateAdapter {
    private final List<Post> posts;

    public PostDetailPagerAdapter(@NonNull FragmentActivity fragmentActivity, List<Post> posts) {
        super(fragmentActivity);
        this.posts = posts;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return PostDetailFragment.newInstance(posts.get(position));
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }
}
