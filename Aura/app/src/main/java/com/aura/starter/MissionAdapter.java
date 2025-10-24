package com.aura.starter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.aura.starter.model.Mission;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class MissionAdapter extends RecyclerView.Adapter<MissionAdapter.VH> {

    public interface Listener { void onClick(Mission m); }
    private final Listener listener;
    private final List<Mission> data = new ArrayList<>();
    public MissionAdapter(Listener l){ this.listener = l; }

    public void submit(List<Mission> list){
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mission, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int i) {
        Mission m = data.get(i);
        h.title.setText(m.title);
        h.desc.setText(m.desc);
        h.points.setText("Points +" + m.points);
        h.progressText.setText(m.progress + "/" + m.target);
        h.progressBar.setMax(m.target);
        h.progressBar.setProgress(m.progress);
        h.action.setText(m.done? "Done" : "Do it");
        h.action.setTextColor(m.done? 0xFF999999 : 0xFF2962FF);
        if (m.photoUri != null && !m.photoUri.isEmpty()) {
            Glide.with(h.thumb.getContext()).load(Uri.parse(m.photoUri)).placeholder(R.drawable.placeholder).into(h.thumb);
        } else {
            h.thumb.setImageResource(R.drawable.placeholder);
        }
        h.itemView.setOnClickListener(v -> listener.onClick(m));
        h.action.setOnClickListener(v -> listener.onClick(m));
    }

    @Override public int getItemCount(){ return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, desc, points, action, progressText;
        ProgressBar progressBar;
        ImageView thumb;
        VH(View v){
            super(v);
            title = v.findViewById(R.id.tvTitle);
            desc = v.findViewById(R.id.tvDesc);
            points = v.findViewById(R.id.tvPoints);
            action = v.findViewById(R.id.tvAction);
            progressText = v.findViewById(R.id.tvProgress);
            progressBar = v.findViewById(R.id.progress);
            thumb = v.findViewById(R.id.imgThumb);
        }
    }
}
