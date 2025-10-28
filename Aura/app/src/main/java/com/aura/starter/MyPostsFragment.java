
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
    private FeedViewModel vm;
    private PostRepository postRepo;
    private List<Post> current = new ArrayList<>();
    private PostAdapter adapter;
    private Long currentUserId;
    private String currentCursor = null;
    private boolean hasMorePages = true;
    private boolean isLoading = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s){
        android.util.Log.d("MyPostsFragment", ">>> onCreateView called <<<");

        // Check login status
        AuthManager authManager = new AuthManager(requireContext());
        if (!authManager.isLoggedIn()) {
            android.util.Log.w("MyPostsFragment", "User not logged in, skipping data load");
            // Return empty view without loading data
            return inf.inflate(R.layout.fragment_list_posts, c, false);
        }

        View v = inf.inflate(R.layout.fragment_list_posts, c, false);
        vm = new ViewModelProvider(requireActivity()).get(FeedViewModel.class);
        postRepo = PostRepository.getInstance();

        // Get current user ID from AuthManager
        currentUserId = authManager.getUserId();
        android.util.Log.d("MyPostsFragment", "=== AUTHENTICATION STATUS ===");
        android.util.Log.d("MyPostsFragment", "Current logged-in userId: " + currentUserId);
        android.util.Log.d("MyPostsFragment", "Access token: " + (authManager.getAccessToken() != null ? "EXISTS" : "NULL"));
        android.util.Log.d("MyPostsFragment", "Is logged in: " + authManager.isLoggedIn());
        android.util.Log.d("MyPostsFragment", "============================");

        RecyclerView r = v.findViewById(R.id.recycler);
        r.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        adapter = new PostAdapter(new PostAdapter.Listener() {
            @Override public void onOpen(Post p) { Intent it = new Intent(requireContext(), PostDetailActivity.class); it.putExtra("post", p); startActivity(it); }
            @Override public void onLike(Post p) {
                vm.toggleLike(p);
            }
            @Override public void onBookmark(Post p) {
                vm.toggleBookmark(p);
            }
        });
        r.setAdapter(adapter);

        TextView sortTime = v.findViewById(R.id.btnSortTime);
        TextView sortLikes = v.findViewById(R.id.btnSortLikes);
        sortTime.setOnClickListener(btn -> sortByTime());
        sortLikes.setOnClickListener(btn -> sortByLikes());

        // Setup scroll listener for load more
        setupScrollListener(r);

        // Load my posts from backend API
        loadMyPosts();
        return v;
    }

    private void setupScrollListener(RecyclerView recyclerView) {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (isLoading || !hasMorePages) return;

                StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) return;

                int[] lastVisibleItemPositions = layoutManager.findLastVisibleItemPositions(null);
                int lastVisibleItemPosition = getMaxPosition(lastVisibleItemPositions);

                int totalItemCount = layoutManager.getItemCount();

                // Load more when user scrolls to near the end
                if (lastVisibleItemPosition >= totalItemCount - 3) {
                    loadMyPosts();
                }
            }
        });
    }

    private int getMaxPosition(int[] positions) {
        int max = positions[0];
        for (int position : positions) {
            if (position > max) {
                max = position;
            }
        }
        return max;
    }

    private void loadMyPosts() {
        if (isLoading) {
            android.util.Log.d("MyPostsFragment", "Already loading, skipping");
            return;
        }

        android.util.Log.d("MyPostsFragment", "=== START loadMyPosts ===");
        android.util.Log.d("MyPostsFragment", "Current userId: " + currentUserId);
        android.util.Log.d("MyPostsFragment", "Current cursor: " + currentCursor);

        isLoading = true;
        postRepo.listMyPosts(20, currentCursor, new PostRepository.ResultCallback<PageResponse<PostCardResponse>>() {
            @Override
            public void onSuccess(PageResponse<PostCardResponse> response) {
                mainHandler.post(() -> {
                    android.util.Log.d("MyPostsFragment", "API Response received:");
                    android.util.Log.d("MyPostsFragment", "  - Items count: " + (response.getItems() != null ? response.getItems().size() : 0));
                    android.util.Log.d("MyPostsFragment", "  - Next cursor: " + response.getNextCursor());
                    android.util.Log.d("MyPostsFragment", "  - Has more: " + response.getHasMore());

                    if (response.getItems() != null) {
                        for (int i = 0; i < response.getItems().size(); i++) {
                            PostCardResponse item = response.getItems().get(i);
                            android.util.Log.d("MyPostsFragment", "  Post " + i + ": id=" + item.getId() +
                                ", authorId=" + item.getAuthorId() +
                                ", title=" + item.getTitle());
                        }
                    }

                    List<Post> newPosts = convertPostCardResponseToPosts(response.getItems());
                    android.util.Log.d("MyPostsFragment", "Converted " + newPosts.size() + " posts");

                    if (currentCursor == null) {
                        // Initial load
                        android.util.Log.d("MyPostsFragment", "Initial load - clearing current list");
                        current.clear();
                        current.addAll(newPosts);
                    } else {
                        // Load more
                        android.util.Log.d("MyPostsFragment", "Load more - appending to current list");
                        current.addAll(newPosts);
                    }

                    android.util.Log.d("MyPostsFragment", "Total posts in current list: " + current.size());

                    currentCursor = response.getNextCursor();
                    hasMorePages = response.getHasMore();
                    isLoading = false;

                    sortByTime();
                    android.util.Log.d("MyPostsFragment", "=== END loadMyPosts SUCCESS ===");
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    android.util.Log.e("MyPostsFragment", "=== loadMyPosts FAILED ===");
                    android.util.Log.e("MyPostsFragment", "Error: " + message);
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
        android.util.Log.d("MyPostsFragment", "sortByTime - displaying " + sorted.size() + " posts");
        for (int i = 0; i < Math.min(sorted.size(), 5); i++) {
            Post p = sorted.get(i);
            android.util.Log.d("MyPostsFragment", "  Display " + i + ": id=" + p.id + ", author=" + p.author + ", title=" + p.title);
        }
        adapter.submit(sorted);
    }
    private void sortByLikes(){
        List<Post> sorted = new ArrayList<>(current);
        Collections.sort(sorted, new Comparator<Post>() {
            @Override public int compare(Post a, Post b) { return Integer.compare(b.likes, a.likes); }
        });
        android.util.Log.d("MyPostsFragment", "sortByLikes - displaying " + sorted.size() + " posts");
        for (int i = 0; i < Math.min(sorted.size(), 5); i++) {
            Post p = sorted.get(i);
            android.util.Log.d("MyPostsFragment", "  Display " + i + ": id=" + p.id + ", author=" + p.author + ", title=" + p.title);
        }
        adapter.submit(sorted);
    }
}
