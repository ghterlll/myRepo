
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
        loadMyPosts();
        return v;
    }

    private void loadMyPosts() {
        if (isLoading) return;
        
        isLoading = true;
        postRepo.listMyPosts(20, currentCursor, new PostRepository.ResultCallback<PageResponse<PostCardResponse>>() {
            @Override
            public void onSuccess(PageResponse<PostCardResponse> response) {
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
                    
                    sortByTime();
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    android.util.Log.e("MyPostsFragment", "Load my posts failed: " + message);
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
