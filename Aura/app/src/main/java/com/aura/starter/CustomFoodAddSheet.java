package com.aura.starter;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class CustomFoodAddSheet extends BottomSheetDialogFragment {

    private String mealType;
    
    private EditText etFoodName;
    private EditText etWeight;
    private EditText etCalories;
    private ImageView btnClose;
    private TextView btnCancel;
    private TextView btnConfirm;
    
    private OnCustomFoodAddedListener listener;

    public interface OnCustomFoodAddedListener {
        void onCustomFoodAdded(String mealType, String foodName, int weight, int calories);
    }

    public static CustomFoodAddSheet newInstance(String mealType) {
        CustomFoodAddSheet sheet = new CustomFoodAddSheet();
        Bundle args = new Bundle();
        args.putString("meal_type", mealType);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mealType = getArguments().getString("meal_type", "Dinner");
        }
    }

    @Override
    public Dialog onCreateDialog(@NonNull Bundle savedInstanceState) {
        return new BottomSheetDialog(requireContext(), R.style.TransparentBottomSheetDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_custom_food_add, container, false);
        
        initViews(view);
        setupClickListeners();
        
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog) {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
            View sheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
                behavior.setFitToContents(false);
                behavior.setSkipCollapsed(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                
                // Remove default background and padding, make it centered
                sheet.setBackground(null);
                sheet.setPadding(0, 0, 0, 0);
                
                // Make the dialog centered by setting layout params
                ViewGroup.LayoutParams params = sheet.getLayoutParams();
                if (params != null) {
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    sheet.setLayoutParams(params);
                }
            }
        }
    }

    private void initViews(View view) {
        etFoodName = view.findViewById(R.id.etFoodName);
        etWeight = view.findViewById(R.id.etWeight);
        etCalories = view.findViewById(R.id.etCalories);
        btnClose = view.findViewById(R.id.btnClose);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnConfirm = view.findViewById(R.id.btnConfirm);
    }

    private void setupClickListeners() {
        btnClose.setOnClickListener(v -> dismiss());
        
        btnCancel.setOnClickListener(v -> dismiss());
        
        btnConfirm.setOnClickListener(v -> {
            if (validateInput()) {
                addCustomFood();
            }
        });
    }

    private boolean validateInput() {
        String foodName = etFoodName.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();
        
        if (foodName.isEmpty()) {
            Toast.makeText(getContext(), "Please enter food name", Toast.LENGTH_SHORT).show();
            etFoodName.requestFocus();
            return false;
        }
        
        if (weightStr.isEmpty()) {
            Toast.makeText(getContext(), "Please enter weight", Toast.LENGTH_SHORT).show();
            etWeight.requestFocus();
            return false;
        }
        
        try {
            int weight = Integer.parseInt(weightStr);
            if (weight <= 0) {
                Toast.makeText(getContext(), "Weight must be greater than 0", Toast.LENGTH_SHORT).show();
                etWeight.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Please enter a valid weight", Toast.LENGTH_SHORT).show();
            etWeight.requestFocus();
            return false;
        }
        
        return true;
    }

    private void addCustomFood() {
        String foodName = etFoodName.getText().toString().trim();
        int weight = Integer.parseInt(etWeight.getText().toString().trim());
        
        String caloriesStr = etCalories.getText().toString().trim();
        int calories = 0;
        
        if (!caloriesStr.isEmpty()) {
            try {
                calories = Integer.parseInt(caloriesStr);
                if (calories <= 0) {
                    Toast.makeText(getContext(), "Calories must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Please enter a valid calories value", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            // If no calories provided, calculate based on weight (rough estimate: 2 kcal per gram)
            calories = weight * 2;
        }
        
        if (listener != null) {
            listener.onCustomFoodAdded(mealType, foodName, weight, calories);
        }
        
        dismiss();
    }

    public void setOnCustomFoodAddedListener(OnCustomFoodAddedListener listener) {
        this.listener = listener;
    }
}
