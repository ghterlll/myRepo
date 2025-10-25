
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MyPostsFragment extends Fragment {
    private FeedViewModel vm;
    private List<Post> current = new ArrayList<>();
    private PostAdapter adapter;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s){
        View v = inf.inflate(R.layout.fragment_list_posts, c, false);
        vm = new ViewModelProvider(requireActivity()).get(FeedViewModel.class);

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

        // For now, observe all posts and filter for current user
        // TODO: Add backend API to get posts by user ID
        vm.getDisplayedPosts().observe(getViewLifecycleOwner(), all -> {
            List<Post> list = new ArrayList<>();
            if (all != null) for (Post p : all) if ("You".equals(p.author)) list.add(p);
            current = list;
            sortByTime();
        });
        return v;
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
