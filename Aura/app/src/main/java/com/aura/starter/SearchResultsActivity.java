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
import com.aura.starter.network.PostRepository;
import com.aura.starter.network.models.PageResponse;
import com.aura.starter.network.models.PostCardResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Search results activity showing filtered posts based on search query or tag
 * Supports two modes:
 * - Keyword search: search_query extra
 * - Tag search: search_mode="TAG", tag_id, tag_name extras
 * Uses the same layout as feed but with filtered results and pagination support
 */
public class SearchResultsActivity extends AppCompatActivity {

    private FeedViewModel viewModel;
    private PostRepository postRepo = PostRepository.getInstance();
    private TextView etSearch;
    private RecyclerView recycler;
    private PostAdapter adapter;
    private TextView tvNoResults;

    // Search mode fields
    private enum SearchMode { KEYWORD, TAG }
    private SearchMode searchMode = SearchMode.KEYWORD;
    private String searchQuery;
    private Long tagId;
    private String tagName;

    private String currentCursor = null;
    private boolean hasMorePages = true;
    private boolean isLoading = false;
    private List<Post> currentPosts = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        // Determine search mode from intent
        Intent intent = getIntent();
        String mode = intent.getStringExtra("search_mode");

        if ("TAG".equals(mode)) {
            // Tag search mode
            searchMode = SearchMode.TAG;
            tagId = intent.getLongExtra("tag_id", -1L);
            tagName = intent.getStringExtra("tag_name");

            if (tagId == -1L || TextUtils.isEmpty(tagName)) {
                finish(); // Invalid tag data
                return;
            }
        } else {
            // Keyword search mode (default)
            searchMode = SearchMode.KEYWORD;
            searchQuery = intent.getStringExtra("search_query");

            if (TextUtils.isEmpty(searchQuery)) {
                finish(); // No search query, close activity
                return;
            }
        }

        initializeViews();
        setupViewModel();
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

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(FeedViewModel.class);
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
                viewModel.toggleLike(post);
            }
            @Override public void onBookmark(Post post) {
                viewModel.toggleBookmark(post);
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
                if (lastVisibleItemPosition >= totalItemCount - 3 && hasMorePages) {
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
        if (isLoading || !hasMorePages) return;

        isLoading = true;

        // Create callback for handling results
        PostRepository.ResultCallback<PageResponse<PostCardResponse>> callback = new PostRepository.ResultCallback<PageResponse<PostCardResponse>>() {
            @Override
            public void onSuccess(PageResponse<PostCardResponse> response) {
                runOnUiThread(() -> {
                    // Append to existing posts
                    for (PostCardResponse postCard : response.getItems()) {
                        Post post = new Post(
                            postCard.getId().toString(),
                            "User" + postCard.getAuthorId(),
                            postCard.getTitle(),
                            "",
                            "",
                            postCard.getCoverUrl()
                        );
                        post.authorNickname = postCard.getAuthorNickname();
                        currentPosts.add(post);
                    }

                    // Update pagination state
                    currentCursor = response.getNextCursor();
                    hasMorePages = response.getHasMore();
                    isLoading = false;

                    adapter.submit(currentPosts);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    android.util.Log.e("SearchResultsActivity", "Load more failed: " + message);
                    isLoading = false;
                });
            }
        };

        // Load next page based on search mode
        if (searchMode == SearchMode.TAG) {
            postRepo.getPostsByTag(tagId, 20, currentCursor, callback);
        } else {
            postRepo.searchPosts(searchQuery, null, 20, currentCursor, callback);
        }
    }

    private void setupSearchFunctionality() {
        // Set current search query/tag in search box
        if (searchMode == SearchMode.TAG) {
            etSearch.setText("#" + tagName);
        } else {
            etSearch.setText(searchQuery);
        }

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
        isLoading = true;
        currentCursor = null;
        hasMorePages = true;
        currentPosts.clear(); // Clear previous search results

        // Create callback for handling results
        PostRepository.ResultCallback<PageResponse<PostCardResponse>> callback = new PostRepository.ResultCallback<PageResponse<PostCardResponse>>() {
            @Override
            public void onSuccess(PageResponse<PostCardResponse> response) {
                runOnUiThread(() -> {
                    // Convert PostCardResponse to Post model
                    for (PostCardResponse postCard : response.getItems()) {
                        Post post = new Post(
                            postCard.getId().toString(),
                            "User" + postCard.getAuthorId(), // Keep for backward compatibility
                            postCard.getTitle(),
                            "", // PostCardResp doesn't have content
                            "", // TODO: Get tags from backend
                            postCard.getCoverUrl()
                        );
                        post.authorNickname = postCard.getAuthorNickname();
                        currentPosts.add(post);
                    }

                    // Update pagination state
                    currentCursor = response.getNextCursor();
                    hasMorePages = response.getHasMore();
                    isLoading = false;

                    updateResults(currentPosts);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    android.util.Log.e("SearchResultsActivity", "Search failed: " + message);
                    isLoading = false;
                    updateResults(new ArrayList<>());
                });
            }
        };

        // Perform search based on mode
        if (searchMode == SearchMode.TAG) {
            postRepo.getPostsByTag(tagId, 20, currentCursor, callback);
        } else {
            postRepo.searchPosts(searchQuery, null, 20, currentCursor, callback);
        }
    }

    private void updateResults(List<Post> results) {
        if (results.isEmpty()) {
            recycler.setVisibility(View.GONE);
            tvNoResults.setVisibility(View.VISIBLE);

            // Show appropriate message based on search mode
            if (searchMode == SearchMode.TAG) {
                tvNoResults.setText("No posts found for tag #" + tagName);
            } else {
                tvNoResults.setText("No results found for \"" + searchQuery + "\"");
            }
        } else {
            recycler.setVisibility(View.VISIBLE);
            tvNoResults.setVisibility(View.GONE);
            adapter.submit(results);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh adapter to update interaction states from SharedPreferences
        // This ensures that like/bookmark changes made in PostDetailActivity are reflected
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
