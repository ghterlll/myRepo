package com.aura.starter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SearchFragment - Modern search entry point
 * Features:
 * - Material Design search input with clear and search buttons
 * - 5 popular search terms (horizontal layout) with click to search
 * - Recent search history (up to 5 items, vertical layout) with delete functionality
 * - Click any search term or enter query + search button to navigate to results
 */
public class SearchFragment extends Fragment {

    private static final String PREF_SEARCH_HISTORY = "search_history";
    private static final String KEY_HISTORY = "history";
    private static final int MAX_HISTORY_ITEMS = 5;

    private EditText etSearch;
    private RecyclerView recyclerHotTerms;
    private RecyclerView recyclerHistory;
    private TextView tvClearAll;

    // Hot search terms (fitness, diet, plan, recipe, outcome)
    private final List<String> hotSearchTerms = Arrays.asList("fitness", "diet", "plan", "recipe", "outcome");

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        initializeViews(view);
        setupRecyclerViews();
        setupSearchFunctionality();
        loadSearchHistory();

        // Setup clear all history button
        if (tvClearAll != null) {
            tvClearAll.setOnClickListener(v -> clearAllHistory());
        }

        // Setup clear button for search input
        setupClearButton(view);

        // Setup back button: global back (to previous page)
        ImageButton btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
        }

        // Restore current search text if provided
        Bundle args = getArguments();
        if (args != null && args.containsKey("current_search")) {
            String currentSearch = args.getString("current_search");
            if (etSearch != null && currentSearch != null) {
                etSearch.setText(currentSearch);
            }
        }

        return view;
    }

    private void initializeViews(View view) {
        etSearch = view.findViewById(R.id.etSearch);
        recyclerHotTerms = view.findViewById(R.id.recyclerHotTerms);
        recyclerHistory = view.findViewById(R.id.recyclerHistory);
        tvClearAll = view.findViewById(R.id.tvClearAll);

        // Setup search input functionality
        setupSearchInput();
    }

    private void setupSearchInput() {
        // Search button click
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            performSearch();
            return true;
        });

        // Prevent input too long (max 50 characters)
        etSearch.setFilters(new android.text.InputFilter[]{
            new android.text.InputFilter.LengthFilter(50)
        });
    }

    private void setupRecyclerViews() {
        // Hot terms - horizontal layout
        recyclerHotTerms.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        HotTermsAdapter hotTermsAdapter = new HotTermsAdapter();
        recyclerHotTerms.setAdapter(hotTermsAdapter);

        // Search history - vertical layout
        recyclerHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        HistoryAdapter historyAdapter = new HistoryAdapter();
        recyclerHistory.setAdapter(historyAdapter);
    }

    private void setupSearchFunctionality() {
        // Handle search button click (from parent activity)
        // This will be handled by SearchActivity
    }

    /**
     * Setup clear button for search input
     */
    private void setupClearButton(View view) {
        ImageButton btnClear = view.findViewById(R.id.btnClear);
        if (btnClear != null) {
            btnClear.setOnClickListener(v -> {
                etSearch.setText("");
                hideKeyboard();
            });
        }
    }

    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        if (!TextUtils.isEmpty(query)) {
            // Save to history first
            saveSearchToHistory(query);

            // Navigate to search results
            Intent intent = new Intent(requireContext(), SearchResultsActivity.class);
            intent.putExtra("search_query", query);
            startActivity(intent);
        }
    }

    private void saveSearchToHistory(String query) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_SEARCH_HISTORY, Context.MODE_PRIVATE);
        Set<String> history = prefs.getStringSet(KEY_HISTORY, new HashSet<>());

        // Add new search to history (remove if already exists to move to top)
        history.remove(query);
        history.add(query);

        // Keep only latest 5 items
        if (history.size() > MAX_HISTORY_ITEMS) {
            List<String> historyList = new ArrayList<>(history);
            history.clear();
            for (int i = historyList.size() - MAX_HISTORY_ITEMS; i < historyList.size(); i++) {
                history.add(historyList.get(i));
            }
        }

        prefs.edit().putStringSet(KEY_HISTORY, history).apply();
        loadSearchHistory(); // Refresh history display
    }

    private void loadSearchHistory() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_SEARCH_HISTORY, Context.MODE_PRIVATE);
        Set<String> history = prefs.getStringSet(KEY_HISTORY, new HashSet<>());

        if (history.isEmpty()) {
            recyclerHistory.setVisibility(View.GONE);
            tvClearAll.setVisibility(View.GONE);
        } else {
            recyclerHistory.setVisibility(View.VISIBLE);
            tvClearAll.setVisibility(View.VISIBLE);

            List<String> historyList = new ArrayList<>(history);
            // Reverse to show latest first
            for (int i = 0; i < historyList.size() / 2; i++) {
                String temp = historyList.get(i);
                historyList.set(i, historyList.get(historyList.size() - 1 - i));
                historyList.set(historyList.size() - 1 - i, temp);
            }
            ((HistoryAdapter) recyclerHistory.getAdapter()).submit(historyList);
        }
    }

    private void clearAllHistory() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_SEARCH_HISTORY, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_HISTORY).apply();
        loadSearchHistory(); // Refresh display
    }

    private void hideKeyboard() {
        // Hide soft keyboard if needed
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && etSearch != null) {
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }
    }

    // Adapter for hot search terms (horizontal)
    private class HotTermsAdapter extends RecyclerView.Adapter<HotTermsAdapter.ViewHolder> {
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView textView = new TextView(requireContext());
            // Modern green chip styling with proper margins
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 16, 0); // Right margin for spacing
            textView.setLayoutParams(params);
            textView.setPadding(32, 20, 32, 20); // Larger padding for modern look
            textView.setBackgroundResource(R.drawable.bg_chip_green);
            textView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            textView.setTextSize(14);
            textView.setClickable(true);
            textView.setFocusable(true);
            return new ViewHolder(textView);
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.textView.setText(hotSearchTerms.get(position));
            holder.textView.setOnClickListener(v -> {
                String term = hotSearchTerms.get(position);
                etSearch.setText(term);
                performSearch();
            });
        }

        @Override public int getItemCount() { return hotSearchTerms.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ViewHolder(TextView textView) { super(textView); this.textView = textView; }
        }
    }

    // Adapter for search history (vertical) with delete functionality
    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<String> history = new ArrayList<>();

        public void submit(List<String> history) {
            this.history = history;
            notifyDataSetChanged();
        }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Create horizontal layout for each history item
            android.widget.LinearLayout itemLayout = new android.widget.LinearLayout(requireContext());
            itemLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            itemLayout.setLayoutParams(new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            ));
            itemLayout.setPadding(0, 8, 0, 8);

            TextView textView = new TextView(requireContext());
            textView.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1
            ));
            textView.setPadding(16, 12, 8, 12);
            textView.setTextColor(getResources().getColor(android.R.color.black));

            ImageButton deleteBtn = new ImageButton(requireContext());
            deleteBtn.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                48, 48
            ));
            deleteBtn.setBackgroundResource(android.R.color.transparent);
            deleteBtn.setImageResource(android.R.drawable.ic_menu_delete);
            deleteBtn.setColorFilter(getResources().getColor(android.R.color.darker_gray));

            itemLayout.addView(textView);
            itemLayout.addView(deleteBtn);

            return new ViewHolder(itemLayout, textView, deleteBtn);
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String term = history.get(position);
            holder.textView.setText(term);

            // Click on text to search
            holder.textView.setOnClickListener(v -> {
                etSearch.setText(term);
                performSearch();
            });

            // Click delete button to remove from history
            holder.deleteBtn.setOnClickListener(v -> {
                removeFromHistory(term);
            });
        }

        @Override public int getItemCount() { return history.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ImageButton deleteBtn;
            ViewHolder(View itemView, TextView textView, ImageButton deleteBtn) {
                super(itemView);
                this.textView = textView;
                this.deleteBtn = deleteBtn;
            }
        }
    }

    private void removeFromHistory(String term) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_SEARCH_HISTORY, Context.MODE_PRIVATE);
        Set<String> history = prefs.getStringSet(KEY_HISTORY, new HashSet<>());
        history.remove(term);
        prefs.edit().putStringSet(KEY_HISTORY, history).apply();
        loadSearchHistory(); // Refresh display
    }
}
