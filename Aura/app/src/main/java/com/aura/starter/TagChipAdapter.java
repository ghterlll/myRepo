package com.aura.starter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aura.starter.network.models.TagResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying tag chips in horizontal list
 */
public class TagChipAdapter extends RecyclerView.Adapter<TagChipAdapter.ViewHolder> {

    public interface OnTagClickListener {
        void onTagClick(TagResponse tag);
    }

    private final List<TagResponse> tags = new ArrayList<>();
    private final OnTagClickListener listener;

    public TagChipAdapter(OnTagClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<TagResponse> newTags) {
        tags.clear();
        if (newTags != null) {
            tags.addAll(newTags);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tag_chip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TagResponse tag = tags.get(position);
        holder.bind(tag, listener);
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTagName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTagName = itemView.findViewById(R.id.tvTagName);
        }

        public void bind(TagResponse tag, OnTagClickListener listener) {
            // Display tag with # prefix
            tvTagName.setText("#" + tag.getName());

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTagClick(tag);
                }
            });
        }
    }
}
