package com.aura.starter;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FoodAddSheet extends BottomSheetDialogFragment {

    private String mealType;
    private String foodName;
    private int caloriesPerServing;
    private int currentQuantity = 1;
    private boolean isUsingGrams = false;
    private int gramsPerServing = 150; // Default 150g per serving
    
    private TextView tvFoodName;
    private TextView tvCalories;
    private TextView tvQuantity;
    private TextView tvEdibleWeight;
    private TextView btnUnitGrams;
    private TextView btnUnitServings;
    private TextView tvTime;
    
    private OnFoodAddedListener listener;

    public interface OnFoodAddedListener {
        void onFoodAdded(String mealType, String foodName, int calories, int quantity, boolean isUsingGrams);
    }

    public static FoodAddSheet newInstance(String mealType, String foodName, int caloriesPerServing) {
        FoodAddSheet sheet = new FoodAddSheet();
        Bundle args = new Bundle();
        args.putString("meal_type", mealType);
        args.putString("food_name", foodName);
        args.putInt("calories_per_serving", caloriesPerServing);
        sheet.setArguments(args);
        return sheet;
    }
    
    public void setInitialValues(int quantity, boolean isUsingGrams) {
        this.currentQuantity = quantity;
        this.isUsingGrams = isUsingGrams;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mealType = getArguments().getString("meal_type", "Breakfast");
            foodName = getArguments().getString("food_name", "Food Item");
            caloriesPerServing = getArguments().getInt("calories_per_serving", 100);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Use custom transparent theme to create BottomSheetDialog
        BottomSheetDialog d = new BottomSheetDialog(requireContext(), R.style.TransparentBottomSheetDialog);
        return d;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.sheet_food_add, container, false);
        
        initViews(v);
        setupClickListeners(v);
        updateDisplay();
        
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog == null) return;

        // Set BottomSheet to appropriate expanded height
        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        // Remove default MaterialShapeDrawable background from BottomSheet container
        View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) {
            sheet.setBackground(null);
            sheet.setPadding(0, 0, 0, 0);
        }

        // Set card height to 75% of screen height
        View card = dialog.findViewById(R.id.sheetCard);
        if (card != null) {
            int screenHeight = requireContext().getResources().getDisplayMetrics().heightPixels;
            int cardHeight = (int) (screenHeight * 0.75f);
            ViewGroup.LayoutParams clp = card.getLayoutParams();
            clp.height = cardHeight;
            card.setLayoutParams(clp);
        }
    }

    private void initViews(View v) {
        tvFoodName = v.findViewById(R.id.tvFoodName);
        tvCalories = v.findViewById(R.id.tvCalories);
        tvQuantity = v.findViewById(R.id.tvQuantity);
        tvEdibleWeight = v.findViewById(R.id.tvEdibleWeight);
        btnUnitGrams = v.findViewById(R.id.btnUnitGrams);
        btnUnitServings = v.findViewById(R.id.btnUnitServings);
        tvTime = v.findViewById(R.id.tvTime);
        
        // Set food information
        tvFoodName.setText(foodName);
        updateCaloriesDisplay();
        updateTimeDisplay();
        updateUnitDisplay();
    }

    private void setupClickListeners(View v) {
        // Close button
        v.findViewById(R.id.btnClose).setOnClickListener(view -> dismiss());
        
        // Unit selection buttons
        btnUnitGrams.setOnClickListener(view -> selectUnit(true));
        btnUnitServings.setOnClickListener(view -> selectUnit(false));
        
        // Confirm button
        v.findViewById(R.id.btnConfirm).setOnClickListener(view -> confirmFoodAdd());
        
        // Setup numeric keypad
        setupKeypad(v);
    }

    private void setupKeypad(View v) {
        // Number buttons 0-9
        for (int i = 0; i <= 9; i++) {
            int buttonId = getResources().getIdentifier("btn" + i, "id", requireContext().getPackageName());
            View button = v.findViewById(buttonId);
            if (button != null) {
                final int number = i;
                button.setOnClickListener(view -> addDigit(number));
            }
        }
        
        // Decimal point button
        v.findViewById(R.id.btnDot).setOnClickListener(view -> addDecimal());
        
        // Backspace button
        v.findViewById(R.id.btnBackspace).setOnClickListener(view -> removeLastDigit());
    }

    private void addDigit(int digit) {
        String currentText = tvQuantity.getText().toString();
        if (currentText.equals("0")) {
            tvQuantity.setText(String.valueOf(digit));
        } else {
            tvQuantity.setText(currentText + digit);
        }
        updateCurrentQuantity();
    }

    private void addDecimal() {
        String currentText = tvQuantity.getText().toString();
        if (!currentText.contains(".")) {
            tvQuantity.setText(currentText + ".");
        }
        updateCurrentQuantity();
    }

    private void removeLastDigit() {
        String currentText = tvQuantity.getText().toString();
        if (currentText.length() > 1) {
            tvQuantity.setText(currentText.substring(0, currentText.length() - 1));
        } else {
            tvQuantity.setText("0");
        }
        updateCurrentQuantity();
    }

    private void updateCurrentQuantity() {
        try {
            currentQuantity = (int) Float.parseFloat(tvQuantity.getText().toString());
        } catch (NumberFormatException e) {
            currentQuantity = 1;
        }
        updateCaloriesDisplay();
    }

    private void selectUnit(boolean useGrams) {
        isUsingGrams = useGrams;
        updateDisplay();
    }

    private void updateDisplay() {
        updateUnitDisplay();
        if (isUsingGrams) {
            tvQuantity.setText(String.valueOf(currentQuantity * gramsPerServing));
        } else {
            tvQuantity.setText(String.valueOf(currentQuantity));
        }
        updateCaloriesDisplay();
    }
    
    private void updateUnitDisplay() {
        if (isUsingGrams) {
            btnUnitGrams.setTextColor(getResources().getColor(R.color.auragreen_primary));
            btnUnitServings.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tvEdibleWeight.setVisibility(View.INVISIBLE); // Hide but keep space when using grams
        } else {
            btnUnitGrams.setTextColor(getResources().getColor(android.R.color.darker_gray));
            btnUnitServings.setTextColor(getResources().getColor(R.color.auragreen_primary));
            tvEdibleWeight.setVisibility(View.VISIBLE); // Show when using servings
        }
    }

    private void updateCaloriesDisplay() {
        int totalCalories;
        if (isUsingGrams) {
            // Calculate calories based on grams
            float grams = Float.parseFloat(tvQuantity.getText().toString());
            float servings = grams / gramsPerServing;
            totalCalories = Math.round(servings * caloriesPerServing);
        } else {
            // Calculate calories based on servings
            float servings = Float.parseFloat(tvQuantity.getText().toString());
            totalCalories = Math.round(servings * caloriesPerServing);
        }
        tvCalories.setText(totalCalories + " calories");
    }

    private void updateTimeDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        tvTime.setText(sdf.format(new Date()));
    }

    private void confirmFoodAdd() {
        if (listener != null) {
            int totalCalories;
            if (isUsingGrams) {
                float grams = Float.parseFloat(tvQuantity.getText().toString());
                float servings = grams / gramsPerServing;
                totalCalories = Math.round(servings * caloriesPerServing);
                listener.onFoodAdded(mealType, foodName, totalCalories, (int) grams, true);
            } else {
                float servings = Float.parseFloat(tvQuantity.getText().toString());
                totalCalories = Math.round(servings * caloriesPerServing);
                listener.onFoodAdded(mealType, foodName, totalCalories, (int) servings, false);
            }
        }
        dismiss();
    }

    public void setOnFoodAddedListener(OnFoodAddedListener listener) {
        this.listener = listener;
    }
}
