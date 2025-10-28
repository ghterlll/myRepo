
package com.aura.starter;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.aura.starter.model.Post;
import com.aura.starter.network.AuthManager;
import com.aura.starter.network.PostRepository;
import com.aura.starter.network.models.PageResponse;
import com.aura.starter.network.models.PostCardResponse;
import com.aura.starter.util.PostInteractionManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BookmarksFragment extends Fragment {
    private FeedViewModel vm;
    private PostRepository postRepo;
    private List<Post> current = new ArrayList<>();
    private PostAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recycler;
    private boolean isLoading = false;
    private String currentCursor = null;
    private boolean hasMorePages = true;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private PostInteractionManager interactionManager;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s){
        android.util.Log.d("BookmarksFragment", ">>> onCreateView called <<<");

        // Check login status
        AuthManager authManager = new AuthManager(requireContext());
        if (!authManager.isLoggedIn()) {
            android.util.Log.w("BookmarksFragment", "User not logged in, skipping data load");
            // Return empty view without loading data
            return inf.inflate(R.layout.fragment_list_posts, c, false);
        }

        View v = inf.inflate(R.layout.fragment_list_posts, c, false);
        vm = new ViewModelProvider(requireActivity()).get(FeedViewModel.class);
        postRepo = PostRepository.getInstance();

        // Initialize interaction manager
        interactionManager = PostInteractionManager.getInstance(requireContext());

        // Setup views
        setupViews(v);

        // Setup RecyclerView
        setupRecyclerView(v);

        // Setup SwipeRefreshLayout for pull-to-refresh
        setupSwipeRefresh(v);

        // Setup scroll listener for load more
        setupScrollListener();

        // Setup sorting buttons
        setupSortingButtons(v);

        // Load bookmarked posts from backend API
        loadBookmarkedPosts();

        return v;
    }

    private void loadBookmarkedPosts() {
        if (isLoading) {
            android.util.Log.d("BookmarksFragment", "Already loading, skipping");
            return;
        }

        android.util.Log.d("BookmarksFragment", "=== START loadBookmarkedPosts ===");
        android.util.Log.d("BookmarksFragment", "Current cursor: " + currentCursor);

        isLoading = true;
        postRepo.listBookmarkedPosts(20, currentCursor, new PostRepository.ResultCallback<PageResponse<PostCardResponse>>() {
            @Override
            public void onSuccess(PageResponse<PostCardResponse> response) {
                mainHandler.post(() -> {
                    android.util.Log.d("BookmarksFragment", "API Response received:");
                    android.util.Log.d("BookmarksFragment", "  - Items count: " + (response.getItems() != null ? response.getItems().size() : 0));
                    android.util.Log.d("BookmarksFragment", "  - Next cursor: " + response.getNextCursor());
                    android.util.Log.d("BookmarksFragment", "  - Has more: " + response.getHasMore());

                    if (response.getItems() != null) {
                        for (int i = 0; i < response.getItems().size(); i++) {
                            PostCardResponse item = response.getItems().get(i);
                            android.util.Log.d("BookmarksFragment", "  Bookmark " + i + ": id=" + item.getId() +
                                ", authorId=" + item.getAuthorId() +
                                ", title=" + item.getTitle());
                        }
                    }

                    List<Post> newPosts = convertPostCardResponseToPosts(response.getItems());
                    android.util.Log.d("BookmarksFragment", "Converted " + newPosts.size() + " bookmarked posts");

                    if (currentCursor == null) {
                        // Initial load
                        android.util.Log.d("BookmarksFragment", "Initial load - clearing current list");
                        current.clear();
                        current.addAll(newPosts);
                    } else {
                        // Load more
                        android.util.Log.d("BookmarksFragment", "Load more - appending to current list");
                        current.addAll(newPosts);
                    }

                    android.util.Log.d("BookmarksFragment", "Total bookmarks in current list: " + current.size());

                    currentCursor = response.getNextCursor();
                    hasMorePages = response.getHasMore();
                    isLoading = false;

                    sortByTime();
                    android.util.Log.d("BookmarksFragment", "=== END loadBookmarkedPosts SUCCESS ===");
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    android.util.Log.e("BookmarksFragment", "=== loadBookmarkedPosts FAILED ===");
                    android.util.Log.e("BookmarksFragment", "Error: " + message);
                    isLoading = false;
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
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
            post.bookmarked = true; // All posts from bookmarks API are bookmarked
            
            try {
                post.createdAt = Long.parseLong(response.getCreatedAt());
            } catch (NumberFormatException e) {
                post.createdAt = System.currentTimeMillis();
            }
            posts.add(post);
        }
        return posts;
    }

    private void setupViews(View v) {
        swipeRefreshLayout = v.findViewById(R.id.swipeRefreshLayout);
        recycler = v.findViewById(R.id.recycler);
    }

    private void setupRecyclerView(View v) {
        recycler.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        adapter = new PostAdapter(new PostAdapter.Listener() {
            @Override public void onOpen(Post p) { Intent it = new Intent(requireContext(), PostDetailActivity.class); it.putExtra("post", p); startActivity(it); }
            @Override public void onLike(Post p) {
                vm.toggleLike(p);
            }
            @Override public void onBookmark(Post p) {
                vm.toggleBookmark(p);
            }
        });
        recycler.setAdapter(adapter);
    }

    private void setupSwipeRefresh(View v) {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                // Refresh bookmarks - reload from backend
                currentCursor = null;
                hasMorePages = true;
                loadBookmarkedPosts();
            });

            // Set refresh colors
            swipeRefreshLayout.setColorSchemeResources(
                R.color.auragreen_primary,
                R.color.auragreen_primary_dark,
                R.color.auragreen_secondary
            );
        }
    }

    private void setupScrollListener() {
        recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
                    loadBookmarkedPosts();
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

    private void setupSortingButtons(View v) {
        TextView sortTime = v.findViewById(R.id.btnSortTime);
        TextView sortLikes = v.findViewById(R.id.btnSortLikes);
        sortTime.setOnClickListener(btn -> sortByTime());
        sortLikes.setOnClickListener(btn -> sortByLikes());
    }

    private void sortByTime(){
        List<Post> sorted = new ArrayList<>(current);
        Collections.sort(sorted, new Comparator<Post>() {
            @Override public int compare(Post a, Post b) { return Long.compare(b.createdAt, a.createdAt); }
        });
        android.util.Log.d("BookmarksFragment", "sortByTime - displaying " + sorted.size() + " bookmarked posts");
        for (int i = 0; i < Math.min(sorted.size(), 5); i++) {
            Post p = sorted.get(i);
            android.util.Log.d("BookmarksFragment", "  Display " + i + ": id=" + p.id + ", author=" + p.author + ", title=" + p.title + ", bookmarked=" + p.bookmarked);
        }
        adapter.submit(sorted);
    }
    private void sortByLikes(){
        List<Post> sorted = new ArrayList<>(current);
        Collections.sort(sorted, new Comparator<Post>() {
            @Override public int compare(Post a, Post b) { return Integer.compare(b.likes, a.likes); }
        });
        android.util.Log.d("BookmarksFragment", "sortByLikes - displaying " + sorted.size() + " bookmarked posts");
        for (int i = 0; i < Math.min(sorted.size(), 5); i++) {
            Post p = sorted.get(i);
            android.util.Log.d("BookmarksFragment", "  Display " + i + ": id=" + p.id + ", author=" + p.author + ", title=" + p.title + ", bookmarked=" + p.bookmarked);
        }
        adapter.submit(sorted);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
