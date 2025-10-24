package com.aura.starter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SimpleDrinkPagerAdapter extends RecyclerView.Adapter<SimpleDrinkPagerAdapter.PageViewHolder> {

    private List<List<DrinkType>> pages;
    private OnDrinkSelectedListener listener;
    private String selectedDrinkType = "Water"; // Default selected

    public interface OnDrinkSelectedListener {
        void onDrinkSelected(String drinkType);
    }

    public SimpleDrinkPagerAdapter(List<List<DrinkType>> pages, OnDrinkSelectedListener listener) {
        this.pages = pages;
        this.listener = listener;
    }

    public void setSelectedDrinkType(String drinkType) {
        this.selectedDrinkType = drinkType;
        notifyDataSetChanged(); // Refresh all items to update selection state
    }

    public String getSelectedDrinkType() {
        return selectedDrinkType;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_drink_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        List<DrinkType> pageDrinks = pages.get(position);
        holder.bind(pageDrinks);
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    class PageViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout row1, row2;

        public PageViewHolder(@NonNull View itemView) {
            super(itemView);
            row1 = itemView.findViewById(R.id.drinkRow1);
            row2 = itemView.findViewById(R.id.drinkRow2);
        }

        public void bind(List<DrinkType> drinks) {
            // Clear existing views
            row1.removeAllViews();
            row2.removeAllViews();

            // Add drinks to rows (4 per row, max 8 per page)
            for (int i = 0; i < drinks.size() && i < 8; i++) {
                DrinkType drink = drinks.get(i);
                View drinkView = createDrinkView(drink);

                if (i < 4) {
                    row1.addView(drinkView);
                } else {
                    row2.addView(drinkView);
                }
            }
        }

        private View createDrinkView(DrinkType drink) {
            // Create a simple LinearLayout for each drink button
            LinearLayout drinkButton = new LinearLayout(itemView.getContext());
            drinkButton.setOrientation(LinearLayout.VERTICAL);
            drinkButton.setGravity(android.view.Gravity.CENTER);
            drinkButton.setPadding(16, 16, 16, 16);

            // Set layout parameters - make buttons larger
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            params.setMargins(8, 8, 8, 8);
            drinkButton.setLayoutParams(params);

            // Create icon - make it larger
            ImageView icon = new ImageView(itemView.getContext());
            icon.setLayoutParams(new LinearLayout.LayoutParams(64, 64));
            try {
                icon.setImageResource(drink.getIconRes());
            } catch (Exception e) {
                // Use a default icon if resource fails
                icon.setImageResource(R.drawable.ic_water); // fallback icon
            }
            icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            icon.setAdjustViewBounds(true);

            // Create text - make it larger and more readable
            TextView text = new TextView(itemView.getContext());
            text.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            text.setText(drink.getName());
            text.setTextSize(16);
            text.setGravity(android.view.Gravity.CENTER);
            text.setMaxLines(1);
            text.setEllipsize(android.text.TextUtils.TruncateAt.END);

            // Add views to button
            drinkButton.addView(icon);
            drinkButton.addView(text);

            // Set background and text color based on selection state
            boolean isSelected = drink.getName().equals(selectedDrinkType);
            if (isSelected) {
                drinkButton.setBackgroundResource(R.drawable.bg_drink_selected);
                text.setTextColor(0xFF2196F3); // Blue text for selected
            } else {
                drinkButton.setBackgroundResource(R.drawable.bg_drink_unselected);
                text.setTextColor(0xFF666666); // Gray text for unselected
            }

            // Set click listener
            drinkButton.setOnClickListener(v -> {
                if (SimpleDrinkPagerAdapter.this.listener != null) {
                    SimpleDrinkPagerAdapter.this.listener.onDrinkSelected(drink.getName());
                }
            });

            return drinkButton;
        }
    }
}
