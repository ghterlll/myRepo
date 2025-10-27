
package com.aura.starter;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.aura.starter.model.Post;
import com.aura.starter.network.AuthManager;
import com.aura.starter.network.PostRepository;
import com.aura.starter.network.models.PageResponse;
import com.aura.starter.network.models.PostCardResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.os.Handler;
import android.os.Looper;

public class MyPostsFragment extends Fragment {
    private static final String TAG = "MyPostsFragment_DEBUG";
    private FeedViewModel vm;
    private PostRepository postRepo;
    private List<Post> current = new ArrayList<>();
    private PostAdapter adapter;
    private Long currentUserId;
    private String currentCursor = null;
    private boolean hasMorePages = true;
    private boolean isLoading = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.e(TAG, "========== onCreate() called ==========");
        android.widget.Toast.makeText(requireContext(), "MyPosts: onCreate", android.widget.Toast.LENGTH_SHORT).show();
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s){
        android.util.Log.e(TAG, "========== onCreateView() called ==========");
        View v = inf.inflate(R.layout.fragment_list_posts, c, false);
        vm = new ViewModelProvider(requireActivity()).get(FeedViewModel.class);
        postRepo = PostRepository.getInstance();

        // Get current user ID from AuthManager
        AuthManager authManager = new AuthManager(requireContext());
        currentUserId = authManager.getUserId();

        RecyclerView r = v.findViewById(R.id.recycler);
        r.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        adapter = new PostAdapter(new PostAdapter.Listener() {
            @Override public void onOpen(Post p) { Intent it = new Intent(requireContext(), PostDetailActivity.class); it.putExtra("post", p); startActivity(it); }
            @Override public void onLike(Post p) {
                try {
                    Long postId = Long.parseLong(p.id);
                    vm.toggleLike(postId);
                } catch (NumberFormatException e) {
                    android.util.Log.e("MyPostsFragment", "Invalid post ID: " + p.id, e);
                }
            }
            @Override public void onBookmark(Post p) {
                try {
                    Long postId = Long.parseLong(p.id);
                    vm.toggleBookmark(postId);
                } catch (NumberFormatException e) {
                    android.util.Log.e("MyPostsFragment", "Invalid post ID: " + p.id, e);
                }
            }
        });
        r.setAdapter(adapter);

        TextView sortTime = v.findViewById(R.id.btnSortTime);
        TextView sortLikes = v.findViewById(R.id.btnSortLikes);
        sortTime.setOnClickListener(btn -> sortByTime());
        sortLikes.setOnClickListener(btn -> sortByLikes());

        // Load my posts from backend API
        android.util.Log.e(TAG, "========== About to call loadMyPosts() ==========");
        android.widget.Toast.makeText(requireContext(), "MyPosts: About to load data", android.widget.Toast.LENGTH_SHORT).show();
        loadMyPosts();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        android.util.Log.e(TAG, "========== onResume() called ==========");
        android.widget.Toast.makeText(requireContext(), "MyPosts: onResume", android.widget.Toast.LENGTH_SHORT).show();
    }

    private void loadMyPosts() {
        android.util.Log.e(TAG, "loadMyPosts() ENTERED - isLoading=" + isLoading);
        if (isLoading) {
            android.util.Log.e(TAG, "loadMyPosts() SKIPPED - already loading");
            return;
        }

        android.util.Log.e(TAG, "loadMyPosts() STARTING API CALL");
        android.widget.Toast.makeText(requireContext(), "MyPosts: API call starting...", android.widget.Toast.LENGTH_LONG).show();
        isLoading = true;
        postRepo.listMyPosts(20, currentCursor, new PostRepository.ResultCallback<PageResponse<PostCardResponse>>() {
            @Override
            public void onSuccess(PageResponse<PostCardResponse> response) {
                android.util.Log.e(TAG, "API SUCCESS - received " + response.getItems().size() + " posts");
                android.widget.Toast.makeText(requireContext(), "MyPosts: API SUCCESS - " + response.getItems().size() + " posts", android.widget.Toast.LENGTH_LONG).show();
                mainHandler.post(() -> {
                    List<Post> newPosts = convertPostCardResponseToPosts(response.getItems());

                    if (currentCursor == null) {
                        // Initial load
                        current.clear();
                        current.addAll(newPosts);
                    } else {
                        // Load more
                        current.addAll(newPosts);
                    }

                    currentCursor = response.getNextCursor();
                    hasMorePages = response.getHasMore();
                    isLoading = false;

                    android.util.Log.e(TAG, "About to sort and display " + current.size() + " posts");
                    sortByTime();
                });
            }

            @Override
            public void onError(String message) {
                android.util.Log.e(TAG, "API ERROR: " + message);
                android.widget.Toast.makeText(requireContext(), "MyPosts: API ERROR - " + message, android.widget.Toast.LENGTH_LONG).show();
                mainHandler.post(() -> {
                    isLoading = false;
                });
            }
        });
    }

    private List<Post> convertPostCardResponseToPosts(List<PostCardResponse> responses) {
        List<Post> posts = new ArrayList<>();
        for (PostCardResponse response : responses) {
            Post post = new Post(
                response.getId().toString(),
                "User" + response.getAuthorId(),
                response.getTitle(),
                "",
                "fitness,diet",
                response.getCoverUrl()
            );
            post.authorNickname = response.getAuthorNickname();
            
            try {
                post.createdAt = Long.parseLong(response.getCreatedAt());
            } catch (NumberFormatException e) {
                post.createdAt = System.currentTimeMillis();
            }
            posts.add(post);
        }
        return posts;
    }

    private void sortByTime(){
        List<Post> sorted = new ArrayList<>(current);
        Collections.sort(sorted, new Comparator<Post>() {
            @Override public int compare(Post a, Post b) { return Long.compare(b.createdAt, a.createdAt); }
        });
        adapter.submit(sorted);
    }
    private void sortByLikes(){
        List<Post> sorted = new ArrayList<>(current);
        Collections.sort(sorted, new Comparator<Post>() {
            @Override public int compare(Post a, Post b) { return Integer.compare(b.likes, a.likes); }
        });
        adapter.submit(sorted);
    }
}
