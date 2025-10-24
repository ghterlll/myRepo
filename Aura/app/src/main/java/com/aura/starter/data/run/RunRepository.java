package com.aura.starter.data.run;

import android.content.Context;

import com.aura.starter.data.AppDatabase;
import org.osmdroid.util.GeoPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RunRepository {
    private final RunDao dao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public RunRepository(Context ctx) {
        dao = AppDatabase.get(ctx).runDao();
    }

    public void saveSessionAsync(long start, long end, double meters, long durationMs, double paceMinPerKm, double kcal, List<GeoPoint> poly) {
        io.execute(() -> {
            RunSession s = new RunSession();
            s.startTimeMillis = start;
            s.endTimeMillis = end;
            s.distanceMeters = meters;
            s.durationMillis = durationMs;
            s.avgPaceMinPerKm = paceMinPerKm;
            s.kcal = kcal;
            long sid = dao.insertSession(s);

            List<RunPoint> pts = new ArrayList<>();
            int i = 0;
            for (GeoPoint gp : poly) {
                RunPoint p = new RunPoint();
                p.sessionId = sid;
                p.lat = gp.getLatitude();
                p.lon = gp.getLongitude();
                p.seq = i++;
                pts.add(p);
            }
            if (!pts.isEmpty()) dao.insertPoints(pts);
        });
    }
}
