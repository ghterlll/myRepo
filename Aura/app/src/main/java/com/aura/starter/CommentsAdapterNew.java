package com.aura.starter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aura.starter.model.Comment;

import java.util.ArrayList;
import java.util.List;

public class CommentsAdapterNew extends RecyclerView.Adapter<CommentsAdapterNew.VH> {
    public interface Listener {
        void onLikeComment(Comment comment);
    }

    private final Listener listener;
    private final List<Comment> data = new ArrayList<>();

    public CommentsAdapterNew(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Comment> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Comment comment = data.get(position);
        h.tvAuthor.setText(comment.author);
        h.tvContent.setText(comment.content);
        h.tvTimestamp.setText(comment.timestamp);
        h.tvLikeCount.setText(String.valueOf(comment.likeCount));
        h.btnLike.setImageResource(comment.liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

        h.layoutLike.setOnClickListener(v -> listener.onLikeComment(comment));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAuthor, tvContent, tvTimestamp, tvLikeCount;
        ImageView btnLike;
        LinearLayout layoutLike;

        VH(View v) {
            super(v);
            tvAuthor = v.findViewById(R.id.tvAuthor);
            tvContent = v.findViewById(R.id.tvContent);
            tvTimestamp = v.findViewById(R.id.tvTimestamp);
            tvLikeCount = v.findViewById(R.id.tvLikeCount);
            btnLike = v.findViewById(R.id.btnLike);
            layoutLike = v.findViewById(R.id.layoutLike);
        }
    }
}
