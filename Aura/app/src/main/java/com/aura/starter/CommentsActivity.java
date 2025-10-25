package com.aura.starter;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class CommentsActivity extends AppCompatActivity {
    private String postId;
    private RecyclerView rvComments;
    private LinearLayout layoutEmptyState;
    private EditText etComment;
    private MaterialButton btnSendComment;
    private TextView tvCommentCount;
    private CommentsAdapterNew adapter;
    private List<Comment> comments = new ArrayList<>();

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

            // TODO: Send comment to backend
            // For now, add to local list
            Comment newComment = new Comment(
                "temp_" + System.currentTimeMillis(),
                postId,
                "Current User",
                content,
                "Just now"
            );
            comments.add(0, newComment);
            adapter.submit(comments);
            updateUI();

            // Clear input
            etComment.setText("");
            Toast.makeText(this, "Comment posted!", Toast.LENGTH_SHORT).show();
        });

        // Load comments from backend
        loadComments();
    }

    private void loadComments() {
        // TODO: Load comments from backend API
        // For now, show empty state
        comments.clear();
        adapter.submit(comments);
        updateUI();
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
