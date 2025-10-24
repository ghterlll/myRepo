package com.aura.starter;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.view.Gravity;
import android.app.DatePickerDialog;
import android.widget.Toast;
import android.util.Log;
import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.room.Room;

// keep your existing data imports
import com.aura.starter.data.*;

// NEW: only need RunHubActivity for the single CTA
import com.aura.starter.hub.RunHubActivity;
import com.aura.starter.network.AuraRepository;
import com.aura.starter.network.models.*;
import com.aura.starter.data.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.math.BigDecimal;

public class RecordFragment extends Fragment {

    private static final String TAG = "RecordFragment";
    private TextView tvCurrentWeight;
    private TextView tvIntake;
    private TextView tvCaloriesLeft;
    private TextView tvBurn;
    
    // Repository for API calls
    private AuraRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_record, container, false);

        
        // Initialize repository
        repository = new AuraRepository(requireContext());
        repository.getAuthManager().initTokenToApiClient();
        
        View dateStrip = v.findViewById(R.id.dateStrip);
        View chevron = v.findViewById(R.id.btnDateChevron);
        LinearLayout daysContainer = v.findViewById(R.id.daysContainer);
        tvCurrentWeight = v.findViewById(R.id.tvCurrentWeight);

        // init 7-day strip centered on today
        Calendar c = Calendar.getInstance();
        populateSevenDays(daysContainer, c);

        tvIntake = v.findViewById(R.id.tvIntake);
        tvCaloriesLeft = v.findViewById(R.id.tvCaloriesLeft);
        tvBurn = v.findViewById(R.id.tvBurn);
        // remove duplicate initialization of the 7-day strip
        
        // Initialize data from API
        updateCalorieData();
        View.OnClickListener openPicker = view -> {
            Calendar now = Calendar.getInstance();
            DatePickerDialog dlg = new DatePickerDialog(requireContext(), (dp, y, m, d) -> {
                Calendar picked = Calendar.getInstance();
                picked.set(y, m, d);
                populateSevenDays(daysContainer, picked);
                // TODO: load records for selected date
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
            dlg.show();
        };
        dateStrip.setOnClickListener(openPicker);
        chevron.setOnClickListener(openPicker);

        // Setup meal chip click listeners
        setupMealChipListeners(v);

        // Navigate to weight trend from Current Weight card
        View currentWeight = v.findViewById(R.id.currentWeightCard);
        if (currentWeight != null) {
            currentWeight.setOnClickListener(view -> {
                Intent i = new Intent(requireContext(), WeightTrendActivity.class);
                startActivity(i);
            });
        }

        // Camera quick action: open FoodCameraActivity when tapping the big camera icon
        View cameraWrap = v.findViewById(R.id.homeCameraWrap);
        if (cameraWrap != null) {
            cameraWrap.setOnClickListener(view -> {
                Intent i = new Intent(requireContext(), FoodCameraActivity.class);
                startActivity(i);
            });
        }

        // Water card → open WaterIntakeActivity / bottom sheet
        View waterCard = v.findViewById(R.id.waterCard);
        if (waterCard != null) {
            waterCard.setOnClickListener(view -> {
                Intent i = new Intent(requireContext(), WaterIntakeActivity.class);
                startActivity(i);
            });
        }
        
        // Setup meal chip click listeners for food adding
        // already set above
        
        // initial sync with weight DB
        updateCurrentWeightFromDb();

        // NEW: single big CTA "Start Run" → open RunHubActivity (auto start)
        setupStartRunButton(v);

        return v;
    }

    @Override public void onResume() {
        super.onResume();
        updateCurrentWeightFromDb();
        updateWaterData();
        updateFoodRecordsDisplay();
        updateCalorieData();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == getActivity().RESULT_OK) {
            // Water intake activity returned successfully, update water data
            updateWaterData();
        } else if (requestCode == 1002 && resultCode == getActivity().RESULT_OK) {
            // Food selection activity returned successfully, update food data and calorie data
            updateFoodRecordsDisplay();
            updateCalorieData();
        }
    }

    private void updateCurrentWeightFromDb() {
        if (tvCurrentWeight == null) return;
        executor.execute(() -> {
            try {
                ApiResponse<WeightLogResponse> response = repository.getLatestWeight();
                if (response != null && response.isSuccess() && response.getData() != null) {
                    BigDecimal weightKg = response.getData().getLatestWeightKg();
                    mainHandler.post(() -> {
                        if (weightKg != null) {
                            tvCurrentWeight.setText(String.format(Locale.getDefault(), "%.1f kg", weightKg.floatValue()));
                        } else {
                            tvCurrentWeight.setText("-- kg");
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to load weight", e);
            }
        });
    }
    
    private void updateWaterData() {
        TextView tvWater = getView().findViewById(R.id.tvWater);
        if (tvWater == null) return;
        
        executor.execute(() -> {
            try {
                String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                ApiResponse<WaterDailySummaryResponse> response = repository.getWaterDailySummary(currentDate);
                if (response != null && response.isSuccess() && response.getData() != null) {
                    int totalIntake = response.getData().getTotalMl();
                    mainHandler.post(() -> {
                        tvWater.setText(totalIntake + "/2800ml");
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to load water data", e);
            }
        });
    }

    private void populateSevenDays(LinearLayout container, Calendar center){
        container.removeAllViews();
        int padH = dp(10), padV = dp(6);
        Calendar tmp = (Calendar) center.clone();
        tmp.add(Calendar.DAY_OF_MONTH, -3);
        for (int i=0;i<7;i++){
            final int day = tmp.get(Calendar.DAY_OF_MONTH);
            TextView chip = new TextView(requireContext());
            chip.setText(String.valueOf(day));
            chip.setPadding(padH, padV, padH, padV);
            chip.setGravity(Gravity.CENTER);
            if (i==3) {
                chip.setBackgroundResource(R.drawable.bg_day_chip);
                chip.setTextColor(0xFF2E7D32);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = dp(i==0?0:6);
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> {
                Calendar c2 = (Calendar) center.clone();
                c2.set(Calendar.DAY_OF_MONTH, day);
                populateSevenDays(container, c2);
                // TODO: load records for selected day
            });
            container.addView(chip);
            tmp.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void setupMealChipListeners(View v) {
        // Find meal chip containers and set click listeners
        LinearLayout mealsContainer = v.findViewById(R.id.mealsContainer);
        if (mealsContainer != null) {
            // Breakfast chip (first child)
            View breakfastChip = mealsContainer.getChildAt(0);
            if (breakfastChip != null) {
                breakfastChip.setOnClickListener(view -> openFoodSelection("Breakfast"));
            }

            // Lunch chip (second child)
            View lunchChip = mealsContainer.getChildAt(1);
            if (lunchChip != null) {
                lunchChip.setOnClickListener(view -> openFoodSelection("Lunch"));
            }

            // Dinner chip (third child)
            View dinnerChip = mealsContainer.getChildAt(2);
            if (dinnerChip != null) {
                dinnerChip.setOnClickListener(view -> openFoodSelection("Dinner"));
            }

            // Snack chip (fourth child)
            View snackChip = mealsContainer.getChildAt(3);
            if (snackChip != null) {
                snackChip.setOnClickListener(view -> openFoodSelection("Snack"));
            }
        }
    }

    
    private void showFoodAddSheet(String mealType) {
        // For demo purposes, using sample food data
        String sampleFoodName = "Sample Food";
        int sampleCalories = 150;
        
        FoodAddSheet sheet = FoodAddSheet.newInstance(mealType, sampleFoodName, sampleCalories);
        sheet.setOnFoodAddedListener(new FoodAddSheet.OnFoodAddedListener() {
            @Override
            public void onFoodAdded(String mealType, String foodName, int calories, int quantity, boolean isUsingGrams) {
                // Save food record to database
                saveFoodRecord(mealType, foodName, calories, quantity, isUsingGrams);
                // Update UI to show the added food
                updateFoodRecordsDisplay();
            }
        });
        sheet.show(getParentFragmentManager(), "FoodAddSheet");
    }
    
    private void saveFoodRecord(String mealType, String foodName, int calories, int quantity, boolean isUsingGrams) {
        executor.execute(() -> {
            try {
                String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                MealAddRequest request = new MealAddRequest(
                        getMealTypeCode(mealType),
                        2, // sourceType=2 (free input)
                        null, // sourceId (not needed for free input)
                        foodName,
                        isUsingGrams ? "g" : "serving",
                        new BigDecimal(quantity).doubleValue(),
                        currentDate
                );
                
                ApiResponse<MealLogIdResponse> response = repository.addMeal(request);
                if (response != null && response.isSuccess()) {
                    mainHandler.post(() -> {
                        updateFoodRecordsDisplay();
                        updateCalorieData();
                    });
                } else {
                    mainHandler.post(() -> {
                        Toast.makeText(requireContext(), "Failed to save food record", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to save food record", e);
                mainHandler.post(() -> {
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private int getMealTypeCode(String mealType) {
        switch (mealType.toLowerCase()) {
            case "breakfast": return 0;
            case "lunch": return 1;
            case "dinner": return 2;
            case "snack": return 3;
            default: return 0;
        }
    }
    
    private void updateFoodRecordsDisplay() {
        executor.execute(() -> {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String today = dateFormat.format(new Date());
                
                ApiResponse<DailySummaryResponse> response = repository.getDailySummary(today);
                Log.d(TAG, "getDailySummary response: " + (response != null ? "not null" : "null"));
                if (response != null) {
                    Log.d(TAG, "response.isSuccess(): " + response.isSuccess() + ", code: " + response.getCode());
                    Log.d(TAG, "response.getData(): " + (response.getData() != null ? "not null" : "null"));
                }
                if (response != null && response.isSuccess() && response.getData() != null) {
                    List<DailySummaryResponse.MealItemDto> allRecords = response.getData().getItems();
                    Log.d(TAG, "Found " + (allRecords != null ? allRecords.size() : "null") + " meal records");
                    
                    mainHandler.post(() -> {
                        LinearLayout foodRecordsContainer = getView().findViewById(R.id.foodRecordsContainer);
                        TextView tvNoFoodRecords = getView().findViewById(R.id.tvNoFoodRecords);
                        
                        if (foodRecordsContainer == null) return;
                        foodRecordsContainer.removeAllViews();
                        
                        if (allRecords == null || allRecords.isEmpty()) {
                            // Build placeholder programmatically to avoid parent-attach issues
                            TextView empty = new TextView(requireContext());
                            empty.setText("No food recorded today~");
                            empty.setTextSize(14);
                            empty.setTextColor(0xFF9CA3AF); // gray-400
                            int pad = dp(16);
                            empty.setPadding(pad, pad, pad, pad);
                            foodRecordsContainer.addView(empty);
                        } else {
                            // Group records by meal type
                            Map<Integer, List<DailySummaryResponse.MealItemDto>> recordsByMeal = new HashMap<>();
                            for (DailySummaryResponse.MealItemDto record : allRecords) {
                                recordsByMeal.computeIfAbsent(record.getMealType(), k -> new ArrayList<>()).add(record);
                            }
                            
                            // Display records for each meal type
                            String[] mealNames = {"Breakfast", "Lunch", "Dinner", "Snack"};
                            for (Map.Entry<Integer, List<DailySummaryResponse.MealItemDto>> entry : recordsByMeal.entrySet()) {
                                int mealType = entry.getKey();
                                String mealName = (mealType >= 0 && mealType < mealNames.length) ? mealNames[mealType] : "Other";
                                List<DailySummaryResponse.MealItemDto> mealRecords = entry.getValue();
                                
                                // Create meal header
                                TextView mealHeader = new TextView(requireContext());
                                mealHeader.setText(mealName + " (" + mealRecords.size() + " items)");
                                mealHeader.setTextSize(14);
                                mealHeader.setTextColor(getResources().getColor(android.R.color.black));
                                mealHeader.setPadding(0, 8, 0, 4);
                                foodRecordsContainer.addView(mealHeader);
                                
                                // Add food items for this meal
                                for (DailySummaryResponse.MealItemDto record : mealRecords) {
                                    View foodItemView = createFoodRecordItemView(record);
                                    foodRecordsContainer.addView(foodItemView);
                                }
                            }
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to load food records", e);
            }
        });
    }
    
    // Overloaded method for API data
    private View createFoodRecordItemView(DailySummaryResponse.MealItemDto record) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View itemView = inflater.inflate(R.layout.item_food_record, null);
        
        TextView foodName = itemView.findViewById(R.id.foodName);
        TextView foodQuantity = itemView.findViewById(R.id.foodQuantity);
        TextView foodCalories = itemView.findViewById(R.id.foodCalories);
        TextView foodTime = itemView.findViewById(R.id.foodTime);
        ImageView btnDelete = itemView.findViewById(R.id.btnDelete);
        
        // Populate data
        foodName.setText(record.getItemName());
        foodQuantity.setText(record.getUnitQty() + " " + normalizeUnitName(record.getUnitName()));
        foodCalories.setText(record.getKcal() + " kcal");
        if (record.getCreatedAt() != null && record.getCreatedAt().length() >= 16) {
            foodTime.setText(record.getCreatedAt().substring(11, 16)); // Extract HH:mm
        }
        
        btnDelete.setOnClickListener(v -> {
            deleteFoodRecord(record.getId());
        });
        
        return itemView;
    }
    
    // Legacy method for local FoodRecord (can be removed later)
    private View createFoodRecordItemView(FoodRecord record) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View itemView = inflater.inflate(R.layout.item_food_record, null);
        
        TextView foodName = itemView.findViewById(R.id.foodName);
        TextView foodQuantity = itemView.findViewById(R.id.foodQuantity);
        TextView foodCalories = itemView.findViewById(R.id.foodCalories);
        TextView foodTime = itemView.findViewById(R.id.foodTime);
        ImageView btnDelete = itemView.findViewById(R.id.btnDelete);
        
        foodName.setText(record.foodName);
        String unit = record.isUsingGrams ? "g" : "servings";
        foodQuantity.setText(record.quantity + " " + unit);
        foodCalories.setText(record.calories + " kcal");
        foodTime.setText(record.time);
        
        return itemView;
    }
    
    private void deleteFoodRecord(Long recordId) {
        executor.execute(() -> {
            try {
                ApiResponse<Void> response = repository.deleteMeal(recordId);
                if (response != null && response.isSuccess()) {
                    mainHandler.post(() -> {
                        updateFoodRecordsDisplay();
                        updateCalorieData();
                        Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    mainHandler.post(() -> {
                        Toast.makeText(requireContext(), "Failed to delete", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to delete food record", e);
                mainHandler.post(() -> {
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void updateCalorieData() {
        executor.execute(() -> {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String today = dateFormat.format(new Date());
                
                // Get daily summary from API
                ApiResponse<DailySummaryResponse> response = repository.getDailySummary(today);
                
                if (response != null && response.isSuccess() && response.getData() != null) {
                    int totalCalories = response.getData().getTotalKcal();
                    int dailyGoal = 1600;
                    int caloriesLeft = Math.max(0, dailyGoal - totalCalories);
                    
                    // Also fetch latest/initial weight to compute lost
                    ApiResponse<WeightLogResponse> w = repository.getLatestWeight();
                    BigDecimal initial = (w!=null && w.getData()!=null)? w.getData().getInitialWeightKg(): null;
                    BigDecimal latest  = (w!=null && w.getData()!=null)? w.getData().getLatestWeightKg(): null;
                    String lostText = "0.0 kg";
                    if (initial != null && latest != null) {
                        lostText = String.format(Locale.getDefault(), "%.1f kg",
                                Math.max(0f, initial.floatValue() - latest.floatValue()));
                    }
                    
                    final int tc = totalCalories;
                    final int cl = caloriesLeft;
                    final String lostFinal = lostText;
                    mainHandler.post(() -> {
                        tvIntake.setText(String.valueOf(tc));
                        tvCaloriesLeft.setText(String.valueOf(cl));
                        tvBurn.setText("0"); // TODO: Get from exercise API
                        TextView tvLost = getView().findViewById(R.id.tvWeightLost);
                        if (tvLost != null) tvLost.setText(lostFinal);
                    });
                } else {
                    mainHandler.post(() -> {
                        tvIntake.setText("0");
                        tvCaloriesLeft.setText("1600");
                        tvBurn.setText("0");
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to load calorie data", e);
                mainHandler.post(() -> {
                    tvIntake.setText("0");
                    tvCaloriesLeft.setText("1600");
                    tvBurn.setText("0");
                });
            }
        });
    }
    
    private void openFoodSelection(String mealType) {
        Intent intent = new Intent(getContext(), FoodSelectionActivity.class);
        intent.putExtra("meal_type", mealType);
        startActivityForResult(intent, 1002); // Request code for food selection
    }

    // NEW: single CTA to RunHubActivity with autoStart
    private void setupStartRunButton(View root) {
        View startBtn = root.findViewById(R.id.btnStartRunHome);
        if (startBtn != null) {
            startBtn.setOnClickListener(v -> {
                Intent i = new Intent(requireContext(), RunHubActivity.class);
                i.putExtra("autoStart", true); // auto start tracking on the hub page
                startActivity(i);
            });
        }
    }

    private int dp(int v){ return Math.round(v * getResources().getDisplayMetrics().density); }

    private String normalizeUnitName(String unit) {
        if (unit == null) return "unit";
        String u = unit.trim();
        switch (u) {
            case "份": return "serving";
            case "个":
            case "顆": return "pc";
            case "杯": return "cup";
            case "克": return "g";
            case "千克":
            case "公斤": return "kg";
            case "毫升": return "ml";
            default: return u;
        }
    }
}