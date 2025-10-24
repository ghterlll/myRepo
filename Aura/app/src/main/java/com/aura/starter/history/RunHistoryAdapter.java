package com.aura.starter.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aura.starter.R;
import com.aura.starter.data.run.RunSession;

import java.util.List;
import java.util.Locale;

public class RunHistoryAdapter extends RecyclerView.Adapter<RunHistoryAdapter.VH> {

    public interface OnClick { void onClick(RunSession s); }

    private final List<RunSession> data;
    private final OnClick cb;

    public RunHistoryAdapter(List<RunSession> data, OnClick cb) {
        this.data = data; this.cb = cb;
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_run_session, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int i) {
        RunSession s = data.get(i);
        String title = RunHistoryActivity.formatDuration(s.durationMillis) + " • " + String.format(Locale.getDefault(), "%.2f km", s.distanceMeters/1000.0);
        String sub = String.format(Locale.getDefault(), "Pace %.0f′%02.0f″/km • %.0f kcal • %s",
                Math.floor(s.avgPaceMinPerKm), (s.avgPaceMinPerKm - Math.floor(s.avgPaceMinPerKm)) * 60, s.kcal, RunHistoryActivity.dateStr(s.startTimeMillis));
        h.title.setText(title);
        h.sub.setText(sub);
        h.itemView.setOnClickListener(v -> cb.onClick(s));
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, sub;
        VH(View v) { super(v);
            title = v.findViewById(R.id.tvTitle);
            sub = v.findViewById(R.id.tvSub);
        }
    }
}
