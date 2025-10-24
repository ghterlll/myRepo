package com.aura.starter;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.aura.starter.network.AuthManager;
// ✅ 关键：导入 RunHubActivity
import com.aura.starter.hub.RunHubActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AuthManager authManager = new AuthManager(this);
        if (authManager.getAccessToken() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();


            if (id == R.id.nav_run) {
                Intent i = new Intent(MainActivity.this, RunHubActivity.class);
                startActivity(i);
                // overridePendingTransition(0, 0);

                return false;
            }

            Fragment f = null;
            if (id == R.id.nav_record) {
                f = new RecordFragment();
            } else if (id == R.id.nav_posts) {
                f = new FeedFragment();
            } else if (id == R.id.nav_profile) {
                f = new ProfileFragment();
            } else {
                f = new RecordFragment(); // fallback
            }

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, f)
                    .commit();
            return true; // 其它 Tab 正常切换 Fragment
        });


        nav.setSelectedItemId(R.id.nav_record);


        nav.setOnItemReselectedListener(item -> {
        });
    }
}