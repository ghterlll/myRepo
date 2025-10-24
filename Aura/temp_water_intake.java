package com.aura.starter;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ImageView;
import androidx.viewpager2.widget.ViewPager2;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import androidx.appcompat.app.AppCompatActivity;
import com.aura.starter.data.*;

public class WaterIntakeActivity extends AppCompatActivity {

    private TextView tvCurrentAmount;
    private TextView tvProgress;
    private TextView tvRecordCount;
    private LinearLayout emptyState;
    private LinearLayout recordsList;
    private ImageView ivWaterLevel;
    private WaterDao waterDao;
    private String currentDate;
    private int dailyGoal = 2268; // ml

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water_intake);
        
        // Initialize database
        WaterDb db = WaterDb.getDatabase(this);
        waterDao = db.waterDao();
        
        // Get current date
        currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        
        // Initialize views
        tvCurrentAmount = findViewById(R.id.tvCurrentAmount);
        tvProgress = findViewById(R.id.tvProgress);
        tvRecordCount = findViewById(R.id.tvRecordCount);
        emptyState = findViewById(R.id.emptyState);
        recordsList = findViewById(R.id.recordsList);
        ivWaterLevel = findViewById(R.id.ivWaterLevel);
        

        // Setup click listeners
        setupClickListeners();

        // Load current data
        loadWaterData();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadWaterData();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2001 && resultCode == RESULT_OK) {
            // Water record activity returned successfully, reload data
            loadWaterData();
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


