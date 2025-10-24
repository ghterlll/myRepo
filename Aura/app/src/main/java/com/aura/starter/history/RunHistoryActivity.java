package com.aura.starter.history;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aura.starter.R;
import com.aura.starter.data.AppDatabase;
import com.aura.starter.data.run.RunSession;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RunHistoryActivity extends AppCompatActivity implements RunHistoryAdapter.OnClick {
    private RecyclerView rv;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_history);
        setTitle("Run History");

        rv = findViewById(R.id.rvRuns);
        rv.setLayoutManager(new LinearLayoutManager(this));
        new Thread(() -> {
            List<RunSession> data = AppDatabase.get(this).runDao().getAllSessions();
            runOnUiThread(() -> rv.setAdapter(new RunHistoryAdapter(data, this)));
        }).start();
    }

    @Override public void onClick(RunSession s) {
        Intent i = new Intent(this, RunDetailActivity.class);
        i.putExtra("sid", s.id);
        startActivity(i);
    }

    public static String formatDuration(long ms) {
        long m = ms / 60000;
        long s = (ms / 1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }
    public static String dateStr(long ms) {
        return new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(ms);
    }
}
