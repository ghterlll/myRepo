package com.aura.starter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.aura.starter.data.AppRepository;
import com.aura.starter.model.Post;
import com.aura.starter.network.PostRepository;
import com.aura.starter.network.models.CommentCreateRequest;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.util.Map;

public class PostDetailActivity extends AppCompatActivity {
    private static final String TAG = "PostDetailActivity";
    private Post post;
    private ImageButton btnLike, btnBookmark, btnBack, btnWriteComment;
    private ImageView img;
    private TextView tvContent, btnExpand;
    private LinearLayout layoutCommentInput, layoutLike, layoutBookmark, layoutComment;
    private TextView tvLikeCount, tvBookmarkCount, tvCommentCount;
    private boolean isContentExpanded = false;
    private BottomSheetDialog commentDialog;
    private PostRepository postRepo = PostRepository.getInstance();

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
        btnWriteComment = findViewById(R.id.btnWriteComment);

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
        layoutLike.setOnClickListener(v -> {
            try {
                Long postId = Long.parseLong(post.id);
                FeedViewModel feedVm = new androidx.lifecycle.ViewModelProvider(this).get(FeedViewModel.class);
                feedVm.toggleLike(postId);
                // Refresh the current post data
                bind();
            } catch (NumberFormatException e) {
                android.util.Log.e("PostDetailActivity", "Invalid post ID: " + post.id, e);
            }
        });
        layoutBookmark.setOnClickListener(v -> {
            try {
                Long postId = Long.parseLong(post.id);
                FeedViewModel feedVm = new androidx.lifecycle.ViewModelProvider(this).get(FeedViewModel.class);
                feedVm.toggleBookmark(postId);
                // Refresh the current post data
                bind();
            } catch (NumberFormatException e) {
                android.util.Log.e("PostDetailActivity", "Invalid post ID: " + post.id, e);
            }
        });

        // Write comment button opens bottom sheet
        btnWriteComment.setOnClickListener(v -> showWriteCommentDialog());

        // Blank box also opens bottom sheet
        layoutCommentInput.setOnClickListener(v -> showWriteCommentDialog());

        // Comment count click opens comment page
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

    private void showWriteCommentDialog() {
        commentDialog = new BottomSheetDialog(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_write_comment, null);
        commentDialog.setContentView(dialogView);

        EditText etComment = dialogView.findViewById(R.id.etComment);
        MaterialButton btnSend = dialogView.findViewById(R.id.btnSend);
        ImageButton btnClose = dialogView.findViewById(R.id.btnClose);

        // Enable/disable send button based on input
        etComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnSend.setEnabled(s.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Send comment
        btnSend.setOnClickListener(v -> {
            String content = etComment.getText().toString().trim();
            if (content.isEmpty()) return;

            // Disable send button
            btnSend.setEnabled(false);

            // Send comment to backend
            try {
                Long postIdLong = Long.parseLong(post.id);
                CommentCreateRequest request = new CommentCreateRequest();
                request.setContent(content);
                request.setParentId(null); // Root comment

                postRepo.createComment(postIdLong, request, new PostRepository.ResultCallback<Map<String, Long>>() {
                    @Override
                    public void onSuccess(Map<String, Long> result) {
                        runOnUiThread(() -> {
                            Toast.makeText(PostDetailActivity.this, "Comment sent!", Toast.LENGTH_SHORT).show();
                            commentDialog.dismiss();

                            // Jump to comments page to show the new comment
                            openCommentsPage();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            Toast.makeText(PostDetailActivity.this, "Failed to send comment: " + message, Toast.LENGTH_SHORT).show();
                            btnSend.setEnabled(true);
                        });
                    }
                });
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid post ID: " + post.id, e);
                Toast.makeText(this, "Invalid post ID", Toast.LENGTH_SHORT).show();
                btnSend.setEnabled(true);
            }
        });

        // Close button
        btnClose.setOnClickListener(v -> commentDialog.dismiss());

        commentDialog.show();
    }

    private void openCommentsPage() {
        Intent intent = new Intent(this, CommentsActivity.class);
        intent.putExtra("post_id", post.id);
        startActivity(intent);
    }

    private void bind(){
        // Update like/bookmark icons
        btnLike.setImageResource(post.liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        btnBookmark.setImageResource(post.bookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);

        // Update counts (using placeholder 9999+ for now)
        // TODO: Get real counts from backend API
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
