package com.aura.starter;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.aura.starter.model.Post;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Search results activity showing filtered posts based on search query
 * Uses the same layout as feed but with filtered results and pagination support
 */
public class SearchResultsActivity extends AppCompatActivity {

    private FeedViewModel viewModel;
    private TextInputEditText etSearch;
    private RecyclerView recycler;
    private PostAdapter adapter;
    private TextView tvNoResults;

    private String searchQuery;
    private boolean isLoading = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        // Get search query from intent
        searchQuery = getIntent().getStringExtra("search_query");
        if (TextUtils.isEmpty(searchQuery)) {
            finish(); // No search query, close activity
            return;
        }

        initializeViews();
        setupRecyclerView();
        // Note: Removed setupSwipeRefresh() call for search results page as per requirements
        setupSearchFunctionality();
        performInitialSearch();
    }

    private void initializeViews() {
        etSearch = findViewById(R.id.etSearch);
        tvNoResults = findViewById(R.id.tvNoResults);
        recycler = findViewById(R.id.recycler);
        // Note: Removed SwipeRefreshLayout for search results page as per requirements
    }

    private void setupRecyclerView() {
        recycler.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        adapter = new PostAdapter(new PostAdapter.Listener() {
            @Override public void onOpen(Post post) {
                Intent intent = new Intent(SearchResultsActivity.this, PostDetailActivity.class);
                intent.putExtra("post", post);
                startActivity(intent);
            }
            @Override public void onLike(Post post) {
                try {
                    Long postId = Long.parseLong(post.id);
                    viewModel.toggleLike(postId);
                } catch (NumberFormatException e) {
                    android.util.Log.e("SearchResultsActivity", "Invalid post ID: " + post.id, e);
                }
            }
            @Override public void onBookmark(Post post) {
                try {
                    Long postId = Long.parseLong(post.id);
                    viewModel.toggleBookmark(postId);
                } catch (NumberFormatException e) {
                    android.util.Log.e("SearchResultsActivity", "Invalid post ID: " + post.id, e);
                }
            }
        });
        recycler.setAdapter(adapter);

        // Setup scroll listener for load more
        setupScrollListener();
    }

    // Note: SwipeRefreshLayout functionality removed for search results page as per requirements

    private void setupScrollListener() {
        recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (isLoading) return;

                StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) return;

                int[] lastVisibleItemPositions = layoutManager.findLastVisibleItemPositions(null);
                int lastVisibleItemPosition = getMaxPosition(lastVisibleItemPositions);

                int totalItemCount = layoutManager.getItemCount();

                // Load more when user scrolls to near the end (only if we have more pages)
                if (lastVisibleItemPosition >= totalItemCount - 3 && viewModel.hasMorePages()) {
                    loadMorePosts();
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

    private void loadMorePosts() {
        if (isLoading || !viewModel.hasMorePages()) return;

        isLoading = true;

        // Simulate loading delay
        executor.execute(() -> {
            try {
                Thread.sleep(500); // Simulate network delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            mainHandler.post(() -> {
                viewModel.loadMorePosts();
                isLoading = false;
            });
        });
    }

    private void setupSearchFunctionality() {
        // Set current search query in search box
        etSearch.setText(searchQuery);

        // Handle search action (IME search button) - NOT ALLOWED in results page
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            // Do not allow searching from results page - return to search page instead
            returnToSearchPage(true); // Keep current text
            return true;
        });

        // Handle clear button - return to search page with empty text
        findViewById(R.id.btnClear).setOnClickListener(v -> {
            returnToSearchPage(false); // Clear text
        });

        // Handle back button - return to search page with empty text
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            returnToSearchPage(false); // Clear text
        });

        // Handle search text click - return to search page with current text
        etSearch.setOnClickListener(v -> {
            returnToSearchPage(true); // Keep current text
        });
    }

    /**
     * Return to search page with specified text clearing behavior
     * @param keepText true to keep current search text, false to clear it
     */
    private void returnToSearchPage(boolean keepText) {
        Intent intent = new Intent(this, SearchActivity.class);
        if (keepText) {
            intent.putExtra("current_search", etSearch.getText().toString());
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void performInitialSearch() {
        viewModel = new ViewModelProvider(this).get(FeedViewModel.class);

        // Observe displayed posts and filter based on search query
        viewModel.getDisplayedPosts().observe(this, posts -> {
            if (posts == null) return;

            List<Post> filteredPosts = new ArrayList<>();
            String lowerQuery = searchQuery.toLowerCase();

            for (Post post : posts) {
                String searchText = (post.title + " " + post.content + " " + post.tags).toLowerCase();
                if (searchText.contains(lowerQuery)) {
                    filteredPosts.add(post);
                }
            }

            updateResults(filteredPosts);
        });
    }

    private void updateResults(List<Post> results) {
        if (results.isEmpty()) {
            recycler.setVisibility(View.GONE);
            tvNoResults.setVisibility(View.VISIBLE);
            tvNoResults.setText("No results found for \"" + searchQuery + "\"");
        } else {
            recycler.setVisibility(View.VISIBLE);
            tvNoResults.setVisibility(View.GONE);
            adapter.submit(results);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
