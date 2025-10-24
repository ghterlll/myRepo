package com.aura.starter;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.aura.starter.network.AuraRepository;
import com.aura.starter.network.models.ApiResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class WaterRecordActivity extends AppCompatActivity {

    private static final String TAG = "WaterRecordActivity";
    private TextView tvQuantity;
    private String selectedDrinkType = "Water";
    private int currentQuantity = 400;
    private AuraRepository repository;
    private String currentDate;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ViewPager2 viewPagerDrinks;
    private LinearLayout paginationDots;
    private SimpleDrinkPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water_record);

        // Initialize repository
        repository = new AuraRepository(this);
        repository.getAuthManager().initTokenToApiClient();
        currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Initialize views and setup UI
        initializeViews();
        setupUI();
    }

    private void initializeViews() {
        try {
            // Initialize views
            tvQuantity = findViewById(R.id.tvQuantity);
            viewPagerDrinks = findViewById(R.id.viewPagerDrinks);
            paginationDots = findViewById(R.id.paginationDots);

            if (tvQuantity == null || viewPagerDrinks == null || paginationDots == null) {
                throw new RuntimeException("Required views not found in layout");
            }
        } catch (Exception e) {
            System.err.println("Error initializing views: " + e.getMessage());
            e.printStackTrace();
            finish(); // Exit if views can't be initialized
        }
    }

    private void setupUI() {
        try {
            // Setup close button
            View closeButton = findViewById(R.id.btnClose);
            if (closeButton != null) {
                closeButton.setOnClickListener(v -> finish());
            }

            // Setup time display
            updateTimeDisplay();

            // Setup ViewPager2 for drink selection
            setupViewPager();

            // Setup numeric keypad
            setupNumericKeypad();

            // Setup confirm button
            View confirmButton = findViewById(R.id.btnConfirm);
            if (confirmButton != null) {
                confirmButton.setOnClickListener(v -> saveWaterRecord());
            }
        } catch (Exception e) {
            System.err.println("Error setting up UI: " + e.getMessage());
            e.printStackTrace();
            finish(); // Exit if setup fails
        }
    }

    private void setupViewPager() {
        try {
            // Create pages with drinks (8 per page, 2 rows x 4 columns)
            List<List<DrinkType>> pages = new ArrayList<>();

            // Page 1: First 8 drinks
            List<DrinkType> page1 = Arrays.asList(
                new DrinkType("Water", R.drawable.ic_water),
                new DrinkType("Mineral", R.drawable.ic_mineral_water),
                new DrinkType("Soda", R.drawable.ic_soda_water),
                new DrinkType("Tea", R.drawable.ic_tea),
                new DrinkType("Sparkling", R.drawable.ic_effervescent),
                new DrinkType("Sports", R.drawable.ic_sports_drink),
                new DrinkType("Soup", R.drawable.ic_soup),
                new DrinkType("Coconut", R.drawable.ic_coconut_water)
            );
            pages.add(page1);

            // Page 2: Remaining drinks
            List<DrinkType> page2 = Arrays.asList(
                new DrinkType("Coffee", R.drawable.ic_coffee),
                new DrinkType("Juice", R.drawable.ic_juice),
                new DrinkType("Milk", R.drawable.ic_milk),
                new DrinkType("Beer", R.drawable.ic_beer)
            );
            pages.add(page2);

            pagerAdapter = new SimpleDrinkPagerAdapter(pages, drinkType -> {
                selectedDrinkType = drinkType;
                pagerAdapter.setSelectedDrinkType(drinkType);
            });

            if (viewPagerDrinks == null) {
                System.err.println("ViewPager2 is null!");
                return;
            }

            viewPagerDrinks.setAdapter(pagerAdapter);

            // Set initial selection
            pagerAdapter.setSelectedDrinkType(selectedDrinkType);

            // Enable user input for swiping
            viewPagerDrinks.setUserInputEnabled(true);

            // Setup pagination dots
            setupPaginationDots(pages.size());

            // Setup page change listener
            viewPagerDrinks.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    updatePaginationDots(position);
                }
            });

        } catch (Exception e) {
            System.err.println("Error setting up ViewPager2: " + e.getMessage());
            e.printStackTrace();
            // Don't finish immediately, let user see the error
        }
    }

    private void setupPaginationDots(int pageCount) {
        paginationDots.removeAllViews();
        for (int i = 0; i < pageCount; i++) {
            View dot = new View(this);
            dot.setLayoutParams(new LinearLayout.LayoutParams(16, 16));
            dot.setBackgroundResource(i == 0 ? R.drawable.bg_dot_selected : R.drawable.bg_dot_unselected);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) dot.getLayoutParams();
            params.setMargins(8, 0, 8, 0);
            dot.setLayoutParams(params);
            paginationDots.addView(dot);
        }
    }

    private void updatePaginationDots(int selectedPage) {
        for (int i = 0; i < paginationDots.getChildCount(); i++) {
            View dot = paginationDots.getChildAt(i);
            dot.setBackgroundResource(i == selectedPage ? R.drawable.bg_dot_selected : R.drawable.bg_dot_unselected);
        }
    }




    private void setupNumericKeypad() {
        try {
            // Number buttons
            int[] numberButtons = {
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
            };

            for (int i = 0; i < numberButtons.length; i++) {
                final int number = i;
                View button = findViewById(numberButtons[i]);
                if (button != null) {
                    button.setOnClickListener(v -> addDigit(number));
                }
            }

            // Backspace button
            View backspaceButton = findViewById(R.id.btnBackspace);
            if (backspaceButton != null) {
                backspaceButton.setOnClickListener(v -> removeDigit());
            }
        } catch (Exception e) {
            System.err.println("Error setting up numeric keypad: " + e.getMessage());
            e.printStackTrace();
            finish(); // Exit if setup fails
        }
    }

    private void addDigit(int digit) {
        try {
            if (currentQuantity > 999) return; // Limit to 4 digits
            currentQuantity = currentQuantity * 10 + digit;
            updateQuantityDisplay();
        } catch (Exception e) {
            System.err.println("Error in addDigit: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void removeDigit() {
        try {
            currentQuantity = currentQuantity / 10;
            updateQuantityDisplay();
        } catch (Exception e) {
            System.err.println("Error in removeDigit: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateQuantityDisplay() {
        try {
            if (tvQuantity != null) {
                tvQuantity.setText(String.valueOf(currentQuantity));
            }
        } catch (Exception e) {
            System.err.println("Error updating quantity display: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateTimeDisplay() {
        try {
            TextView tvTime = findViewById(R.id.tvTime);
            if (tvTime != null) {
                String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                tvTime.setText(currentTime);
            }
        } catch (Exception e) {
            System.err.println("Error updating time display: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveWaterRecord() {
        if (currentQuantity <= 0) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        executor.execute(() -> {
            try {
                ApiResponse<Void> response = repository.addWater(currentDate, currentQuantity);
                mainHandler.post(() -> {
                    if (response != null && response.isSuccess()) {
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("record_added", true);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to save water record", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to save water record", e);
                mainHandler.post(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                    finish();
                });
            }
        });
    }
}
