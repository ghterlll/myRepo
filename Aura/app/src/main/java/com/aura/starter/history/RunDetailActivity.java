package com.aura.starter.history;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aura.starter.R;
import com.aura.starter.data.AppDatabase;
import com.aura.starter.data.run.RunPoint;
import com.aura.starter.data.run.RunSession;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;

import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RunDetailActivity extends AppCompatActivity {

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_detail);
        setTitle("Run Detail");

        long sid = getIntent().getLongExtra("sid", -1);
        MapView map = findViewById(R.id.detailMap);
        TextView tv = findViewById(R.id.tvSummary);

        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("osmdroid", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        new Thread(() -> {
            RunSession s = AppDatabase.get(this).runDao().getAllSessions()
                    .stream().filter(it -> it.id == sid).findFirst().orElse(null);
            List<RunPoint> pts = AppDatabase.get(this).runDao().getPointsForSession(sid);
            List<GeoPoint> line = new ArrayList<>();
            for (RunPoint p : pts) line.add(new GeoPoint(p.lat, p.lon));

            runOnUiThread(() -> {
                tv.setText(String.format(Locale.getDefault(), "%.2f km • %02d:%02d • %.0f kcal",
                        s.distanceMeters/1000.0, (int)(s.durationMillis/60000), (int)((s.durationMillis/1000)%60), s.kcal));
                if (!line.isEmpty()) {
                    Polyline poly = new Polyline();
                    poly.setPoints(line);
                    map.getOverlays().add(poly);
                    map.getController().setZoom(16.0);
                    map.getController().setCenter(line.get(line.size()-1));
                    map.invalidate();
                }
            });
        }).start();
    }
}
