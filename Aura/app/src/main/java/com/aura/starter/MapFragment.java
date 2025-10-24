package com.aura.starter;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import com.aura.starter.run.RunTrackingService;
import com.aura.starter.util.EnergyCalc;
import com.aura.starter.data.run.RunRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapFragment extends Fragment {

    private static final String SP = "map_prefs";
    private static final String KEY_LAT = "last_lat";
    private static final String KEY_LON = "last_lon";
    private static final String KEY_ZOOM = "last_zoom";

    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;
    private CompassOverlay compassOverlay;
    private ScaleBarOverlay scaleBarOverlay;
    private Polyline runLine;
    private final List<GeoPoint> runPoints = new ArrayList<>();
    private TextView tvStats;
    private ImageButton btnStartRun, btnStopRun;

    private long startWall = 0L;
    private double latestMeters = 0.0;
    private long latestElapsed = 0L;
    private RunRepository repo;

    /** 新增：是否正在记录跑步 */
    private boolean tracking = false;
    public boolean isTracking() { return tracking; }
    
    /** 新增：是否等待权限授予后开始跑步 */
    private boolean pendingRunStart = false;

    private final ActivityResultLauncher<String> fineLocPerm =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    enableMyLocation();
                    // If we were trying to start run tracking, do it now
                    if (pendingRunStart) {
                        startRunTracking();
                        pendingRunStart = false;
                    }
                } else {
                    Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    private final BroadcastReceiver runReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!RunTrackingService.ACTION_BROADCAST.equals(intent.getAction())) return;
            double lat = intent.getDoubleExtra(RunTrackingService.EXTRA_LAT, 0);
            double lon = intent.getDoubleExtra(RunTrackingService.EXTRA_LON, 0);
            latestMeters = intent.getDoubleExtra(RunTrackingService.EXTRA_DIST, 0);
            latestElapsed = intent.getLongExtra(RunTrackingService.EXTRA_ELAPSED, 0);

            GeoPoint p = new GeoPoint(lat, lon);
            runPoints.add(p);
            runLine.setPoints(runPoints);
            mapView.getController().animateTo(p);

            double km = latestMeters / 1000.0;
            double paceMin = (km > 0.01) ? (latestElapsed / 60000.0) / km : 0;
            String paceStr = (km > 0.01)
                    ? String.format(Locale.getDefault(), "%d′%02d″/km",
                    (int) paceMin, (int) Math.round((paceMin - (int) paceMin) * 60))
                    : "0′00″/km";
            double weightKg = EnergyCalc.readUserWeight(requireContext());
            double kcal = EnergyCalc.kcalFromRunKm(weightKg, km);
            tvStats.setText(String.format(Locale.getDefault(),
                    "%.2f km | %s | %02d:%02d | %.0f kcal",
                    km, paceStr, (int) (latestElapsed / 60000), (int) ((latestElapsed / 1000) % 60), kcal));
        }
    };

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        repo = new RunRepository(requireContext());

        Context ctx = requireContext().getApplicationContext();
        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(ctx.getPackageName());

        View root = inflater.inflate(R.layout.fragment_map, container, false);
        mapView = root.findViewById(R.id.mapView);
        tvStats = root.findViewById(R.id.tvRunStats);
        btnStartRun = root.findViewById(R.id.btnStartRun);
        btnStopRun = root.findViewById(R.id.btnStopRun);

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN || e.getAction() == MotionEvent.ACTION_MOVE) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
            }
            return false;
        });

        SharedPreferences sp = requireContext().getSharedPreferences(SP, Context.MODE_PRIVATE);
        double lat = Double.longBitsToDouble(sp.getLong(KEY_LAT, Double.doubleToLongBits(-37.8136)));
        double lon = Double.longBitsToDouble(sp.getLong(KEY_LON, Double.doubleToLongBits(144.9631)));
        double zoom = Double.longBitsToDouble(sp.getLong(KEY_ZOOM, Double.doubleToLongBits(14.0)));
        mapView.getController().setCenter(new GeoPoint(lat, lon));
        mapView.getController().setZoom(zoom);

        compassOverlay = new CompassOverlay(requireContext(),
                new InternalCompassOrientationProvider(requireContext()), mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);

        scaleBarOverlay = new ScaleBarOverlay(mapView);
        scaleBarOverlay.setAlignRight(true);
        scaleBarOverlay.setScaleBarOffset(20, 30);
        mapView.getOverlays().add(scaleBarOverlay);

        runLine = new Polyline();
        runLine.setGeodesic(true);
        mapView.getOverlays().add(runLine);

        root.findViewById(R.id.btnRecenter).setOnClickListener(v -> {
            GeoPoint center = (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null)
                    ? myLocationOverlay.getMyLocation()
                    : (GeoPoint) mapView.getMapCenter();
            mapView.getController().animateTo(center);
        });

        root.findViewById(R.id.btnMyLocation).setOnClickListener(v -> ensureFineLocation());

        root.findViewById(R.id.btnHelp).setOnClickListener(v -> new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Tips")
                .setMessage("Tap ▶ to start tracking, ⏹ to stop and save. Long-press any map point to navigate in external map apps.")
                .setPositiveButton("OK", null)
                .show());

        // —— 开始 —— //
        btnStartRun.setOnClickListener(v -> {
            // Check location permissions before starting run tracking
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                startRunTracking();
            } else {
                // Set flag to start tracking after permission is granted
                pendingRunStart = true;
                // Request location permission
                fineLocPerm.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        });

        // —— 停止并保存 —— //
        btnStopRun.setOnClickListener(v -> {
            tracking = false;                // 维护状态
            refreshToggleUi();               // 同步按钮显示

            long endWall = System.currentTimeMillis();
            requireContext().startService(
                    new Intent(requireContext(), RunTrackingService.class)
                            .setAction(RunTrackingService.ACTION_STOP)
            );

            double km = latestMeters / 1000.0;
            double paceMin = (km > 0.01) ? (latestElapsed / 60000.0) / km : 0;
            double weightKg = EnergyCalc.readUserWeight(requireContext());
            double kcal = EnergyCalc.kcalFromRunKm(weightKg, km);

            repo.saveSessionAsync(startWall, endWall, latestMeters, latestElapsed, paceMin, kcal, new ArrayList<>(runPoints));
            Toast.makeText(requireContext(), "Run saved", Toast.LENGTH_SHORT).show();
        });

        // 初始 UI
        refreshToggleUi();

        // 自动开始（来自 RunHubActivity 的 autoStart 参数）
        Bundle args = getArguments();
        if (args != null && args.getBoolean("autoStart", false)) {
            startFromHost();
        }

        return root;
    }

    /** 供宿主 Activity 复用 Fragment 的“开始/停止”按钮逻辑 */
    public void startFromHost() {
        if (btnStartRun != null && btnStartRun.getVisibility() == View.VISIBLE) {
            btnStartRun.performClick();
        }
    }
    public void stopFromHost() {
        if (btnStopRun != null && btnStopRun.getVisibility() == View.VISIBLE) {
            btnStopRun.performClick();
        } else {
            // 兜底：至少确保服务停止
            requireContext().startService(
                    new Intent(requireContext(), RunTrackingService.class)
                            .setAction(RunTrackingService.ACTION_STOP)
            );
            tracking = false;
            refreshToggleUi();
        }
    }

    /** 统一根据 tracking 状态切换两个按钮的可见性 */
    private void refreshToggleUi() {
        if (btnStartRun != null) btnStartRun.setVisibility(tracking ? View.GONE : View.VISIBLE);
        if (btnStopRun  != null) btnStopRun.setVisibility(tracking ? View.VISIBLE : View.GONE);
    }
    
    /** 开始跑步跟踪 */
    private void startRunTracking() {
        tracking = true;                 // 维护状态
        refreshToggleUi();               // 同步按钮显示

        runPoints.clear();
        runLine.setPoints(runPoints);
        tvStats.setText(getString(R.string.run_stats_placeholder));
        startWall = System.currentTimeMillis();
        latestMeters = 0;
        latestElapsed = 0;

        Intent serviceIntent = new Intent(requireContext(), RunTrackingService.class)
                .setAction(RunTrackingService.ACTION_START);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(serviceIntent);
        } else {
            requireContext().startService(serviceIntent);
        }
    }

    private void ensureFineLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            fineLocPerm.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void enableMyLocation() {
        if (myLocationOverlay != null) return;
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        mapView.getOverlays().add(myLocationOverlay);
        mapView.invalidate();
        Toast.makeText(requireContext(), "My location enabled", Toast.LENGTH_SHORT).show();
    }

    @Override public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(RunTrackingService.ACTION_BROADCAST);
        ContextCompat.registerReceiver(requireContext(), runReceiver, filter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
        mapView.onResume();
    }

    @Override public void onPause() {
        super.onPause();
        try { requireContext().unregisterReceiver(runReceiver); } catch (Exception ignored) {}
        mapView.onPause();

        GeoPoint c = (GeoPoint) mapView.getMapCenter();
        double zoom = mapView.getZoomLevelDouble();
        SharedPreferences sp = requireContext().getSharedPreferences(SP, Context.MODE_PRIVATE);
        sp.edit()
                .putLong(KEY_LAT, Double.doubleToLongBits(c.getLatitude()))
                .putLong(KEY_LON, Double.doubleToLongBits(c.getLongitude()))
                .putLong(KEY_ZOOM, Double.doubleToLongBits(zoom))
                .apply();
    }
}