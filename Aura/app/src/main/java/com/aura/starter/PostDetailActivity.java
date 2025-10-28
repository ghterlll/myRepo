package com.aura.starter;

import android.os.Bundle;
import android.widget.ImageButton;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.aura.starter.model.Post;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for displaying post details with vertical swipe navigation
 * Supports swiping up/down to navigate between posts (similar to TikTok/Douyin)
 */
public class PostDetailActivity extends AppCompatActivity {
    private static final String TAG = "PostDetailActivity";

    private ViewPager2 viewPager;
    private PostDetailPagerAdapter adapter;
    private List<Post> posts;
    private int initialPosition;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail_pager);

        // Get post data from intent
        Post currentPost = (Post) getIntent().getSerializableExtra("post");
        ArrayList<Post> postList = (ArrayList<Post>) getIntent().getSerializableExtra("post_list");
        initialPosition = getIntent().getIntExtra("position", 0);

        // If post_list is provided, use it for swipe navigation
        if (postList != null && !postList.isEmpty()) {
            posts = postList;
        } else {
            // Fallback: single post mode (no swipe navigation)
            posts = new ArrayList<>();
            if (currentPost != null) {
                posts.add(currentPost);
                initialPosition = 0;
            }
        }

        // Initialize ViewPager2
        viewPager = findViewById(R.id.viewPager);
        viewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);

        // Setup adapter
        adapter = new PostDetailPagerAdapter(this, posts);
        viewPager.setAdapter(adapter);

        // Set initial position
        viewPager.setCurrentItem(initialPosition, false);

        // Setup back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }
}
