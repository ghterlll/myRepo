package com.aura.starter.run;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import com.aura.starter.MainActivity;
import com.aura.starter.R;

public class RunTrackingService extends Service implements LocationListener {

    public static final String ACTION_START = "com.aura.starter.run.START";
    public static final String ACTION_STOP = "com.aura.starter.run.STOP";
    public static final String ACTION_BROADCAST = "com.aura.starter.run.LOC_UPDATE";

    public static final String EXTRA_LAT = "lat";
    public static final String EXTRA_LON = "lon";
    public static final String EXTRA_DIST = "dist";
    public static final String EXTRA_ELAPSED = "elapsed";

    private static final int NOTIF_ID = 3331;
    private static final String CHANNEL_ID = "run_tracking";

    private LocationManager locationManager;
    private double totalMeters = 0.0;
    private Location lastLoc = null;
    private long startElapsed = 0L;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        startElapsed = SystemClock.elapsedRealtime();
        ensureChannel();
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE);
        Notification n;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            n = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Running…")
                    .setContentText("Tracking your route")
                    .setSmallIcon(R.drawable.ic_run_start_24)
                    .setContentIntent(pi)
                    .setOngoing(true)
                    .build();
        } else {
            n = new Notification.Builder(this)
                    .setContentTitle("Running…")
                    .setContentText("Tracking your route")
                    .setSmallIcon(R.drawable.ic_run_start_24)
                    .setContentIntent(pi)
                    .setOngoing(true)
                    .build();
        }
        startForeground(NOTIF_ID, n);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1f, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000, 5f, this);
        } catch (SecurityException e) {
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Run tracking", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(ch);
        }
    }

    @Nullable @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onLocationChanged(Location location) {
        if (lastLoc != null) {
            totalMeters += lastLoc.distanceTo(location);
        }
        lastLoc = location;
        long elapsed = SystemClock.elapsedRealtime() - startElapsed;

        Intent b = new Intent(ACTION_BROADCAST);
        b.setPackage(getPackageName()); // Make it explicit to avoid security issues
        b.putExtra(EXTRA_LAT, location.getLatitude());
        b.putExtra(EXTRA_LON, location.getLongitude());
        b.putExtra(EXTRA_DIST, totalMeters);
        b.putExtra(EXTRA_ELAPSED, elapsed);
        sendBroadcast(b);
    }
}
