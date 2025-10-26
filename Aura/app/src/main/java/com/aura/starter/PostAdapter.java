package com.aura.starter;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.aura.starter.model.Post;
import com.bumptech.glide.Glide;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.VH> {
    public interface Listener { void onOpen(Post p); void onLike(Post p); void onBookmark(Post p); }
    private final Listener listener;
    private final List<Post> data = new ArrayList<>();
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
        h.tvTitle.setText(p.title);
        h.tvAuthor.setText(p.author);

        // Handle image display - Simplified logic with proper URL support
        if (p.imageUri != null && !p.imageUri.isEmpty()){
            // Priority 1: HTTP/HTTPS URLs (from MinIO or other web sources)
            if (p.imageUri.startsWith("http://") || p.imageUri.startsWith("https://")) {
                Glide.with(h.imgCover.getContext())
                    .load(p.imageUri)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .into(h.imgCover);
            }
            // Priority 2: Local file paths
            else if (p.imageUri.startsWith("/") || p.imageUri.startsWith("file://")) {
                Glide.with(h.imgCover.getContext())
                    .load(new File(p.imageUri.replace("file://", "")))
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .into(h.imgCover);
            }
            // Priority 3: Assets images (imgX format)
            else if (p.imageUri.startsWith("img") && p.imageUri.length() <= 5) {
                try {
                    AssetManager assetManager = h.imgCover.getContext().getAssets();
                    InputStream inputStream = assetManager.open("images/" + p.imageUri + ".png");
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();
                    if (bitmap != null) {
                        Glide.with(h.imgCover.getContext())
                            .load(bitmap)
                            .placeholder(R.drawable.placeholder)
                            .into(h.imgCover);
                    } else {
                        h.imgCover.setImageResource(R.drawable.placeholder);
                    }
                } catch (IOException e) {
                    h.imgCover.setImageResource(R.drawable.placeholder);
                }
            }
            // Priority 4: Resource IDs (numeric strings)
            else {
                try {
                    int resourceId = Integer.parseInt(p.imageUri);
                    Glide.with(h.imgCover.getContext())
                        .load(resourceId)
                        .placeholder(R.drawable.placeholder)
                        .into(h.imgCover);
                } catch (NumberFormatException e) {
                    // Fallback: try as URL one more time
                    Glide.with(h.imgCover.getContext())
                        .load(p.imageUri)
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.placeholder)
                        .into(h.imgCover);
                }
            }
        } else {
            h.imgCover.setImageResource(R.drawable.placeholder);
        }

        h.itemView.setOnClickListener(v -> listener.onOpen(p));
        
        // Set initial icon state
        h.btnLike.setImageResource(p.liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        h.btnBookmark.setImageResource(p.bookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);

        // Like button - update immediately on click
        h.btnLike.setOnClickListener(v -> {
            // Toggle state immediately for visual feedback
            p.liked = !p.liked;
            h.btnLike.setImageResource(p.liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            // Notify listener to update backend
            listener.onLike(p);
        });

        // Bookmark button - update immediately on click
        h.btnBookmark.setOnClickListener(v -> {
            // Toggle state immediately for visual feedback
            p.bookmarked = !p.bookmarked;
            h.btnBookmark.setImageResource(p.bookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);
            // Notify listener to update backend
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
