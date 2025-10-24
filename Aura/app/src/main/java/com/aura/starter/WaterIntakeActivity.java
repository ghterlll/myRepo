package com.aura.starter;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ImageView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.appcompat.app.AppCompatActivity;
import com.aura.starter.network.AuraRepository;
import com.aura.starter.network.models.*;

public class WaterIntakeActivity extends AppCompatActivity {
    
    private static final String TAG = "WaterIntakeActivity";
    private TextView tvCurrentAmount;
    private TextView tvProgress;
    private TextView tvRecordCount;
    private LinearLayout emptyState;
    private LinearLayout recordsList;
    private ImageView ivWaterLevel;
    private String currentDate;
    private int dailyGoal = 2268; // ml
    
    private AuraRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water_intake);
        
        // Initialize repository
        repository = new AuraRepository(this);
        repository.getAuthManager().initTokenToApiClient();
        currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Initialize views and setup UI
        initializeViews();
        setupClickListeners();
        
        // Load current data
        loadWaterData();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadWaterData();
    }
    
    private void initializeViews() {
        try {
            // Initialize views
            tvCurrentAmount = findViewById(R.id.tvCurrentAmount);
            tvProgress = findViewById(R.id.tvProgress);
            tvRecordCount = findViewById(R.id.tvRecordCount);
            emptyState = findViewById(R.id.emptyState);
            recordsList = findViewById(R.id.recordsList);
            ivWaterLevel = findViewById(R.id.ivWaterLevel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupClickListeners() {
        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        // Settings button
        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            // TODO: Open settings
        });
        
        // Record water button - show bottom sheet
        findViewById(R.id.btnRecordWater).setOnClickListener(v -> {
            WaterRecordSheet sheet = new WaterRecordSheet();
            sheet.setOnSavedListener(() -> {
                // Refresh data when record is saved
                loadWaterData();
            });
            sheet.show(getSupportFragmentManager(), "WaterRecordSheet");
        });
        
        // Quick record buttons
        setupQuickRecordButtons();
    }
    
    private void setupQuickRecordButtons() {
        // Get all quick record button containers
        LinearLayout quickRecordContainer = findViewById(R.id.quickRecordContainer);
        if (quickRecordContainer != null) {
            for (int i = 0; i < quickRecordContainer.getChildCount(); i++) {
                View child = quickRecordContainer.getChildAt(i);
                if (child instanceof LinearLayout) {
                    LinearLayout row = (LinearLayout) child;
                    for (int j = 0; j < row.getChildCount(); j++) {
                        View button = row.getChildAt(j);
                        if (button instanceof LinearLayout) {
                            button.setOnClickListener(v -> {
                                // Quick record - show bottom sheet
                                WaterRecordSheet sheet = new WaterRecordSheet();
                                sheet.setOnSavedListener(() -> {
                                    loadWaterData();
                                });
                                sheet.show(getSupportFragmentManager(), "WaterRecordSheet");
                            });
                        }
                    }
                }
            }
        }
    }
    
    
    private void loadWaterData() {
        executor.execute(() -> {
            try {
                ApiResponse<WaterDailySummaryResponse> response = repository.getWaterDailySummary(currentDate);
                if (response != null && response.isSuccess() && response.getData() != null) {
                    int totalIntake = response.getData().getTotalMl();
                    
                    mainHandler.post(() -> {
                        updateWaterDisplay(totalIntake);
                        updateProgress(totalIntake);
                        // Note: API doesn't return individual records, only total
                        // You may need to add a separate API endpoint for detailed records
                        updateRecordsListSimplified(totalIntake);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to load water data", e);
            }
        });
    }
    
    private void updateRecordsListSimplified(int totalIntake) {
        if (totalIntake == 0) {
            emptyState.setVisibility(View.VISIBLE);
            recordsList.setVisibility(View.GONE);
            tvRecordCount.setText("0 times");
        } else {
            emptyState.setVisibility(View.GONE);
            recordsList.setVisibility(View.VISIBLE);
            tvRecordCount.setText("Total: " + totalIntake + "ml");
            recordsList.removeAllViews();
        }
    }
    
    private void updateWaterDisplay(int totalIntake) {
        tvCurrentAmount.setText(totalIntake + "ml");
        
        // Update water level in glass
        if (totalIntake > 0) {
            ivWaterLevel.setVisibility(View.VISIBLE);
            // Calculate water level percentage (max 80% of glass height)
            float percentage = Math.min((float) totalIntake / dailyGoal, 1.0f);
            int waterHeight = (int) (200 * 0.8f * percentage); // 200dp is glass height
            ivWaterLevel.getLayoutParams().height = dpToPx(waterHeight);
            ivWaterLevel.requestLayout();
        } else {
            ivWaterLevel.setVisibility(View.GONE);
        }
    }
    
    private void updateProgress(int totalIntake) {
        int percentage = (int) ((float) totalIntake / dailyGoal * 100);
        int remaining = Math.max(0, dailyGoal - totalIntake);
        
        String progressText = String.format("Completed %d%%, still need to drink %dml", 
                percentage, remaining);
        tvProgress.setText(progressText);
    }
    
    
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
