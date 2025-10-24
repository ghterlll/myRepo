package com.aura.starter.hub;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;

import com.aura.starter.R;
import com.aura.starter.MapFragment;
import com.aura.starter.history.RunHistoryActivity;
import com.aura.starter.leaderboard.StepsLeaderboardActivity;
import com.aura.starter.run.RunTrackingService;

import java.util.Locale;

public class RunHubActivity extends AppCompatActivity {

    private com.google.android.material.button.MaterialButton btnRunToggle;

    private final android.content.BroadcastReceiver runReceiver = new android.content.BroadcastReceiver() {
        @Override public void onReceive(android.content.Context context, Intent intent) {
            if (!RunTrackingService.ACTION_BROADCAST.equals(intent.getAction())) return;

            double meters  = intent.getDoubleExtra(RunTrackingService.EXTRA_DIST, 0);
            long elapsed   = intent.getLongExtra(RunTrackingService.EXTRA_ELAPSED, 0);

            double km = meters / 1000.0;
            String pace = "0′00″/km";
            if (km > 0.01) {
                double paceMin = (elapsed / 60000.0) / km;
                int mm = (int) paceMin;
                int ss = (int) Math.round((paceMin - mm) * 60);
                pace = String.format(java.util.Locale.getDefault(), "%d′%02d″/km", mm, ss);
            }
            long mm = elapsed / 60000;
            long ss = (elapsed / 1000) % 60;

            TextView d = findViewById(R.id.tvNowDist);
            TextView p = findViewById(R.id.tvNowPace);
            TextView t = findViewById(R.id.tvNowTime);
            if (d != null) d.setText(String.format(java.util.Locale.getDefault(), "%.2f km", km));
            if (p != null) p.setText(pace);
            if (t != null) t.setText(String.format(java.util.Locale.getDefault(), "%02d:%02d", mm, ss));

            // 广播频繁，但这里查询一次 Fragment 状态是轻量的
            updateRunButton();
        }
    };

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_hub);
        setTitle("Run");

        boolean autoStart = getIntent().getBooleanExtra("autoStart", false);
        FragmentContainerView host = findViewById(R.id.mapHost);
        if (host != null) {
            MapFragment f = new MapFragment();
            Bundle args = new Bundle();
            args.putBoolean("autoStart", autoStart);
            f.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.mapHost, f)
                    .commitNow();
        }

        // 注意：布局里按钮 id 还是 btnStopRun，不改xml也能用
        btnRunToggle = findViewById(R.id.btnStopRun);

        // 切换：正在跑步 -> 停止；未跑步 -> 开始
        btnRunToggle.setOnClickListener(v -> {
            androidx.fragment.app.Fragment frag = getSupportFragmentManager().findFragmentById(R.id.mapHost);
            if (frag instanceof MapFragment) {
                MapFragment map = (MapFragment) frag;
                if (map.isTracking()) {
                    map.stopFromHost();
                } else {
                    map.startFromHost();
                }
                // 点击后立刻刷新文案
                updateRunButton();
            } else {
                // 不在的话做一个兜底：只发停止
                startService(new Intent(this, RunTrackingService.class)
                        .setAction(RunTrackingService.ACTION_STOP));
            }
        });

        findViewById(R.id.btnSeeAllHistory)
                .setOnClickListener(v -> startActivity(new Intent(this, RunHistoryActivity.class)));
        findViewById(R.id.btnSeeAllLeaderboard)
                .setOnClickListener(v -> startActivity(new Intent(this, StepsLeaderboardActivity.class)));

        TextView hist = findViewById(R.id.tvHistoryPreview);
        TextView lb   = findViewById(R.id.tvLeaderboardPreview);
        if (hist != null) hist.setText("Last: 3.45 km • 21:12 • Oct 7, 17:05");
        if (lb   != null) lb.setText("1. Maya 10,650  •  2. You 9,820  •  3. Alex 8,200");

        updateRunButton();
    }

    @Override protected void onResume() {
        super.onResume();
        android.content.IntentFilter filter = new android.content.IntentFilter(RunTrackingService.ACTION_BROADCAST);
        androidx.core.content.ContextCompat.registerReceiver(this, runReceiver, filter,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);
        updateRunButton();
    }

    @Override protected void onPause() {
        super.onPause();
        try { unregisterReceiver(runReceiver); } catch (Exception ignored) {}
    }

    /** 根据 MapFragment 的状态，切换按钮文案/样式 */
    private void updateRunButton() {
        if (btnRunToggle == null) return;
        androidx.fragment.app.Fragment frag = getSupportFragmentManager().findFragmentById(R.id.mapHost);
        boolean running = (frag instanceof MapFragment) && ((MapFragment) frag).isTracking();
        btnRunToggle.setText(running ? R.string.stop_run : R.string.start_run);

        // 想更明显也可以换底色/图标（可选）
        // int color = androidx.core.content.ContextCompat.getColor(this, running ? R.color.red_600 : R.color.teal_600);
        // btnRunToggle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        // btnRunToggle.setIconResource(running ? R.drawable.ic_stop_24 : R.drawable.ic_play_24);
    }
}