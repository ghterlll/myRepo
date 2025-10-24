package com.aura.starter;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.*;

import com.aura.starter.network.AuraRepository;
import com.aura.starter.network.models.*;
import com.aura.starter.view.TrendChartView;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.content.SharedPreferences;

public class WeightTrendActivity extends Activity {

    private static final String TAG = "WeightTrendActivity";
    private static final String PREF_WEIGHT = "weight_prefs";
    private static final String KEY_FIRST_PROMPTED = "first_setup_prompted_v1";
    private static final String KEY_START_KG = "start_kg_v1";
    private static final String KEY_TARGET_KG = "target_kg_v1";
    private TextView tvLossTotal, tvStart, tvCurrent, tvTarget, tvUpdatedAt;
    private TrendChartView chartView;
    private Button btnAdd;
    private ImageButton btnInlineEditStart, btnInlineEditCurrent, btnInlineEditTarget;

    private final List<WeightEntry> entries = new ArrayList<>();
    private Float startKg = null;
    private float currentKg = 0.0f;
    private Float targetKg = null;

    private AuraRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean promptedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight_trend);

        repository = new AuraRepository(this);
        repository.getAuthManager().initTokenToApiClient();

        bindViews();
        loadWeightDataFromAPI();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnAdd.setOnClickListener(v -> showAddCurrentDialog());

        View.OnClickListener editStart = v -> showNumberEditDialog("Set Start Weight", startKg == null ? 0f : startKg, kg -> updateStartViaWeightInitial(kg));
        btnInlineEditStart.setOnClickListener(editStart);
        tvStart.setOnClickListener(editStart);

        btnInlineEditCurrent.setOnClickListener(v -> showEditDialog("Current", currentKg, kg -> {
            addWeightToAPI(kg);
        }));

        View.OnClickListener editTarget = v -> showNumberEditDialog("Set Target Weight", targetKg == null ? 0f : targetKg, kg -> updateProfileTarget(kg));
        btnInlineEditTarget.setOnClickListener(editTarget);
        tvTarget.setOnClickListener(editTarget);
    }

    private void bindViews() {
        tvLossTotal = findViewById(R.id.tvLossTotal);
        tvStart     = findViewById(R.id.tvStart);
        tvCurrent   = findViewById(R.id.tvCurrent);
        tvTarget    = findViewById(R.id.tvTarget);
        tvUpdatedAt = findViewById(R.id.tvUpdatedAt);
        chartView   = findViewById(R.id.trendChart);
        btnAdd      = findViewById(R.id.btnAddWeight);
        btnInlineEditStart   = findViewById(R.id.btnInlineEditStart);
        btnInlineEditCurrent = findViewById(R.id.btnInlineEditCurrent);
        btnInlineEditTarget  = findViewById(R.id.btnInlineEditTarget);
    }

    private void loadWeightDataFromAPI() {
        executor.execute(() -> {
            try {
                // Calculate date range (last 30 days)
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String endDate = sdf.format(new Date());
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_MONTH, -30);
                String startDate = sdf.format(cal.getTime());
                
                // Get weight history
                ApiResponse<WeightHistoryResponse> historyResponse = repository.getWeightHistory(startDate, endDate);
                
                // Get user profile for goals
                ApiResponse<UserProfileResponse> profileResponse = repository.getMyProfile();
                
                if (historyResponse != null && historyResponse.isSuccess() && historyResponse.getData() != null) {
                    List<WeightHistoryResponse.WeightDayItem> logs = historyResponse.getData().getItems();
                    entries.clear();
                    for (WeightHistoryResponse.WeightDayItem log : logs) {
                        try {
                            Date date = sdf.parse(log.getDate());
                            entries.add(new WeightEntry(date, log.getWeightKg().floatValue()));
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to parse date", e);
                        }
                    }
                    
                    // Keep last 7 entries for chart
                    if (entries.size() > 7) {
                        entries.subList(0, entries.size() - 7).clear();
                    }
                    
                    if (!entries.isEmpty()) {
                        currentKg = entries.get(entries.size() - 1).kg;
                    }
                }
                
                if (profileResponse != null && profileResponse.isSuccess() && profileResponse.getData() != null) {
                    UserProfileResponse profile = profileResponse.getData();
                    startKg = profile.getWeightKg() == null ? null : profile.getWeightKg().floatValue();
                    targetKg = profile.getTargetWeightKg() == null ? null : profile.getTargetWeightKg().floatValue();
                }

                // Override with local cached values if present (fallback when backend doesn't persist these fields)
                SharedPreferences sp = getSharedPreferences(PREF_WEIGHT, MODE_PRIVATE);
                if (sp.contains(KEY_START_KG)) {
                    try { startKg = Float.parseFloat(sp.getString(KEY_START_KG, "")); } catch (Exception ignore) {}
                }
                if (sp.contains(KEY_TARGET_KG)) {
                    try { targetKg = Float.parseFloat(sp.getString(KEY_TARGET_KG, "")); } catch (Exception ignore) {}
                }
                
                mainHandler.post(() -> {
                    renderHeader();
                    renderChart();
                    maybePromptFirstSetup();
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to load weight data", e);
            }
        });
    }

    private void maybePromptFirstSetup() {
        if (promptedOnce) return;
        SharedPreferences sp = getSharedPreferences(PREF_WEIGHT, MODE_PRIVATE);
        if (sp.getBoolean(KEY_FIRST_PROMPTED, false)) return; // only once per install
        boolean needStart = (startKg == null);
        boolean needTarget = (targetKg == null);
        if (!needStart && !needTarget) return;
        promptedOnce = true;

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(20), dp(12), dp(20), dp(8));

        final EditText etStart = new EditText(this);
        etStart.setHint("Start weight (kg)");
        etStart.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (startKg != null) etStart.setText(String.format(Locale.getDefault(), "%.1f", startKg));
        wrap.addView(etStart);

        final EditText etTarget = new EditText(this);
        etTarget.setHint("Target weight (kg)");
        etTarget.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (targetKg != null) etTarget.setText(String.format(Locale.getDefault(), "%.1f", targetKg));
        wrap.addView(etTarget);

        // mark as prompted to avoid future popups even if user skips
        sp.edit().putBoolean(KEY_FIRST_PROMPTED, true).apply();

        new AlertDialog.Builder(this)
                .setTitle("Set Start & Target")
                .setView(wrap)
                .setNegativeButton("Skip", (d, w) -> { renderHeader(); })
                .setPositiveButton("Save", (d, w) -> {
                    try {
                        UserProfileUpdateRequest req = new UserProfileUpdateRequest();
                        boolean doUpdate = false;
                        if (etStart.getText()!=null && etStart.getText().length()>0) {
                            float v = Float.parseFloat(etStart.getText().toString());
                            startKg = v;
                            // call weight initial API asynchronously
                            updateStartViaWeightInitial(v);
                        }
                        if (etTarget.getText()!=null && etTarget.getText().length()>0) {
                            float v2 = Float.parseFloat(etTarget.getText().toString());
                            targetKg = v2;
                            // cache then update profile
                            getSharedPreferences(PREF_WEIGHT, MODE_PRIVATE).edit().putString(KEY_TARGET_KG, String.valueOf(v2)).apply();
                            updateProfileTarget(v2);
                        }
                        renderHeader();
                        // ensure data reflects server after saves complete
                        loadWeightDataFromAPI();
                    } catch (Exception ex) {
                        Toast.makeText(this, "Invalid numbers", Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    private void updateStartViaWeightInitial(float kg) {
        startKg = kg;
        getSharedPreferences(PREF_WEIGHT, MODE_PRIVATE).edit().putString(KEY_START_KG, String.valueOf(kg)).apply();
        executor.execute(() -> {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                String today = sdf.format(new java.util.Date());
                ApiResponse<Void> resp = repository.updateInitialWeight(today, new java.math.BigDecimal(kg));
                mainHandler.post(() -> {
                    // Some backends return 200 with null body; treat as success if no exception
                    Toast.makeText(this, (resp == null || resp.isSuccess()) ? "Start saved" : "Start save failed", Toast.LENGTH_SHORT).show();
                    loadWeightDataFromAPI();
                });
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(this, "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateProfileTarget(float kg) {
        UserProfileUpdateRequest req = new UserProfileUpdateRequest();
        req.setTargetWeightKg(new java.math.BigDecimal(kg));
        targetKg = kg;
        getSharedPreferences(PREF_WEIGHT, MODE_PRIVATE).edit().putString(KEY_TARGET_KG, String.valueOf(kg)).apply();
        updateProfile(req);
        // also refresh to reflect latest from /weight/latest
        loadWeightDataFromAPI();
    }

    private void updateProfile(UserProfileUpdateRequest req) {
        executor.execute(() -> {
            try {
                ApiResponse<Void> resp = repository.updateMyProfile(req);
                mainHandler.post(() -> {
                    if (resp != null && resp.isSuccess()) {
                        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
                        renderHeader();
                    } else {
                        Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "updateProfile", e);
                mainHandler.post(() -> Toast.makeText(this, "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void addWeightToAPI(float kg) {
        executor.execute(() -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String today = sdf.format(new Date());
                
                ApiResponse<Void> response = repository.logWeight(today, new BigDecimal(kg));
                Log.d(TAG, "Weight log response: " + (response != null ? response.toString() : "null"));
                
                if (response != null && response.isSuccess()) {
                    currentKg = kg;
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Weight logged successfully", Toast.LENGTH_SHORT).show();
                        loadWeightDataFromAPI();
                    });
                } else {
                    String errorMsg = response != null ? 
                            "Code: " + response.getCode() + ", Msg: " + response.getMessage() : 
                            "Response is null";
                    Log.e(TAG, "Failed to log weight: " + errorMsg);
                    
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to add weight", e);
                mainHandler.post(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void renderHeader() {
        Float s = startKg;
        Float t = targetKg;
        if (s == null) {
            tvLossTotal.setText("--");
        } else {
            float loss = s - currentKg; // negative means weight gain
        tvLossTotal.setText(String.format(Locale.getDefault(), "%+.1f kg", loss));
        }
        tvStart.setText(s == null ? "--" : String.format(Locale.getDefault(), "%.1f kg", s));
        tvCurrent.setText(String.format(Locale.getDefault(), "%.1f kg", currentKg));
        tvTarget.setText(t == null ? "--" : String.format(Locale.getDefault(), "%.1f kg", t));
        tvUpdatedAt.setText("(updated " + new SimpleDateFormat("MMM d", Locale.getDefault()).format(new Date()) + ")");
    }

    private void renderChart() {
        chartView.setData(toEntryList(), targetKg == null ? 0f : targetKg);
    }

    private List<WeightEntry> toEntryList() { return new ArrayList<>(entries); }

    private void showAddCurrentDialog() {
        showEditDialog("+ Record Weight", currentKg, this::addWeightToAPI);
    }

    private interface OnNumberPicked { void onPicked(float v); }

    private void showNumberEditDialog(String title, Float defaultValue, OnNumberPicked onOk) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(20), dp(12), dp(20), dp(4));

        final EditText et = new EditText(this);
        et.setHint("Weight (kg)");
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (defaultValue != null) et.setText(String.format(Locale.getDefault(), "%.1f", defaultValue));
        wrap.addView(et);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(wrap)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    try {
                        float kg = Float.parseFloat(et.getText().toString());
                        onOk.onPicked(kg);
                    } catch (Exception ex) {
                        Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    private void showEditDialog(String title, float defaultValue, OnNumberPicked onOk) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(20), dp(12), dp(20), dp(4));

        final EditText et = new EditText(this);
        et.setHint("Weight (kg)");
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        et.setText(String.format(Locale.getDefault(), "%.1f", defaultValue));
        wrap.addView(et);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(wrap)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    try {
                        float kg = Float.parseFloat(et.getText().toString());
                        onOk.onPicked(kg);
                    } catch (Exception ex) {
                        Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    private int dp(int v) { return Math.round(getResources().getDisplayMetrics().density * v); }

    // ------- inner model for chart -------
    public static class WeightEntry {
        public final Date date;
        public final float kg;
        public WeightEntry(Date d, float k) { date = d; kg = k; }
    }
}
