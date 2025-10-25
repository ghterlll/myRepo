package com.aura.starter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aura.starter.data.AppRepository;
import com.aura.starter.model.Post;
import com.bumptech.glide.Glide;

public class PostDetailActivity extends AppCompatActivity {
    private Post post;
    private ImageButton btnLike, btnBookmark, btnBack;
    private ImageView img;
    private TextView tvContent, btnExpand;
    private LinearLayout layoutCommentInput, layoutLike, layoutBookmark, layoutComment;
    private TextView tvLikeCount, tvBookmarkCount, tvCommentCount;
    private boolean isContentExpanded = false;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        post = (Post)getIntent().getSerializableExtra("post");

        // Initialize views
        TextView tvTitle = findViewById(R.id.tvTitle);
        tvContent = findViewById(R.id.tvContent);
        btnExpand = findViewById(R.id.btnExpand);
        img = findViewById(R.id.imgCover);

        btnBack = findViewById(R.id.btnBack);
        btnLike = findViewById(R.id.btnLikeDetail);
        btnBookmark = findViewById(R.id.btnBookmarkDetail);

        // Interaction bar views
        layoutCommentInput = findViewById(R.id.layoutCommentInput);
        layoutLike = findViewById(R.id.layoutLike);
        layoutBookmark = findViewById(R.id.layoutBookmark);
        layoutComment = findViewById(R.id.layoutComment);

        tvLikeCount = findViewById(R.id.tvLikeCount);
        tvBookmarkCount = findViewById(R.id.tvBookmarkCount);
        tvCommentCount = findViewById(R.id.tvCommentCount);

        // Back button handler
        btnBack.setOnClickListener(v -> finish());

        // Like and bookmark handlers
        layoutLike.setOnClickListener(v -> { AppRepository.get().toggleLike(post.id); bind(); });
        layoutBookmark.setOnClickListener(v -> { AppRepository.get().toggleBookmark(post.id); bind(); });

        // Comment input opens comment page
        layoutCommentInput.setOnClickListener(v -> openCommentsPage());

        // Comment count click also opens comment page
        layoutComment.setOnClickListener(v -> openCommentsPage());

        // Expand/collapse content
        btnExpand.setOnClickListener(v -> {
            if (isContentExpanded) {
                tvContent.setMaxLines(3);
                btnExpand.setText("Expand");
                isContentExpanded = false;
            } else {
                tvContent.setMaxLines(Integer.MAX_VALUE);
                btnExpand.setText("Collapse");
                isContentExpanded = true;
            }
        });

        // Set content
        tvTitle.setText(post.title);
        tvContent.setText(post.content);

        // Check if content needs expand button
        tvContent.post(() -> {
            if (tvContent.getLineCount() > 3) {
                btnExpand.setVisibility(View.VISIBLE);
            } else {
                btnExpand.setVisibility(View.GONE);
            }
        });

        bind();
    }

    private void openCommentsPage() {
        Intent intent = new Intent(this, CommentsActivity.class);
        intent.putExtra("post_id", post.id);
        startActivity(intent);
    }

    private void bind(){
        for (Post p : AppRepository.get().posts().getValue()){
            if (p.id.equals(post.id)){ post = p; break; }
        }

        // Update like/bookmark icons
        btnLike.setImageResource(post.liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        btnBookmark.setImageResource(post.bookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);

        // Update counts (using placeholder 9999+ for now)
        tvLikeCount.setText("9999+");
        tvBookmarkCount.setText("9999+");
        tvCommentCount.setText("9999+");

        // Load image
        if (post.imageUri != null && !post.imageUri.isEmpty()){
            Glide.with(this).load(Uri.parse(post.imageUri))
                .placeholder(R.drawable.placeholder)
                .fitCenter()
                .into(img);
        } else {
            img.setImageResource(R.drawable.placeholder);
        }
    }
}
