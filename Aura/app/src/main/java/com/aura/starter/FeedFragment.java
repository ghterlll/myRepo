package com.aura.starter;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FeedFragment extends Fragment {

    private FeedViewModel vm;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recycler;
    private PostAdapter adapter;
    private DraggableFloatingButton fabCreate;
    private boolean isLoading = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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
            @Override public void onLike(Post p) { vm.toggleLike(p.id); }
            @Override public void onBookmark(Post p) { vm.toggleBookmark(p.id); }
        });
        recycler.setAdapter(adapter);

        // Observe displayed posts instead of all posts
        vm.getDisplayedPosts().observe(getViewLifecycleOwner(), posts -> {
            if (posts != null) {
                adapter.submit(posts);
            }
        });
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                // Refresh posts - load random 10 posts
                vm.refreshPosts();
                // Stop refresh animation after a short delay
                mainHandler.postDelayed(() -> {
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }, 1000);
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

                if (isLoading || !vm.hasMorePages()) return;

                StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) return;

                int[] lastVisibleItemPositions = layoutManager.findLastVisibleItemPositions(null);
                int lastVisibleItemPosition = getMaxPosition(lastVisibleItemPositions);

                int totalItemCount = layoutManager.getItemCount();

                // Load more when user scrolls to near the end
                if (lastVisibleItemPosition >= totalItemCount - 3) {
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
        if (isLoading || !vm.hasMorePages()) return;

        isLoading = true;

        // Simulate loading delay
        executor.execute(() -> {
            try {
                Thread.sleep(500); // Simulate network delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            mainHandler.post(() -> {
                vm.loadMorePosts();
                isLoading = false;
            });
        });
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
        TextInputEditText etSearch = view.findViewById(R.id.etSearch);

        // Add click listener to open search page
        etSearch.setOnClickListener(v -> {
            // Navigate to search activity
            Intent intent = new Intent(requireContext(), SearchActivity.class);
            startActivity(intent);

            // Add visual feedback (optional)
            v.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start();
                })
                .start();
        });

        // Make the search field a navigation button (not editable)
        etSearch.setFocusable(false);
        etSearch.setClickable(true);
        etSearch.setCursorVisible(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
