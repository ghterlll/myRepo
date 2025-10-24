package com.aura.starter.leaderboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.aura.starter.R;

import java.util.List;

public class StepsLeaderboardAdapter extends RecyclerView.Adapter<StepsLeaderboardAdapter.VH> {

    private final List<StepsLeaderboardActivity.StepsRow> data;
    public StepsLeaderboardAdapter(List<StepsLeaderboardActivity.StepsRow> data) { this.data = data; }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard_row, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int i) {
        StepsLeaderboardActivity.StepsRow r = data.get(i);
        h.name.setText(r.name + (r.isYou ? " (you)" : ""));
        h.steps.setText(String.valueOf(r.steps));
        h.rank.setText(String.valueOf(i+1));
        h.subtitle.setText("today");
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView rank, name, subtitle, steps;
        VH(View v) { super(v);
            rank = v.findViewById(R.id.tvRank);
            name = v.findViewById(R.id.tvName);
            subtitle = v.findViewById(R.id.tvSubtitle);
            steps = v.findViewById(R.id.tvSteps);
        }
    }
}
