package com.aura.starter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.VH> {
    private final List<String> data = new ArrayList<>();
    public void submit(List<String> list){
        data.clear();
        if (list!=null) data.addAll(list);
        notifyDataSetChanged();
    }
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView t = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new VH(t);
    }
    @Override public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.t.setText(data.get(position));
    }
    @Override public int getItemCount(){ return data.size(); }
    static class VH extends RecyclerView.ViewHolder {
        TextView t;
        VH(View v){ super(v); t = (TextView) v; }
    }
}
