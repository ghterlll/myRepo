package com.aura.starter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.aura.starter.model.Post;
import com.aura.starter.util.GlideUtils;
import com.aura.starter.util.PostInteractionManager;
import java.util.ArrayList;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.VH> {
    public interface Listener { void onOpen(Post p); void onLike(Post p); void onBookmark(Post p); }
    private final Listener listener;
    private final List<Post> data = new ArrayList<>();
    private PostInteractionManager interactionManager;

    public PostAdapter(Listener l){ this.listener=l; }

    public void submit(List<Post> list){
        data.clear();
        if (list!=null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int i) {
        Post p = data.get(i);

        // Initialize interaction manager lazily
        if (interactionManager == null) {
            interactionManager = PostInteractionManager.getInstance(h.itemView.getContext());
        }

        // Load interaction state from local storage
        p.liked = interactionManager.isLiked(p.id);
        p.bookmarked = interactionManager.isBookmarked(p.id);

        h.tvTitle.setText(p.title);
        h.tvAuthor.setText(p.author);

        // Handle image display using unified GlideUtils
        GlideUtils.loadImage(h.imgCover.getContext(), p.imageUri, h.imgCover);

        h.itemView.setOnClickListener(v -> listener.onOpen(p));

        // Set icon state based on local storage
        h.btnLike.setImageResource(p.liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        h.btnBookmark.setImageResource(p.bookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);

        // Like button - update local state and UI immediately
        h.btnLike.setOnClickListener(v -> {
            // Toggle state immediately for visual feedback
            p.liked = !p.liked;
            interactionManager.setLiked(p.id, p.liked);
            h.btnLike.setImageResource(p.liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            // Notify listener to sync with backend
            listener.onLike(p);
        });

        // Bookmark button - update local state and UI immediately
        h.btnBookmark.setOnClickListener(v -> {
            // Toggle state immediately for visual feedback
            p.bookmarked = !p.bookmarked;
            interactionManager.setBookmarked(p.id, p.bookmarked);
            h.btnBookmark.setImageResource(p.bookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);
            // Notify listener to sync with backend
            listener.onBookmark(p);
        });
    }

    @Override public int getItemCount(){ return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAuthor;
        ImageView imgCover;
        ImageButton btnLike, btnBookmark;
        VH(View v){
            super(v);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvAuthor = v.findViewById(R.id.tvAuthor);
            imgCover = v.findViewById(R.id.imgCover);
            btnLike = v.findViewById(R.id.btnLike);
            btnBookmark = v.findViewById(R.id.btnBookmark);
        }
    }
}
