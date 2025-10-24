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
        com.aura.starter.model.Post p = data.get(i);
        h.tvTitle.setText(p.title);
        h.tvAuthor.setText(p.author);

        // 处理图片显示
        if (p.imageUri != null && !p.imageUri.isEmpty()){
            // 如果是图片文件路径，加载图片
            if (p.imageUri.startsWith("/data/") || p.imageUri.contains("cache") || p.imageUri.contains("Pictures")) {
                // 文件路径
                Glide.with(h.imgCover.getContext()).load(new File(p.imageUri)).placeholder(R.drawable.placeholder).into(h.imgCover);
            } else if (p.imageUri.startsWith("img") && p.imageUri.length() <= 5) {
                // Assets图片
                try {
                    AssetManager assetManager = h.imgCover.getContext().getAssets();
                    InputStream inputStream = assetManager.open("images/" + p.imageUri + ".png");
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();

                    if (bitmap != null) {
                        Glide.with(h.imgCover.getContext()).load(bitmap).placeholder(R.drawable.placeholder).into(h.imgCover);
                    } else {
                        h.imgCover.setImageResource(R.drawable.placeholder);
                    }
                } catch (IOException e) {
                    // assets中找不到图片，使用占位符
                    h.imgCover.setImageResource(R.drawable.placeholder);
                }
            } else {
                // 其他类型的图片URI（drawable资源ID或网络图片）
                try {
                    // 尝试作为资源ID加载
                    int resourceId = Integer.parseInt(p.imageUri);
                    Glide.with(h.imgCover.getContext()).load(resourceId).placeholder(R.drawable.placeholder).into(h.imgCover);
                } catch (NumberFormatException e) {
                    // 不是资源ID，作为普通URI加载
                    Glide.with(h.imgCover.getContext()).load(p.imageUri).placeholder(R.drawable.placeholder).into(h.imgCover);
                }
            }
        } else {
            h.imgCover.setImageResource(R.drawable.placeholder);
        }

        h.itemView.setOnClickListener(v -> listener.onOpen(p));
        h.btnLike.setImageResource(p.liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        h.btnBookmark.setImageResource(p.bookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);

        h.btnLike.setOnClickListener(v -> listener.onLike(p));
        h.btnBookmark.setOnClickListener(v -> listener.onBookmark(p));
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
