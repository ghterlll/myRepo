package com.aura.starter;

import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.aura.starter.network.ApiClient;
import com.aura.starter.network.ApiService;
import com.aura.starter.network.PingResp;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.math.BigDecimal;

import com.aura.starter.network.ApiClient;
import com.aura.starter.network.ApiService;
import com.aura.starter.network.FoodListResp;
import com.aura.starter.network.AuraRepository;
import com.aura.starter.network.models.*;
import com.aura.starter.data.FoodRecord;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FoodSelectionActivity extends AppCompatActivity {

    private static final String TAG = "FoodSelectionActivity";
    // Backend-only toggle used in code paths; default false to allow mock + backend
    private static final boolean USE_BACKEND_ONLY = false;
    private TextView tvMealType;
    private EditText etSearch;
    private String currentMealType = "Breakfast";
    private long currentSessionId = System.currentTimeMillis(); // Unique ID for current session

    private AuraRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private LinearLayout foodContainer;
    private static final String PREF_FOOD = "food_prefs";
    private static final String KEY_RECENTS = "recent_foods_v1";
    private static final int RECENT_LIMIT = 20;
    
    // Bottom summary bar
    private LinearLayout bottomSummaryBar;
    private LinearLayout summaryHeader;
    private LinearLayout expandableFoodList;
    private LinearLayout foodListContainer;
    private TextView tvMealSummary;
    private TextView tvFoodCount;
    private TextView btnComplete;
    private TextView btnClear;
    private ImageView ivExpandArrow;
    
    private boolean isExpanded = false;

    // Pending selection state (client-side only, no DB)
    private static class PendingMealItem {
        final String mealType;
        final String foodName;
        final int caloriesPerUnit;
        final int quantity;
        final boolean usingGrams;
        final int imageResId; // simple placeholder; backend could provide URL later
        Long foodItemId; // backend food id when available
        String unitName; // backend unit name
        PendingMealItem(String mealType, String foodName, int caloriesPerUnit, int quantity, boolean usingGrams) {
            this.mealType = mealType;
            this.foodName = foodName;
            this.caloriesPerUnit = caloriesPerUnit;
            this.quantity = quantity;
            this.usingGrams = usingGrams;
            this.imageResId = android.R.drawable.ic_menu_gallery;
        }
        int totalKcal(){ return caloriesPerUnit * quantity; }
    }
    private final List<PendingMealItem> pendingItems = new ArrayList<>();
    private int pendingTotalKcal = 0;

    private PendingMealItem findPending(String mealType, String foodName){
        for (PendingMealItem it : pendingItems){
            if (it.mealType.equalsIgnoreCase(mealType) && it.foodName.equalsIgnoreCase(foodName)){
                return it;
            }
        }
        return null;
    }
    private boolean isPending(String mealType, String foodName){
        return findPending(mealType, foodName) != null;
    }
    private void upsertPending(String mealType, String foodName, int caloriesPerUnit, int quantity, boolean usingGrams){
        PendingMealItem exist = findPending(mealType, foodName);
        if (exist != null){
            pendingTotalKcal -= exist.totalKcal();
            pendingItems.remove(exist);
        }
        PendingMealItem item = new PendingMealItem(mealType, foodName, caloriesPerUnit, quantity, usingGrams);
        pendingItems.add(item);
        pendingTotalKcal += item.totalKcal();
    }

    private TextView selectedCategoryView;
    private final Map<Integer, String> idToCategory = new HashMap<>();

    private String currentCategory = "Common";

    // Mock data
    private List<FoodItem> commonFoods, breakfastFoods, meatFoods, vegetarianFoods,
            foreignFoods, stapleFoods, fruitFoods, snackFoods, drinkFoods, otherFoods;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_selection);

        // Initialize repository
        repository = new AuraRepository(this);
        repository.getAuthManager().initTokenToApiClient();

        String mealType = getIntent().getStringExtra("meal_type");
        if (mealType != null) currentMealType = mealType;

        initViews();
        initMockData();
        setupIdMap();
        setupListeners();

        TextView tvCommon = findViewById(R.id.categoryCommon);
        applyCategoryStyle(tvCommon, true);
        selectedCategoryView = tvCommon;

        // Try backend fetch first; on failure fallback to mock
        String initialQuery = etSearch.getText() == null ? "" : etSearch.getText().toString();
        if (!"Common".equals(currentCategory)) {
            fetchFoodsFromBackend(currentCategory, initialQuery);
        } else {
            loadFoodsForCategory(currentCategory);
        }
    }

    private void initViews() {
        tvMealType = findViewById(R.id.tvMealType);
        etSearch = findViewById(R.id.etSearch);
        foodContainer = findViewById(R.id.foodContainer);
        
        // Initialize bottom summary bar
        bottomSummaryBar = findViewById(R.id.bottomSummaryBar);
        summaryHeader = findViewById(R.id.summaryHeader);
        expandableFoodList = findViewById(R.id.expandableFoodList);
        foodListContainer = findViewById(R.id.foodListContainer);
        tvMealSummary = findViewById(R.id.tvMealSummary);
        tvFoodCount = findViewById(R.id.tvFoodCount);
        btnComplete = findViewById(R.id.btnComplete);
        btnClear = findViewById(R.id.btnClear);
        ivExpandArrow = findViewById(R.id.ivExpandArrow);
        
        // Set up click listeners
        btnComplete.setOnClickListener(view -> {
            submitPendingItems();
        });
        btnClear.setOnClickListener(v -> {
            pendingItems.clear();
            pendingTotalKcal = 0;
            updateSummary();
        });
        summaryHeader.setOnClickListener(view -> toggleExpand());
        
        tvMealType.setText(currentMealType);
        updateSummary();
        
        // Initially hide the bottom summary bar
        bottomSummaryBar.setVisibility(View.GONE);
    }

    private void setupIdMap() {
        idToCategory.put(R.id.categoryCommon, "Common");
        idToCategory.put(R.id.categoryBreakfast, "Breakfast");
        idToCategory.put(R.id.categoryMeat, "Meat Dishes");
        idToCategory.put(R.id.categoryVegetarian, "Vegetarian");
        idToCategory.put(R.id.categoryForeign, "Foreign Cuisine");
        idToCategory.put(R.id.categoryStaple, "Staple Food");
        idToCategory.put(R.id.categoryFruit, "Fruit");
        idToCategory.put(R.id.categorySnacks, "Snacks");
        idToCategory.put(R.id.categoryDrinks, "Soups & Drinks");
        idToCategory.put(R.id.categoryOther, "Other");
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.mealDropdown).setOnClickListener(v -> showMealTypeDialog());
        findViewById(R.id.btnCustomAdd).setOnClickListener(v -> showCustomFoodAddSheet());

        View.OnClickListener categoryClick = v -> {
            if (!(v instanceof TextView)) return;

            if (selectedCategoryView != null) applyCategoryStyle(selectedCategoryView, false);
            selectedCategoryView = (TextView) v;
            applyCategoryStyle(selectedCategoryView, true);

            String cat = idToCategory.get(v.getId());
            if (cat != null) {
                currentCategory = cat;
                String kw = etSearch.getText() == null ? "" : etSearch.getText().toString();
                if ("Common".equals(cat)) {
                    loadFoodsForCategory(cat); // recents only
                } else if (USE_BACKEND_ONLY) {
                    fetchFoodsFromBackend(cat, kw);
                } else {
                    loadFoodsForCategory(cat);
                }
            }
        };
        // Bind all categories
        for (Integer id : idToCategory.keySet()) {
            View tv = findViewById(id);
            if (tv != null) tv.setOnClickListener(categoryClick);
        }

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            performSearch(etSearch.getText().toString());
            return true;
        });
    }

    private void showMealTypeDialog() {
        String[] mealTypes = {"Breakfast", "Lunch", "Dinner", "Snack"};
        new AlertDialog.Builder(this)
                .setTitle("Select Meal Type")
                .setItems(mealTypes, (dialog, which) -> {
                    currentMealType = mealTypes[which];
                    tvMealType.setText(currentMealType);
                })
                .show();
    }

    // ---------- Styles (no drawables needed) ----------
    private void applyCategoryStyle(TextView tv, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(0);
            if (selected) {
                bg.setColor(0xFF54B266);
                tv.setTextColor(0xFFFFFFFF);
            } else {
                bg.setColor(0x00000000);
                tv.setTextColor(0xFF000000);
            }
            tv.setBackground(bg);
    }

    // ---------- Mock data ----------
    private void initMockData() {
        commonFoods = new ArrayList<>(); // no factory defaults in Common

        breakfastFoods = new ArrayList<>();
        breakfastFoods.add(new FoodItem("Egg", "76 kcal / 1 pc", "Low GI"));
        breakfastFoods.add(new FoodItem("Milk", "132 kcal / 1 cup", "Low GI"));
        breakfastFoods.add(new FoodItem("Soy Milk", "43 kcal / 1 cup", "Low GI"));
        breakfastFoods.add(new FoodItem("Whole Wheat Bread", "127 kcal / 1 slice", "Low GI"));
        breakfastFoods.add(new FoodItem("Fried Egg", "101 kcal / 1 pc", "Low GI"));

        meatFoods = new ArrayList<>();
        meatFoods.add(new FoodItem("Braised Beef", "236 kcal / 1 plate", "Low GI"));
        meatFoods.add(new FoodItem("Chicken Drumstick", "153 kcal / 1 pc", "Low GI"));
        meatFoods.add(new FoodItem("Pan-fried Chicken Breast", "150 kcal / 1 pc", "Low GI"));
        meatFoods.add(new FoodItem("Red Shrimp", "8 kcal / 1 pc", "Low GI"));

        vegetarianFoods = new ArrayList<>();
        vegetarianFoods.add(new FoodItem("Stir-fried Vegetables", "120 kcal / 1 plate", "Low GI"));
        vegetarianFoods.add(new FoodItem("Tofu", "80 kcal / 1 pc", "Low GI"));

        foreignFoods = new ArrayList<>();
        stapleFoods = new ArrayList<>();
        fruitFoods = new ArrayList<>();
        snackFoods = new ArrayList<>();
        drinkFoods = new ArrayList<>();
        otherFoods = new ArrayList<>();
    }

    // ---------- Rendering & search ----------
    private void loadFoodsForCategory(String category) {
        if (USE_BACKEND_ONLY) {
            if (!"Common".equals(category)) {
                String kw = etSearch.getText()==null?"":etSearch.getText().toString();
                fetchFoodsFromBackend(category, kw);
                return;
            }
        }
        foodContainer.removeAllViews();
        List<FoodItem> foods = getFoodsForCategory(category);
        String kw = etSearch.getText() == null ? "" : etSearch.getText().toString().trim().toLowerCase();

        for (FoodItem f : foods) {
            if (!kw.isEmpty() && !f.name.toLowerCase().contains(kw)) continue;
            View v = createFoodItemView(f);
            foodContainer.addView(v);

            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(0xFFE0E0E0);
            foodContainer.addView(divider);
        }

        if (foodContainer.getChildCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText("No items");
            empty.setPadding(dp(32), dp(32), dp(32), dp(32));
            foodContainer.addView(empty);
        }
    }

    private List<FoodItem> getFoodsForCategory(String category) {
        switch (category) {
            case "Common": {
                List<FoodItem> recents = loadRecentFoods();
                return (recents.isEmpty()? commonFoods : recents);
            }
            case "Breakfast": return breakfastFoods;
            case "Meat Dishes": return meatFoods;
            case "Vegetarian": return vegetarianFoods;
            case "Foreign Cuisine": return foreignFoods;
            case "Staple Food": return stapleFoods;
            case "Fruit": return fruitFoods;
            case "Snacks": return snackFoods;
            case "Soups & Drinks": return drinkFoods;
            case "Other": return otherFoods;
            default: return commonFoods;
        }
    }

    private void performSearch(String query) {
        loadFoodsForCategory(currentCategory);
        // optional: verify backend connectivity
        ApiService api = ApiClient.get().create(ApiService.class);
        api.ping().enqueue(new Callback<PingResp>() {
            @Override public void onResponse(Call<PingResp> call, Response<PingResp> response) {
                if (response.isSuccessful() && response.body()!=null) {
                    // show once for confirmation
                    Toast.makeText(FoodSelectionActivity.this, "Ping: "+response.body().msg, Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<PingResp> call, Throwable t) {
                // silent fail to avoid noise
            }
        });
    }

    private View createFoodItemView(FoodItem food) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(16), dp(8), dp(16), dp(8));
        row.setClickable(true);
        row.setBackgroundResource(android.R.drawable.list_selector_background);

        ImageView iv = new ImageView(this);
        iv.setImageResource(android.R.drawable.ic_menu_gallery);
        LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(dp(48), dp(48));
        ivLp.setMargins(0, 0, dp(12), 0);
        iv.setLayoutParams(ivLp);
        row.addView(iv);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        col.setLayoutParams(colLp);

        TextView gi = new TextView(this);
        gi.setTextSize(10);
        gi.setPadding(dp(6), dp(2), dp(6), dp(2));
        if (food.giLabel == null || food.giLabel.trim().isEmpty()) {
            gi.setVisibility(View.GONE);
        } else if (food.giLabel.contains("Low")) {
            gi.setText(food.giLabel);
            gi.setBackgroundColor(0xFFE9F7EA);
            gi.setTextColor(0xFF54B266);
        } else if (food.giLabel.contains("Medium")) {
            gi.setText(food.giLabel);
            gi.setBackgroundColor(0xFFFFC9A6);
            gi.setTextColor(0xFFFFFFFF);
        } else {
            gi.setText(food.giLabel);
            gi.setBackgroundColor(0xFFFFD6D9);
            gi.setTextColor(0xFFB00020);
        }

        TextView name = new TextView(this);
        name.setText(food.name);
        name.setTextSize(16);
        name.setTextColor(0xFF000000);

        TextView cal = new TextView(this);
        cal.setText(food.calories);
        cal.setTextSize(12);
        cal.setTextColor(0xFF666666);

        col.addView(gi);
        col.addView(name);
        col.addView(cal);
        row.addView(col);

        // Check if food is already selected (no duplicates, edit only)
        boolean isAdded = isPending(currentMealType, food.name);
        
        ImageView add = new ImageView(this);
        if (isAdded) {
            add.setImageResource(android.R.drawable.checkbox_on_background);
            add.setColorFilter(getResources().getColor(R.color.auragreen_primary));
        } else {
            add.setImageResource(android.R.drawable.ic_input_add);
            add.clearColorFilter();
        }
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(dp(28), dp(28));
        add.setLayoutParams(addLp);
        add.setOnClickListener(v -> {
            // If already selected, open editor to modify instead of duplicating
            showFoodAddSheet(food);
        });
        row.addView(add);

        // Make the entire row clickable
        row.setOnClickListener(v -> showFoodAddSheet(food));

        return row;
    }
    
    private boolean isFoodAlreadyAdded(String foodName) {
        // TODO: Check via API
        return false;
        /*
        try {
            
            // Get today's date
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = dateFormat.format(new Date());
            
            // Check if food is already added for current meal type AND current session
            List<FoodRecord> records = foodDao.getFoodRecordsByDateAndMeal(today, currentMealType);
            for (FoodRecord record : records) {
                if (record.foodName.equals(foodName) && record.sessionId == currentSessionId) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
        */
    }

    private void showFoodAddSheet(FoodItem food) {
        // Extract calories from the calories string (e.g., "91 kcal / 1 serving" -> 91)
        int caloriesPerServing = 100; // Default value
        try {
            String caloriesStr = food.calories.split(" ")[0];
            caloriesPerServing = Integer.parseInt(caloriesStr);
        } catch (Exception e) {
            // Use default value if parsing fails
        }
        
        FoodAddSheet sheet = FoodAddSheet.newInstance(currentMealType, food.name, caloriesPerServing);
        sheet.setOnFoodAddedListener(new FoodAddSheet.OnFoodAddedListener() {
            @Override
            public void onFoodAdded(String mealType, String foodName, int calories, int quantity, boolean isUsingGrams) {
                upsertPending(mealType, foodName, calories, quantity, isUsingGrams);
                // attach backend id/unit if available
                PendingMealItem last = findPending(mealType, foodName);
                if (last != null) {
                    last.foodItemId = food.id;
                    last.unitName = food.unitName;
                }
                // record recent selection
                recordRecentFood(food);
                Toast.makeText(FoodSelectionActivity.this, "Selected: " + foodName, Toast.LENGTH_SHORT).show();
                updateSummary();
                refreshFoodList();
            }
        });
        sheet.show(getSupportFragmentManager(), "FoodAddSheet");
    }
    
    private void updateOrCreateFoodRecord(String mealType, String foodName, int calories, int quantity, boolean isUsingGrams) {
        // TODO: Use API to add meal
        saveFoodRecord(mealType, foodName, calories, quantity, isUsingGrams);
        /*
        try {
            
            // Get today's date
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = dateFormat.format(new Date());
            
            // Check if food already exists in current session
            List<FoodRecord> existingRecords = foodDao.getFoodRecordsByDateAndMeal(today, mealType);
            FoodRecord existingRecord = null;
            
            for (FoodRecord record : existingRecords) {
                if (record.foodName.equals(foodName) && record.sessionId == currentSessionId) {
                    existingRecord = record;
                    break;
                }
            }
            
            if (existingRecord != null) {
                // Update existing record
                existingRecord.calories = calories;
                existingRecord.quantity = quantity;
                existingRecord.isUsingGrams = isUsingGrams;
                existingRecord.timestamp = System.currentTimeMillis();
                foodDao.updateFoodRecord(existingRecord);
            } else {
                // Create new record
                FoodRecord record = new FoodRecord(mealType, foodName, calories, quantity, isUsingGrams, currentSessionId);
                foodDao.insertFoodRecord(record);
            }
            
        } catch (Exception e) {
            Toast.makeText(this, "Failed to save food record", Toast.LENGTH_SHORT).show();
        }
        */
    }
    
    private void refreshFoodList() {
        // Refresh the food list to update the add/checkmark buttons
        loadFoodsForCategory(currentCategory);
    }
    
    private void saveFoodRecord(String mealType, String foodName, int calories, int quantity, boolean isUsingGrams) {
        executor.execute(() -> {
            try {
                String currentDate = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
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
                mainHandler.post(() -> {
                    if (response != null && response.isSuccess()) {
                        Toast.makeText(this, "Added: " + foodName, Toast.LENGTH_SHORT).show();
                        updateSummary();
                        refreshFoodList();
                    } else {
                        Toast.makeText(this, "Failed to add food", Toast.LENGTH_SHORT).show();
                    }
                });
        } catch (Exception e) {
                Log.e(TAG, "Failed to save food record", e);
                mainHandler.post(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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

    private void fetchFoodsFromBackend(String category, String query) {
        ApiService api = ApiClient.get().create(ApiService.class);
        String catParam = mapCategory(category);
        api.searchFoods(query == null ? "" : query, catParam, 30, 0, "system").enqueue(new Callback<ApiResponse<FoodSearchResponse>>() {
            @Override public void onResponse(Call<ApiResponse<FoodSearchResponse>> call, Response<ApiResponse<FoodSearchResponse>> response) {
                if ("Common".equals(category)) return; // Common uses recents only
                if (!response.isSuccessful() || response.body()==null || response.body().getData()==null) {
                    Toast.makeText(FoodSelectionActivity.this,
                            "foods API failed: code=" + response.code(), Toast.LENGTH_SHORT).show();
                    if (!USE_BACKEND_ONLY) {
                        loadFoodsForCategory(category);
                    } else {
                        // clear list when forcing backend-only
                        foodContainer.removeAllViews();
                        TextView empty = new TextView(FoodSelectionActivity.this);
                        empty.setText("No items");
                        empty.setPadding(dp(32), dp(32), dp(32), dp(32));
                        foodContainer.addView(empty);
                    }
                    return;
                }
                List<FoodItem> list = new ArrayList<>();
                for (FoodSearchResponse.FoodItemDto it : response.body().getData().getItems()) {
                    String gi = it.getGiLabel()==null?"":(it.getGiLabel().equalsIgnoreCase("low")?"Low GI": it.getGiLabel().equalsIgnoreCase("medium")?"Medium GI":"High GI");
                    FoodItem f = new FoodItem(
                            it.getName(),
                            it.getKcalPerUnit()+" kcal / "+(it.getUnitName()==null?"1":it.getUnitName()),
                            gi,
                            it.getId(),
                            it.getUnitName(),
                            it.getKcalPerUnit()
                    );
                    list.add(f);
                }
                // render
                foodContainer.removeAllViews();
                for (FoodItem f : list) {
                    if (query!=null && !query.isEmpty() && !f.name.toLowerCase().contains(query.toLowerCase())) continue;
                    View v = createFoodItemView(f);
                    foodContainer.addView(v);
                    View divider = new View(FoodSelectionActivity.this);
                    divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
                    divider.setBackgroundColor(0xFFE0E0E0);
                    foodContainer.addView(divider);
                }
                Toast.makeText(FoodSelectionActivity.this,
                        "Loaded " + list.size() + " items from backend", Toast.LENGTH_SHORT).show();
                if (foodContainer.getChildCount()==0){
                    TextView empty = new TextView(FoodSelectionActivity.this);
                    empty.setText("No items");
                    empty.setPadding(dp(32), dp(32), dp(32), dp(32));
                    foodContainer.addView(empty);
                }
            }
            @Override public void onFailure(Call<ApiResponse<FoodSearchResponse>> call, Throwable t) {
                Toast.makeText(FoodSelectionActivity.this,
                        "foods API error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                loadFoodsForCategory(category);
            }
        });
    }

    private String mapCategory(String category){
        if (category==null) return null;
        switch (category){
            case "Breakfast": return "breakfast";
            case "Meat Dishes": return "staple";
            case "Vegetarian": return "vegetable";
            case "Foreign Cuisine": return "foreign";
            case "Staple Food": return "staple";
            case "Fruit": return "fruit";
            case "Snacks": return "snack";
            case "Soups & Drinks": return "drink";
            default: return null;
        }
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(v * d);
    }

    private static class FoodItem {
        final String name, calories, giLabel;
        final Long id;          // backend food id
        final String unitName;  // backend unit name
        final int kcalPerUnit;  // backend kcal per unit
        FoodItem(String n, String c, String g){ this(n,c,g,null,null,0); }
        FoodItem(String n, String c, String g, Long id, String unitName, int kcalPerUnit) {
            name = n; calories = c; giLabel = g; this.id = id; this.unitName = unitName; this.kcalPerUnit = kcalPerUnit;
        }
    }
    
    private void updateSummary() {
            tvMealSummary.setText(currentMealType);
        tvFoodCount.setText("Pending " + pendingItems.size() + " items, " + pendingTotalKcal + " kcal");
        if (pendingItems.isEmpty()) {
            bottomSummaryBar.setVisibility(View.GONE);
            isExpanded = false;
            expandableFoodList.setVisibility(View.GONE);
            ivExpandArrow.setImageResource(android.R.drawable.arrow_up_float);
        } else {
            bottomSummaryBar.setVisibility(View.VISIBLE);
        }
        updateExpandableFoodList();
    }
    
    private void toggleExpand() {
        isExpanded = !isExpanded;
        if (isExpanded) {
            expandableFoodList.setVisibility(View.VISIBLE);
            ivExpandArrow.setImageResource(android.R.drawable.arrow_down_float);
        } else {
            expandableFoodList.setVisibility(View.GONE);
            ivExpandArrow.setImageResource(android.R.drawable.arrow_up_float);
        }
    }
    
    private void updateExpandableFoodList() {
        foodListContainer.removeAllViews();
        for (int i = 0; i < pendingItems.size(); i++) {
            PendingMealItem it = pendingItems.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(dp(8), dp(6), dp(8), dp(6));

            ImageView iv = new ImageView(this);
            iv.setImageResource(it.imageResId);
            LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(dp(40), dp(40));
            ivLp.setMargins(0, 0, dp(8), 0);
            iv.setLayoutParams(ivLp);

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView title = new TextView(this);
            title.setText(it.foodName);
            title.setTextSize(14);
            title.setTextColor(0xFF000000);

            TextView sub = new TextView(this);
            sub.setText((it.usingGrams? it.quantity+" g" : it.quantity+" servings") + " · " + it.totalKcal()+" kcal");
            sub.setTextSize(12);
            sub.setTextColor(0xFF666666);

            col.addView(title);
            col.addView(sub);

            TextView remove = new TextView(this);
            remove.setText("Remove");
            remove.setPadding(dp(12), dp(6), dp(12), dp(6));
            remove.setBackgroundResource(android.R.drawable.btn_default);
            final int idx = i;
            remove.setOnClickListener(v -> {
                PendingMealItem removed = pendingItems.remove(idx);
                pendingTotalKcal -= removed.totalKcal();
                updateSummary();
            });

            row.addView(iv);
            row.addView(col);
            row.addView(remove);
            foodListContainer.addView(row);
        }
    }

    private void submitPendingItems() {
        if (pendingItems.isEmpty()) {
            Toast.makeText(this, "No items to submit", Toast.LENGTH_SHORT).show();
            return;
        }
        executor.execute(() -> {
            String currentDate = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            boolean allOk = true;
            for (PendingMealItem it : new ArrayList<>(pendingItems)) {
                try {
                    // Prefer canonical /meal endpoint when we have a foodItemId
                    ApiResponse<MealLogIdResponse> resp;
                    if (it.foodItemId != null) {
                        MealAddRequest req = new MealAddRequest(
                                getMealTypeCode(it.mealType),
                                1, // sourceType=1 (user custom food)
                                it.foodItemId,
                                null, // itemName (not needed for custom food)
                                null, // unitName (not needed for custom food)
                                it.usingGrams ? Math.max(1.0, it.quantity / 1.0) : (double) it.quantity, // unitQty
                                currentDate
                        );
                        retrofit2.Response<ApiResponse<MealLogIdResponse>> http = ApiClient.get().create(ApiService.class).addMealFromSource(req).execute();
                        if (!http.isSuccessful()) {
                            Log.e(TAG, "submitMeal HTTP " + http.code());
                            allOk = false;
                            break;
                        }
                        resp = http.body();
                    } else {
                        // Try to resolve id from backend search by name
                        ApiService api = ApiClient.get().create(ApiService.class);
                        try {
                            retrofit2.Response<ApiResponse<FoodSearchResponse>> s = api.searchFoods(it.foodName, null, 10, 0, "system").execute();
                            if (!s.isSuccessful()) {
                                if (s.code()==401) {
                                    mainHandler.post(() -> repository.handleTokenExpired());
                                }
                                allOk = false; break;
                            }
                            if (s.isSuccessful() && s.body()!=null && s.body().isSuccess() && s.body().getData()!=null) {
                                List<FoodSearchResponse.FoodItemDto> items = s.body().getData().getItems();
                                FoodSearchResponse.FoodItemDto match = null;
                                if (items != null) {
                                    for (FoodSearchResponse.FoodItemDto d : items) {
                                        if (it.foodName.equalsIgnoreCase(d.getName())) { match = d; break; }
                                    }
                                    // secondary: contains match
                                    if (match == null) {
                                        for (FoodSearchResponse.FoodItemDto d : items) {
                                            if (d.getName()!=null && d.getName().toLowerCase().contains(it.foodName.toLowerCase())) { match = d; break; }
                                        }
                                    }
                                }
                                if (match != null) {
                                    it.foodItemId = match.getId();
                                    it.unitName = match.getUnitName();
                                }
                            }
                        } catch (Exception ignore) {}

                        if (it.foodItemId == null) {
                            // create a custom item as fallback
                            try {
                                UserFoodItemRequest c = new UserFoodItemRequest();
                                c.setName(it.foodName);
                                c.setUnitName(it.unitName!=null? it.unitName : (it.usingGrams? "g":"serving"));
                                c.setKcalPerUnit(it.caloriesPerUnit);
                                c.setCarbsG(BigDecimal.ZERO);
                                c.setProteinG(BigDecimal.ZERO);
                                c.setFatG(BigDecimal.ZERO);
                                retrofit2.Response<ApiResponse<UserFoodItemResponse>> cr = api.createUserFoodItem(c).execute();
                                if (!cr.isSuccessful()) {
                                    if (cr.code()==401) {
                                        mainHandler.post(() -> repository.handleTokenExpired());
                                    }
                                    allOk = false; break;
                                }
                                if (cr.isSuccessful() && cr.body()!=null && cr.body().isSuccess() && cr.body().getData()!=null) {
                                    it.foodItemId = cr.body().getData().getId();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Create user food item failed", e);
                            }
                        }

                        if (it.foodItemId == null) {
                            Log.e(TAG, "No foodItemId resolved for " + it.foodName + ", please pick from search results");
                            allOk = false;
                            break;
                        }

                        // Now submit via /meals with resolved id
                        MealAddRequest req = new MealAddRequest(
                                getMealTypeCode(it.mealType),
                                1, // sourceType=1 (user custom food)
                                it.foodItemId,
                                null, // itemName (not needed for custom food)
                                null, // unitName (not needed for custom food)
                                it.usingGrams ? Math.max(1.0, it.quantity / 1.0) : (double) it.quantity, // unitQty
                                currentDate
                        );
                        retrofit2.Response<ApiResponse<MealLogIdResponse>> http = api.addMealFromSource(req).execute();
                        if (!http.isSuccessful()) {
                            Log.e(TAG, "submitMeal HTTP " + http.code());
                            if (http.code()==401) {
                                mainHandler.post(() -> repository.handleTokenExpired());
                            }
                            allOk = false;
                            break;
                        }
                        resp = http.body();
                        if (resp!=null && resp.getCode()==1101) {
                            mainHandler.post(() -> repository.handleTokenExpired());
                            allOk = false; break;
                        }
                    }
                    if (resp == null || !resp.isSuccess()) {
                        Log.e(TAG, "Meal submit failed resp=" + (resp==null?"null":("code="+resp.getCode()+", msg="+resp.getMessage())));
                        allOk = false;
                        break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "submitPendingItems failed", e);
                    allOk = false;
                    break;
                }
            }

            final boolean ok = allOk;
            mainHandler.post(() -> {
                if (ok) {
                    Toast.makeText(this, "Meals submitted", Toast.LENGTH_SHORT).show();
                    pendingItems.clear();
                    pendingTotalKcal = 0;
                    updateSummary();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(this, "Submit failed", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    
    private View createFoodItemView(FoodRecord record) {
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setPadding(0, 12, 0, 12);
        itemLayout.setGravity(LinearLayout.VERTICAL);
        
        // Food image
        ImageView foodImage = new ImageView(this);
        foodImage.setLayoutParams(new LinearLayout.LayoutParams(48, 48));
        foodImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        foodImage.setBackgroundResource(android.R.drawable.ic_menu_gallery);
        
        // Food name and calories
        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        infoLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        infoLayout.setPadding(12, 0, 0, 0);
        
        TextView nameText = new TextView(this);
        nameText.setText(record.foodName);
        nameText.setTextSize(16);
        nameText.setTextColor(getResources().getColor(android.R.color.black));
        
        TextView caloriesText = new TextView(this);
        String unit = record.isUsingGrams ? "g" : "servings";
        caloriesText.setText(record.quantity + " " + unit + " • " + record.calories + " kcal");
        caloriesText.setTextSize(14);
        caloriesText.setTextColor(getResources().getColor(android.R.color.darker_gray));
        
        infoLayout.addView(nameText);
        infoLayout.addView(caloriesText);
        
        // Remove button
        TextView removeButton = new TextView(this);
        removeButton.setText("×");
        removeButton.setTextSize(24);
        removeButton.setTextColor(getResources().getColor(android.R.color.darker_gray));
        removeButton.setPadding(16, 8, 16, 8);
        removeButton.setBackgroundResource(android.R.drawable.btn_default);
        removeButton.setOnClickListener(view -> removeFoodRecord(record));
        
        itemLayout.addView(foodImage);
        itemLayout.addView(infoLayout);
        itemLayout.addView(removeButton);
        
        return itemLayout;
    }
    
    private void removeFoodRecord(FoodRecord record) {
        // TODO: Use API to delete meal
        /*
        try {
            
            // Remove from database
            foodDao.deleteFoodRecord(record);
            
            // Update UI - refresh food list to update add/checkmark buttons
            updateSummary();
            refreshFoodList();
            Toast.makeText(this, "Removed: " + record.foodName, Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "Failed to remove food", Toast.LENGTH_SHORT).show();
        }
        */
    }
    
    private void showCustomFoodAddSheet() {
        CustomFoodAddSheet sheet = CustomFoodAddSheet.newInstance(currentMealType);
        
        sheet.setOnCustomFoodAddedListener(new CustomFoodAddSheet.OnCustomFoodAddedListener() {
            @Override
            public void onCustomFoodAdded(String mealType, String foodName, int weight, int calories) {
                // Use API to add custom food
                saveFoodRecord(mealType, foodName, calories, weight, true);
                // also record as recent (minimal fields)
                FoodItem f = new FoodItem(foodName, calories + " kcal / g", "Low GI");
                recordRecentFood(f);
            }
        });
        
        sheet.show(getSupportFragmentManager(), "custom_food_add");
    }

    // ---------------- Recent foods (Common tab) ----------------
    private void recordRecentFood(FoodItem item) {
        try {
            SharedPreferences sp = getSharedPreferences(PREF_FOOD, MODE_PRIVATE);
            String raw = sp.getString(KEY_RECENTS, "[]");
            JSONArray arr = new JSONArray(raw);
            // remove existing same-name to avoid duplicates
            JSONArray next = new JSONArray();
            for (int i=0;i<arr.length();i++) {
                JSONObject o = arr.getJSONObject(i);
                if (!item.name.equalsIgnoreCase(o.optString("name"))) {
                    next.put(o);
                }
            }
            // prepend new item
            JSONObject obj = new JSONObject();
            obj.put("name", item.name);
            obj.put("calories", item.calories);
            obj.put("gi", item.giLabel);
            obj.put("id", item.id==null? JSONObject.NULL : item.id);
            obj.put("unitName", item.unitName==null? JSONObject.NULL : item.unitName);
            obj.put("kcalPerUnit", item.kcalPerUnit);
            JSONArray merged = new JSONArray();
            merged.put(obj);
            for (int i=0;i<next.length() && i<RECENT_LIMIT-1;i++) merged.put(next.get(i));
            sp.edit().putString(KEY_RECENTS, merged.toString()).apply();
        } catch (Exception ignore) {}
    }

    private List<FoodItem> loadRecentFoods() {
        List<FoodItem> out = new ArrayList<>();
        try {
            SharedPreferences sp = getSharedPreferences(PREF_FOOD, MODE_PRIVATE);
            String raw = sp.getString(KEY_RECENTS, "[]");
            JSONArray arr = new JSONArray(raw);
            for (int i=0;i<arr.length() && i<RECENT_LIMIT;i++) {
                JSONObject o = arr.getJSONObject(i);
                String name = o.optString("name");
                String calories = o.optString("calories");
                String gi = o.optString("gi");
                Long id = o.isNull("id")? null : o.optLong("id");
                String unitName = o.isNull("unitName")? null : o.optString("unitName");
                int kcalPerUnit = o.optInt("kcalPerUnit", 0);
                out.add(new FoodItem(name, calories, gi, id, unitName, kcalPerUnit));
            }
        } catch (Exception ignore) {}
        return out;
    }
}
