package com.aura.starter;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aura.starter.model.Post;
import com.aura.starter.network.PostRepository;
import com.aura.starter.network.models.CommentCreateRequest;
import com.aura.starter.util.GlideUtils;
import com.aura.starter.util.PostInteractionManager;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.util.Map;

/**
 * Fragment for displaying individual post details
 * Used within ViewPager2 for swipeable post navigation
 */
public class PostDetailFragment extends Fragment {
    private static final String TAG = "PostDetailFragment";
    private static final String ARG_POST = "post";

    private Post post;
    private ImageButton btnLike, btnBookmark, btnWriteComment;
    private ImageView img;
    private TextView tvContent, btnExpand, tvTitle, tvAuthor;
    private LinearLayout layoutCommentInput, layoutLike, layoutBookmark, layoutComment;
    private TextView tvLikeCount, tvBookmarkCount, tvCommentCount;
    private boolean isContentExpanded = false;
    private BottomSheetDialog commentDialog;
    private PostRepository postRepo = PostRepository.getInstance();
    private PostInteractionManager interactionManager;

    public static PostDetailFragment newInstance(Post post) {
        PostDetailFragment fragment = new PostDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_POST, post);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            post = (Post) getArguments().getSerializable(ARG_POST);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_post_detail, container, false);

        // Initialize interaction manager and load local state
        interactionManager = PostInteractionManager.getInstance(requireContext());
        post.liked = interactionManager.isLiked(post.id);
        post.bookmarked = interactionManager.isBookmarked(post.id);

        // Initialize views
        initializeViews(view);
        setupInteractionListeners();
        loadPostDetails();
        bind();

        return view;
    }

    private void initializeViews(View view) {
        tvTitle = view.findViewById(R.id.tvTitle);
        tvAuthor = view.findViewById(R.id.tvAuthor);
        tvContent = view.findViewById(R.id.tvContent);
        btnExpand = view.findViewById(R.id.btnExpand);
        img = view.findViewById(R.id.imgCover);

        btnLike = view.findViewById(R.id.btnLikeDetail);
        btnBookmark = view.findViewById(R.id.btnBookmarkDetail);
        btnWriteComment = view.findViewById(R.id.btnWriteComment);

        layoutCommentInput = view.findViewById(R.id.layoutCommentInput);
        layoutLike = view.findViewById(R.id.layoutLike);
        layoutBookmark = view.findViewById(R.id.layoutBookmark);
        layoutComment = view.findViewById(R.id.layoutComment);

        tvLikeCount = view.findViewById(R.id.tvLikeCount);
        tvBookmarkCount = view.findViewById(R.id.tvBookmarkCount);
        tvCommentCount = view.findViewById(R.id.tvCommentCount);

        // Set initial content
        tvTitle.setText(post.title);
        if (post.authorNickname != null && !post.authorNickname.isEmpty()) {
            tvAuthor.setText(post.authorNickname);
        } else {
            tvAuthor.setText(post.author);
        }
    }

    private void setupInteractionListeners() {
        // Like handler
        layoutLike.setOnClickListener(v -> {
            boolean newLikedState = !post.liked;
            post.liked = newLikedState;
            interactionManager.setLiked(post.id, post.liked);
            btnLike.setImageResource(post.liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

            try {
                Long postId = Long.parseLong(post.id);
                if (newLikedState) {
                    postRepo.likePost(postId, new PostRepository.ResultCallback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            Log.d(TAG, "Like synced to backend");
                        }

                        @Override
                        public void onError(String message) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    post.liked = !post.liked;
                                    interactionManager.setLiked(post.id, post.liked);
                                    btnLike.setImageResource(post.liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                                    Toast.makeText(requireContext(), "Failed to like: " + message, Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                    });
                } else {
                    postRepo.unlikePost(postId, new PostRepository.ResultCallback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            Log.d(TAG, "Unlike synced to backend");
                        }

                        @Override
                        public void onError(String message) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    post.liked = !post.liked;
                                    interactionManager.setLiked(post.id, post.liked);
                                    btnLike.setImageResource(post.liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                                    Toast.makeText(requireContext(), "Failed to unlike: " + message, Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                    });
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid post ID: " + post.id, e);
            }
        });

        // Bookmark handler
        layoutBookmark.setOnClickListener(v -> {
            boolean newBookmarkedState = !post.bookmarked;
            post.bookmarked = newBookmarkedState;
            interactionManager.setBookmarked(post.id, post.bookmarked);
            btnBookmark.setImageResource(post.bookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);

            try {
                Long postId = Long.parseLong(post.id);
                if (newBookmarkedState) {
                    postRepo.bookmarkPost(postId, new PostRepository.ResultCallback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            Log.d(TAG, "Bookmark synced to backend");
                        }

                        @Override
                        public void onError(String message) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    post.bookmarked = !post.bookmarked;
                                    interactionManager.setBookmarked(post.id, post.bookmarked);
                                    btnBookmark.setImageResource(post.bookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);
                                    Toast.makeText(requireContext(), "Failed to bookmark: " + message, Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                    });
                } else {
                    postRepo.unbookmarkPost(postId, new PostRepository.ResultCallback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            Log.d(TAG, "Unbookmark synced to backend");
                        }

                        @Override
                        public void onError(String message) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    post.bookmarked = !post.bookmarked;
                                    interactionManager.setBookmarked(post.id, post.bookmarked);
                                    btnBookmark.setImageResource(post.bookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);
                                    Toast.makeText(requireContext(), "Failed to unbookmark: " + message, Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                    });
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid post ID: " + post.id, e);
            }
        });

        // Write comment button opens bottom sheet
        btnWriteComment.setOnClickListener(v -> showWriteCommentDialog());
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
    }

    private void loadPostDetails() {
        try {
            Long postId = Long.parseLong(post.id);
            postRepo.getPostDetail(postId, new PostRepository.ResultCallback<com.aura.starter.network.models.PostDetailResponse>() {
                @Override
                public void onSuccess(com.aura.starter.network.models.PostDetailResponse response) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (response.getCaption() != null && !response.getCaption().isEmpty()) {
                                post.content = response.getCaption();
                                tvContent.setText(post.content);

                                tvContent.post(() -> {
                                    if (tvContent.getLineCount() > 3) {
                                        btnExpand.setVisibility(View.VISIBLE);
                                    } else {
                                        btnExpand.setVisibility(View.GONE);
                                    }
                                });
                            } else {
                                tvContent.setText("No description available");
                                btnExpand.setVisibility(View.GONE);
                            }
                        });
                    }
                }

                @Override
                public void onError(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Log.e(TAG, "Failed to load post details: " + message);
                            tvContent.setText("Failed to load content");
                            btnExpand.setVisibility(View.GONE);
                        });
                    }
                }
            });
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid post ID: " + post.id, e);
            tvContent.setText("Invalid post");
            btnExpand.setVisibility(View.GONE);
        }
    }

    private void showWriteCommentDialog() {
        commentDialog = new BottomSheetDialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_write_comment, null);
        commentDialog.setContentView(dialogView);

        EditText etComment = dialogView.findViewById(R.id.etComment);
        MaterialButton btnSend = dialogView.findViewById(R.id.btnSend);
        ImageButton btnClose = dialogView.findViewById(R.id.btnClose);

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

        btnSend.setOnClickListener(v -> {
            String content = etComment.getText().toString().trim();
            if (content.isEmpty()) return;

            btnSend.setEnabled(false);

            try {
                Long postIdLong = Long.parseLong(post.id);
                CommentCreateRequest request = new CommentCreateRequest(content, null);

                postRepo.createComment(postIdLong, request, new PostRepository.ResultCallback<Map<String, Long>>() {
                    @Override
                    public void onSuccess(Map<String, Long> result) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Comment sent!", Toast.LENGTH_SHORT).show();
                                commentDialog.dismiss();
                                openCommentsPage();
                            });
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Failed to send comment: " + message, Toast.LENGTH_SHORT).show();
                                btnSend.setEnabled(true);
                            });
                        }
                    }
                });
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid post ID: " + post.id, e);
                Toast.makeText(requireContext(), "Invalid post ID", Toast.LENGTH_SHORT).show();
                btnSend.setEnabled(true);
            }
        });

        btnClose.setOnClickListener(v -> commentDialog.dismiss());
        commentDialog.show();
    }

    private void openCommentsPage() {
        android.content.Intent intent = new android.content.Intent(requireContext(), CommentsActivity.class);
        intent.putExtra("post_id", post.id);
        startActivity(intent);
    }

    private void bind() {
        btnLike.setImageResource(post.liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        btnBookmark.setImageResource(post.bookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);

        tvLikeCount.setVisibility(View.GONE);
        tvBookmarkCount.setVisibility(View.GONE);
        tvCommentCount.setVisibility(View.GONE);

        RequestOptions options = new RequestOptions().fitCenter();
        GlideUtils.loadImageWithOptions(requireContext(), post.imageUri, img, options);
    }
}
