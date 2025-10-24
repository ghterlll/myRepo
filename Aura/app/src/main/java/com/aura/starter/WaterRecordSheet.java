package com.aura.starter;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

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

public class WaterRecordSheet extends BottomSheetDialogFragment {

    private static final String TAG = "WaterRecordSheet";
    private TextView tvQuantity;
    private TextView tvTime;
    private ViewPager2 viewPagerDrinks;
    private LinearLayout paginationDots;
    private SimpleDrinkPagerAdapter pagerAdapter;

    private String selectedDrinkType = "Water";
    private int currentQuantity = 400;

    private AuraRepository repository;
    private String currentDate;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Callback interface for parent activity
    public interface OnSavedListener {
        void onSaved();
    }

    private OnSavedListener listener;

    public void setOnSavedListener(OnSavedListener l) {
        listener = l;
    }

    @Override
    public void onAttach(@NonNull Context ctx) {
        super.onAttach(ctx);
        repository = new AuraRepository(ctx);
        repository.getAuthManager().initTokenToApiClient();
        currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Use custom transparent theme to create BottomSheetDialog, completely remove default background
        BottomSheetDialog d = new BottomSheetDialog(requireContext(), R.style.TransparentBottomSheetDialog);

        return d;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.sheet_water_record, c, false);

        tvQuantity = v.findViewById(R.id.tvQuantity);
        tvTime = v.findViewById(R.id.tvTime);
        viewPagerDrinks = v.findViewById(R.id.viewPagerDrinks);
        paginationDots = v.findViewById(R.id.paginationDots);

        // Setup close button
        v.findViewById(R.id.btnClose).setOnClickListener(vw -> dismiss());

        // Initialize UI
        updateTimeDisplay();
        setupViewPager();
        setupKeypad(v);

        // Setup confirm button
        v.findViewById(R.id.btnConfirm).setOnClickListener(vw -> saveWaterRecord());

        // Set initial quantity
        tvQuantity.setText(String.valueOf(currentQuantity));

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog == null) return;

        // Set BottomSheet to appropriate expanded height to show keypad
        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        // Completely fix: remove MaterialShapeDrawable background from BottomSheet container to avoid rectangular background showing behind rounded corners
        View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) {
            // Remove default MaterialShapeDrawable background and padding
            sheet.setBackground(null);
            sheet.setPadding(0, 0, 0, 0);
        }

        // Set card height to 85% of screen height to ensure keypad is fully visible
        View card = dialog.findViewById(R.id.sheetCard);
        if (card != null) {
            int screenHeight = requireContext().getResources().getDisplayMetrics().heightPixels;
            int cardHeight = (int) (screenHeight * 0.85f); // 85% height for good appearance and space
            ViewGroup.LayoutParams clp = card.getLayoutParams();
            clp.height = cardHeight;
            card.setLayoutParams(clp);
        }
    }

    private void updateTimeDisplay() {
        if (tvTime != null) {
            String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            tvTime.setText(currentTime);
        }
    }

    private void setupViewPager() {
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
            return;
        }

        viewPagerDrinks.setAdapter(pagerAdapter);
        viewPagerDrinks.setUserInputEnabled(true);

        // Setup pagination dots
        setupDots(pages.size());

        // Setup page change listener
        viewPagerDrinks.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
            }
        });
    }

    private void setupDots(int count) {
        if (paginationDots == null) return;

        paginationDots.removeAllViews();
        for (int i = 0; i < count; i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(8), dp(8));
            lp.setMargins(dp(4), 0, dp(4), 0);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(i == 0 ? R.drawable.bg_dot_selected : R.drawable.bg_dot_unselected);
            paginationDots.addView(dot);
        }
    }

    private void updateDots(int page) {
        if (paginationDots == null) return;

        for (int i = 0; i < paginationDots.getChildCount(); i++) {
            View dot = paginationDots.getChildAt(i);
            dot.setBackgroundResource(i == page ? R.drawable.bg_dot_selected : R.drawable.bg_dot_unselected);
        }
    }

    private void setupKeypad(View root) {
        // Number buttons
        int[] buttonIds = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                          R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9};

        for (int i = 0; i < buttonIds.length; i++) {
            final int number = i;
            View button = root.findViewById(buttonIds[i]);
            if (button != null) {
                button.setOnClickListener(v -> addDigit(number));
            }
        }

        // Backspace button
        View backspaceButton = root.findViewById(R.id.btnBackspace);
        if (backspaceButton != null) {
            backspaceButton.setOnClickListener(v -> removeDigit());
        }
    }

    private void addDigit(int digit) {
        if (currentQuantity > 999) return; // Limit to 4 digits
        currentQuantity = currentQuantity * 10 + digit;
        tvQuantity.setText(String.valueOf(currentQuantity));
    }

    private void removeDigit() {
        currentQuantity = currentQuantity / 10;
        tvQuantity.setText(String.valueOf(currentQuantity));
    }

    private void saveWaterRecord() {
        if (currentQuantity <= 0) {
            dismiss();
            return;
        }

        executor.execute(() -> {
            try {
                ApiResponse<Void> response = repository.addWater(currentDate, currentQuantity);
                mainHandler.post(() -> {
                    // Check if token expired
                    if (repository.checkAndHandleTokenExpired(response)) {
                        dismiss();
                        return;
                    }
                    
                    if (response != null && response.isSuccess()) {
                        if (listener != null) listener.onSaved();
                        dismiss();
                    } else {
                        String errorMsg = response != null ? response.getMessage() : "Unknown error";
                        Toast.makeText(requireContext(), "Failed: " + errorMsg, Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to save water record", e);
                mainHandler.post(() -> {
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    dismiss();
                });
            }
        });
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
