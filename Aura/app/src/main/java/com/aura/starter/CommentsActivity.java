package com.aura.starter;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aura.starter.model.Comment;
import com.aura.starter.network.PostRepository;
import com.aura.starter.network.models.*;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommentsActivity extends AppCompatActivity {
    private static final String TAG = "CommentsActivity";
    private String postId;
    private RecyclerView rvComments;
    private LinearLayout layoutEmptyState;
    private EditText etComment;
    private MaterialButton btnSendComment;
    private TextView tvCommentCount;
    private CommentsAdapterNew adapter;
    private List<Comment> comments = new ArrayList<>();
    private PostRepository postRepo = PostRepository.getInstance();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        postId = getIntent().getStringExtra("post_id");

        // Initialize views
        ImageButton btnBack = findViewById(R.id.btnBack);
        rvComments = findViewById(R.id.rvComments);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        etComment = findViewById(R.id.etComment);
        btnSendComment = findViewById(R.id.btnSendComment);
        tvCommentCount = findViewById(R.id.tvCommentCount);

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Setup RecyclerView
        adapter = new CommentsAdapterNew(comment -> {
            // Toggle like on comment
            comment.liked = !comment.liked;
            comment.likeCount += comment.liked ? 1 : -1;
            adapter.notifyDataSetChanged();
        });
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(adapter);

        // Enable/disable send button based on input
        etComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnSendComment.setEnabled(s.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Send comment
        btnSendComment.setOnClickListener(v -> {
            String content = etComment.getText().toString().trim();
            if (content.isEmpty()) return;

            // Disable send button
            btnSendComment.setEnabled(false);

            // Send comment to backend
            try {
                Long postIdLong = Long.parseLong(postId);
                CommentCreateRequest request = new CommentCreateRequest(content, null); // Root comment

                postRepo.createComment(postIdLong, request, new PostRepository.ResultCallback<Map<String, Long>>() {
                    @Override
                    public void onSuccess(Map<String, Long> result) {
                        runOnUiThread(() -> {
                            // Clear input
                            etComment.setText("");
                            Toast.makeText(CommentsActivity.this, "Comment posted!", Toast.LENGTH_SHORT).show();

                            // Reload comments to show the new one
                            loadComments();

                            // Re-enable send button
                            btnSendComment.setEnabled(true);
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            Toast.makeText(CommentsActivity.this, "Failed to post comment: " + message, Toast.LENGTH_SHORT).show();
                            btnSendComment.setEnabled(true);
                        });
                    }
                });
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid post ID: " + postId, e);
                Toast.makeText(this, "Invalid post ID", Toast.LENGTH_SHORT).show();
                btnSendComment.setEnabled(true);
            }
        });

        // Load comments from backend
        loadComments();
    }

    private void loadComments() {
        try {
            Long postIdLong = Long.parseLong(postId);

            // Load comments from backend
            postRepo.listComments(postIdLong, 20, null, 3, new PostRepository.ResultCallback<PageResponse<CommentThreadResponse>>() {
                @Override
                public void onSuccess(PageResponse<CommentThreadResponse> response) {
                    runOnUiThread(() -> {
                        comments.clear();

                        // Convert CommentThreadResponse to Comment model
                        for (CommentThreadResponse thread : response.getItems()) {
                            CommentResponse root = thread.getRoot();
                            Comment comment = new Comment(
                                root.getId().toString(),
                                postId,
                                "User" + root.getAuthorId(), // TODO: Get actual username
                                root.getContent(),
                                formatCreatedAt(root.getCreatedAt())
                            );
                            comments.add(comment);
                        }

                        adapter.submit(comments);
                        updateUI();
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "Failed to load comments: " + message);
                        // Show empty state on error
                        comments.clear();
                        adapter.submit(comments);
                        updateUI();
                    });
                }
            });
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid post ID: " + postId, e);
            comments.clear();
            adapter.submit(comments);
            updateUI();
        }
    }

    /**
     * Format created_at timestamp to relative time
     */
    private String formatCreatedAt(String createdAt) {
        try {
            long timestamp = Long.parseLong(createdAt);
            long now = System.currentTimeMillis();
            long diff = now - timestamp;

            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (days > 0) return days + "d ago";
            if (hours > 0) return hours + "h ago";
            if (minutes > 0) return minutes + "m ago";
            return "Just now";
        } catch (NumberFormatException e) {
            return createdAt;
        }
    }

    private void updateUI() {
        if (comments.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvComments.setVisibility(View.GONE);
            tvCommentCount.setText("0");
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvComments.setVisibility(View.VISIBLE);
            tvCommentCount.setText(String.valueOf(comments.size()));
        }
    }
}
