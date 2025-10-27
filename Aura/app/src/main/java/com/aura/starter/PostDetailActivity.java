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
import com.aura.starter.util.GlideUtils;
import com.aura.starter.util.PostInteractionManager;
import com.bumptech.glide.request.RequestOptions;
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
    private PostInteractionManager interactionManager;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        post = (Post)getIntent().getSerializableExtra("post");

        // Initialize interaction manager and load local state
        interactionManager = PostInteractionManager.getInstance(this);
        post.liked = interactionManager.isLiked(post.id);
        post.bookmarked = interactionManager.isBookmarked(post.id);

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

        // Like handler - immediate UI update + backend sync
        layoutLike.setOnClickListener(v -> {
            // Toggle local state immediately for instant feedback
            boolean newLikedState = !post.liked;
            post.liked = newLikedState;
            interactionManager.setLiked(post.id, post.liked);

            // Update UI immediately
            btnLike.setImageResource(post.liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

            // Sync with backend - call like or unlike based on new state
            try {
                Long postId = Long.parseLong(post.id);

                if (newLikedState) {
                    // User is liking the post
                    postRepo.likePost(postId, new PostRepository.ResultCallback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            Log.d(TAG, "Like synced to backend");
                        }

                        @Override
                        public void onError(String message) {
                            // Failed - revert local state
                            runOnUiThread(() -> {
                                post.liked = !post.liked;
                                interactionManager.setLiked(post.id, post.liked);
                                btnLike.setImageResource(post.liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                                Toast.makeText(PostDetailActivity.this, "Failed to like: " + message, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                } else {
                    // User is unliking the post
                    postRepo.unlikePost(postId, new PostRepository.ResultCallback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            Log.d(TAG, "Unlike synced to backend");
                        }

                        @Override
                        public void onError(String message) {
                            // Failed - revert local state
                            runOnUiThread(() -> {
                                post.liked = !post.liked;
                                interactionManager.setLiked(post.id, post.liked);
                                btnLike.setImageResource(post.liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                                Toast.makeText(PostDetailActivity.this, "Failed to unlike: " + message, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid post ID: " + post.id, e);
            }
        });

        // Bookmark handler - immediate UI update + backend sync
        layoutBookmark.setOnClickListener(v -> {
            // Toggle local state immediately for instant feedback
            boolean newBookmarkedState = !post.bookmarked;
            post.bookmarked = newBookmarkedState;
            interactionManager.setBookmarked(post.id, post.bookmarked);

            // Update UI immediately
            btnBookmark.setImageResource(post.bookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);

            // Sync with backend - call bookmark or unbookmark based on new state
            try {
                Long postId = Long.parseLong(post.id);

                if (newBookmarkedState) {
                    // User is bookmarking the post
                    postRepo.bookmarkPost(postId, new PostRepository.ResultCallback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            Log.d(TAG, "Bookmark synced to backend");
                        }

                        @Override
                        public void onError(String message) {
                            // Failed - revert local state
                            runOnUiThread(() -> {
                                post.bookmarked = !post.bookmarked;
                                interactionManager.setBookmarked(post.id, post.bookmarked);
                                btnBookmark.setImageResource(post.bookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);
                                Toast.makeText(PostDetailActivity.this, "Failed to bookmark: " + message, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                } else {
                    // User is unbookmarking the post
                    postRepo.unbookmarkPost(postId, new PostRepository.ResultCallback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            Log.d(TAG, "Unbookmark synced to backend");
                        }

                        @Override
                        public void onError(String message) {
                            // Failed - revert local state
                            runOnUiThread(() -> {
                                post.bookmarked = !post.bookmarked;
                                interactionManager.setBookmarked(post.id, post.bookmarked);
                                btnBookmark.setImageResource(post.bookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);
                                Toast.makeText(PostDetailActivity.this, "Failed to unbookmark: " + message, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid post ID: " + post.id, e);
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
                CommentCreateRequest request = new CommentCreateRequest(content, null); // Root comment

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

        // Hide counts until backend provides them (better UX than showing "9999+")
        tvLikeCount.setVisibility(View.GONE);
        tvBookmarkCount.setVisibility(View.GONE);
        tvCommentCount.setVisibility(View.GONE);

        // Load image using unified GlideUtils with fitCenter option
        RequestOptions options = new RequestOptions().fitCenter();
        GlideUtils.loadImageWithOptions(this, post.imageUri, img, options);
    }
}
