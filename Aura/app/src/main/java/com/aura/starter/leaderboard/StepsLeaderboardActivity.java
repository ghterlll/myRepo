package com.aura.starter.leaderboard;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aura.starter.R;
import com.aura.starter.steps.StepTracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StepsLeaderboardActivity extends AppCompatActivity {

    private boolean optedIn = false;
    private final List<StepsRow> rows = new ArrayList<>();
    private StepsLeaderboardAdapter adapter;
    private StepTracker stepTracker;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_steps_leaderboard);
        setTitle("Steps Leaderboard");

        RecyclerView rv = findViewById(R.id.rvLeaderboard);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StepsLeaderboardAdapter(rows);
        rv.setAdapter(adapter);

        // simple opt-in
        getSharedPreferences("leaderboard", MODE_PRIVATE).edit().putBoolean("asked", true).apply();
        optedIn = getSharedPreferences("leaderboard", MODE_PRIVATE).getBoolean("opt_in", false);
        if (!optedIn) {
            new AlertDialog.Builder(this)
                    .setTitle("Join leaderboard?")
                    .setMessage("Share your today's steps with friends for motivation. You can opt-out anytime.")
                    .setPositiveButton("Join", (d, w) -> {
                        optedIn = true;
                        getSharedPreferences("leaderboard", MODE_PRIVATE).edit().putBoolean("opt_in", true).apply();
                        refreshData();
                    })
                    .setNegativeButton("Not now", (d,w) -> refreshData())
                    .show();
        } else {
            refreshData();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (stepTracker == null) stepTracker = new StepTracker(this);
        stepTracker.start();
        stepTracker.stepsLive().observe(this, steps -> {
            // index 0 reserved for "You" row
            if (steps == null) return;
            for (StepsRow r : rows) if (r.isYou) { r.steps = steps; break; }
            sortAndNotify();
        });
    }

    @Override protected void onPause() {
        super.onPause();
        if (stepTracker != null) stepTracker.stop();
    }

    private void refreshData() {
        rows.clear();
        if (optedIn) {
            rows.add(new StepsRow("You", 0, true));
        }
        // mock friends - replace with server API when ready
        rows.add(new StepsRow("Alex", 8200, false));
        rows.add(new StepsRow("Maya", 10650, false));
        rows.add(new StepsRow("Leo", 5400, false));
        sortAndNotify();
    }

    private void sortAndNotify() {
        Collections.sort(rows, Comparator.comparingInt((StepsRow r) -> r.steps).reversed());
        // write rank inside adapter bind
        adapter.notifyDataSetChanged();
    }

    static class StepsRow {
        final String name;
        int steps;
        final boolean isYou;
        StepsRow(String n, int s, boolean y) { name = n; steps = s; isYou = y; }
    }
}
