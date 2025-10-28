package com.aura.starter;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.aura.starter.model.Post;
import com.aura.starter.widget.DraggableFloatingButton;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.TextView;

public class FeedFragment extends Fragment {

    private FeedViewModel vm;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recycler;
    private PostAdapter adapter;
    private DraggableFloatingButton fabCreate;
    private TextView etSearch;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_feed, container, false);
        vm = new ViewModelProvider(requireActivity()).get(FeedViewModel.class);

        // Setup views
        setupViews(v);

        // Setup search functionality
        setupSearchBar(v);

        // Setup RecyclerView
        setupRecyclerView();

        // Setup SwipeRefreshLayout for pull-to-refresh
        setupSwipeRefresh();

        // Setup scroll listener for load more
        setupScrollListener();

        // Setup floating action button
        setupFloatingButton(v);

        return v;
    }

    private void setupViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        recycler = view.findViewById(R.id.recycler);
        fabCreate = view.findViewById(R.id.fabCreate);
    }

    private void setupRecyclerView() {
        recycler.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        adapter = new PostAdapter(new PostAdapter.Listener() {
            @Override public void onOpen(Post p) {
                Intent it = new Intent(requireContext(), PostDetailActivity.class);
                it.putExtra("post", p);
                startActivity(it);
            }
            @Override public void onLike(Post p) {
                // Pass the post object so ViewModel can check current state
                vm.toggleLike(p);
            }
            @Override public void onBookmark(Post p) {
                // Pass the post object so ViewModel can check current state
                vm.toggleBookmark(p);
            }
        });
        recycler.setAdapter(adapter);

        // Observe displayed posts
        vm.getDisplayedPosts().observe(getViewLifecycleOwner(), posts -> {
            if (posts != null) {
                adapter.submit(posts);
            }
        });

        // Observe loading state for UI updates
        vm.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // Update UI based on loading state if needed
            android.util.Log.d("FeedFragment", "Loading state: " + isLoading);
        });

        // Observe pagination state
        vm.getHasMorePages().observe(getViewLifecycleOwner(), hasMore -> {
            // Update UI based on pagination state if needed
            android.util.Log.d("FeedFragment", "Has more pages: " + hasMore);
        });
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                // Refresh posts - reload from backend
                vm.refreshPosts();
                // Observe loading state to stop refresh animation
                observeLoadingState();
            });

            // Set refresh colors
            swipeRefreshLayout.setColorSchemeResources(
                R.color.auragreen_primary,
                R.color.auragreen_primary_dark,
                R.color.auragreen_secondary
            );
        }
    }

    private void observeLoadingState() {
        vm.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (swipeRefreshLayout != null) {
                // When loading is complete, stop refresh animation
                if (!isLoading) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
    }

    private void setupScrollListener() {
        recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // Check if we should load more posts
                if (shouldLoadMorePosts(recyclerView)) {
                    vm.loadMorePosts();
                }
            }
        });
    }

    private boolean shouldLoadMorePosts(RecyclerView recyclerView) {
        StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager == null) return false;

        int[] lastVisibleItemPositions = layoutManager.findLastVisibleItemPositions(null);
        int lastVisibleItemPosition = getMaxPosition(lastVisibleItemPositions);
        int totalItemCount = layoutManager.getItemCount();

        // Load more when user scrolls to near the end (3 items before the end)
        Boolean hasMore = vm.getHasMorePages().getValue();
        Boolean isLoading = vm.getIsLoading().getValue();

        return lastVisibleItemPosition >= totalItemCount - 3 &&
               (hasMore == null || hasMore) &&
               (isLoading == null || !isLoading);
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
        // 移除这个方法，因为现在ViewModel直接处理API调用
        // vm.loadMorePosts() 会在shouldLoadMorePosts()中被调用
    }

    /**
     * Setup floating action button for creating posts
     */
    private void setupFloatingButton(View view) {
        if (fabCreate != null) {
            fabCreate.setOnClickListener(v -> {
                // Navigate to CreateFragment
                getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new CreateFragment())
                    .addToBackStack(null)
                    .commit();
            });
        }
    }

    /**
     * Setup search bar functionality
     */
    private void setupSearchBar(View view) {
        View searchBar = view.findViewById(R.id.searchBar);
        etSearch = view.findViewById(R.id.etSearch);

        // Click listener for opening search activity
        View.OnClickListener openSearchListener = v -> {
            Intent intent = new Intent(requireContext(), SearchActivity.class);
            startActivity(intent);
        };

        // Add click listener to both searchBar and etSearch
        searchBar.setOnClickListener(openSearchListener);
        etSearch.setOnClickListener(openSearchListener);

        // Make sure etSearch doesn't block clicks (TextView, not EditText)
        etSearch.setFocusable(false);
        etSearch.setClickable(true);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        // No cleanup needed for ViewModel-based approach
    }
}
