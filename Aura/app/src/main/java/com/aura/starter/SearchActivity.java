package com.aura.starter;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity to host SearchFragment
 * Provides a dedicated screen for search functionality
 */
public class SearchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Check if there's a current search text to restore
        String currentSearch = null;
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("current_search")) {
            currentSearch = intent.getStringExtra("current_search");
        }

        // Add SearchFragment to this activity with current search text
        if (savedInstanceState == null) {
            SearchFragment fragment = new SearchFragment();
            if (currentSearch != null) {
                Bundle args = new Bundle();
                args.putString("current_search", currentSearch);
                fragment.setArguments(args);
            }
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        }
    }
}
