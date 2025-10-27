package com.aura.starter;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.aura.starter.R;
import com.aura.starter.data.ProfileRepository;
import com.aura.starter.model.Post;
import com.aura.starter.model.UserProfile;
import com.aura.starter.network.AuthManager;
import com.aura.starter.network.UserRepository;
import com.aura.starter.network.models.UserStatisticsResponse;
import com.aura.starter.network.models.UserProfileResponse;
import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class ProfileFragment extends Fragment {
    private ProfileRepository profileRepo;
    private UserRepository userRepo = UserRepository.getInstance();
    private ImageView imgAvatar, imgCover;
    private View header;

    private final ActivityResultLauncher<String> pickAvatar = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) profileRepo.setAvatar(uri.toString());
            }
    );
    private final ActivityResultLauncher<String> pickCover = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) profileRepo.setCover(uri.toString());
            }
    );

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Check login status first
        AuthManager authManager = new AuthManager(requireContext());
        if (!authManager.isLoggedIn()) {
            showLoginRequiredDialog();
            // Return empty view to prevent loading
            return inflater.inflate(R.layout.fragment_profile, container, false);
        }

        View v = inflater.inflate(R.layout.fragment_profile, container, false);
        FeedViewModel vm = new ViewModelProvider(requireActivity()).get(FeedViewModel.class);
        profileRepo = ProfileRepository.get(requireContext());

        TextView tvName = v.findViewById(R.id.tvName);
        TextView tvBio = v.findViewById(R.id.tvBio);
        TextView tvDays = v.findViewById(R.id.tvStatDays);
        TextView tvMeals = v.findViewById(R.id.tvStatMeals);
        TextView tvHealthy = v.findViewById(R.id.tvStatHealthy);
        TextView tvPosts = v.findViewById(R.id.tvStatPosts);
        ImageView btnEdit = v.findViewById(R.id.btnEdit);
        ImageView btnIG = v.findViewById(R.id.btnIG);
        ImageView btnXHS = v.findViewById(R.id.btnXHS);
        imgAvatar = v.findViewById(R.id.imgAvatar);
        imgCover = v.findViewById(R.id.imgCover);
        header = v.findViewById(R.id.header);

        // Load statistics from backend API
        loadStatisticsFromBackend(tvDays, tvMeals, tvHealthy, tvPosts);

        // Load profile from backend API first, then observe local changes
        loadProfileFromBackend(tvName, tvBio);
        
        // Observe profile for local changes
        profileRepo.profile().observe(getViewLifecycleOwner(), p -> bindProfile(p, tvName, tvBio));

        // Edit dialog
        btnEdit.setOnClickListener(view -> showEditDialog());
        
        // Logout (long press on edit button)
        btnEdit.setOnLongClickListener(view -> {
            new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
            return true;
        });

        // Open links
        btnIG.setOnClickListener(vw -> {
            UserProfile p = profileRepo.profile().getValue();
            if (p==null) return;
            openUrl(p.linkInstagram);
        });
        btnXHS.setOnClickListener(vw -> {
            UserProfile p = profileRepo.profile().getValue();
            if (p==null) return;
            openUrl(p.linkXhs);
        });

        // Pickers
        imgAvatar.setOnClickListener(vw -> pickAvatar.launch("image/*"));
        header.setOnClickListener(vw -> pickCover.launch("image/*"));

        // Theme taps
        v.findViewById(R.id.theme0).setOnClickListener(vw -> profileRepo.setTheme(0));
        v.findViewById(R.id.theme1).setOnClickListener(vw -> profileRepo.setTheme(1));
        v.findViewById(R.id.theme2).setOnClickListener(vw -> profileRepo.setTheme(2));
        v.findViewById(R.id.theme3).setOnClickListener(vw -> profileRepo.setTheme(3));

        // Tabs + pager
        ViewPager2 pager = v.findViewById(R.id.viewPager);
        TabLayout tabs = v.findViewById(R.id.tabLayout);
        pager.setAdapter(new ProfilePagerAdapter(this));
        pager.setOffscreenPageLimit(2); // Preload both tabs immediately
        new TabLayoutMediator(tabs, pager, (tab, pos) -> {
            tab.setText(pos==0 ? getString(R.string.tab_posts) : getString(R.string.tab_bookmarks));
        }).attach();

        return v;
    }

    private void bindProfile(UserProfile p, TextView tvName, TextView tvBio){
        if (p==null) return;
        tvName.setText(p.name);
        tvBio.setText(p.bio);
        // Avatar
        if (p.avatarUri != null && !p.avatarUri.isEmpty()) {
            Glide.with(this).load(Uri.parse(p.avatarUri)).placeholder(R.drawable.placeholder).into(imgAvatar);
        } else {
            imgAvatar.setImageResource(R.drawable.placeholder);
        }
        // Cover
        if (p.coverUri != null && !p.coverUri.isEmpty()) {
            Glide.with(this).load(Uri.parse(p.coverUri)).into(imgCover);
        } else {
            imgCover.setImageDrawable(null);
        }
        // Theme
        int bg = R.drawable.profile_header_bg;
        if (p.themeIndex==1) bg = R.drawable.profile_header_bg_blue;
        else if (p.themeIndex==2) bg = R.drawable.profile_header_bg_purple;
        else if (p.themeIndex==3) bg = R.drawable.profile_header_bg_teal;
        header.setBackgroundResource(bg);
    }

    private void openUrl(String url){
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(i);
        } catch (Exception e){
            Toast.makeText(requireContext(), "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditDialog(){
        View dv = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null, false);
        EditText etName = dv.findViewById(R.id.etName);
        EditText etBio = dv.findViewById(R.id.etBio);
        EditText etIG = dv.findViewById(R.id.etIG);
        EditText etXHS = dv.findViewById(R.id.etXHS);
        UserProfile p = profileRepo.profile().getValue();
        if (p!=null){
            etName.setText(p.name);
            etBio.setText(p.bio);
            etIG.setText(p.linkInstagram);
            etXHS.setText(p.linkXhs);
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Profile")
                .setView(dv)
                .setPositiveButton("Save", (d,w) -> {
                    profileRepo.setName(etName.getText().toString().trim());
                    profileRepo.setBio(etBio.getText().toString().trim());
                    profileRepo.setLinks(etIG.getText().toString().trim(), etXHS.getText().toString().trim());
                    Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    /**
     * Load user profile from backend API
     */
    private void loadProfileFromBackend(TextView tvName, TextView tvBio) {
        userRepo.getMyProfile(new UserRepository.ResultCallback<UserProfileResponse>() {
            @Override
            public void onSuccess(UserProfileResponse profile) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Update local repository with backend data
                        if (profile.getNickname() != null) {
                            profileRepo.setName(profile.getNickname());
                        }
                        if (profile.getBio() != null) {
                            profileRepo.setBio(profile.getBio());
                        }
                        if (profile.getAvatarUrl() != null) {
                            profileRepo.setAvatar(profile.getAvatarUrl());
                        }
                        
                        // UI will be updated through observer
                    });
                }
            }

            @Override
            public void onError(String message) {
                // Silently fail and use local data
                android.util.Log.e("ProfileFragment", "Failed to load profile: " + message);
            }
        });
    }

    /**
     * Load user statistics from backend API
     */
    private void loadStatisticsFromBackend(TextView tvDays, TextView tvMeals, TextView tvHealthy, TextView tvPosts) {
        userRepo.getMyStatistics(new UserRepository.ResultCallback<UserStatisticsResponse>() {
            @Override
            public void onSuccess(UserStatisticsResponse stats) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Update all four statistics
                        if (stats.getJoinedDays() != null) {
                            tvDays.setText(String.valueOf(stats.getJoinedDays()));
                        }
                        if (stats.getMealCount() != null) {
                            tvMeals.setText(String.valueOf(stats.getMealCount()));
                        }
                        if (stats.getHealthyDays() != null) {
                            tvHealthy.setText(String.valueOf(stats.getHealthyDays()));
                        }
                        if (stats.getPostCount() != null) {
                            tvPosts.setText(String.valueOf(stats.getPostCount()));
                        }
                    });
                }
            }

            @Override
            public void onError(String message) {
                // Silently fail and keep default values
                android.util.Log.e("ProfileFragment", "Failed to load statistics: " + message);
            }
        });
    }

    private void logout() {
        AuthManager authManager = new AuthManager(requireContext());
        authManager.logout();

        // Redirect to login
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();

        Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show();
    }

    /**
     * Show modern login required dialog
     */
    private void showLoginRequiredDialog() {
        if (getContext() == null) return;

        new AlertDialog.Builder(requireContext())
            .setTitle("Login Required")
            .setMessage("Please login to access your profile and personalized content.")
            .setPositiveButton("Go to Login", (dialog, which) -> {
                // Navigate to login activity
                Intent intent = new Intent(requireActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            })
            .setNegativeButton("Cancel", (dialog, which) -> {
                // User chose not to login, do nothing
                dialog.dismiss();
            })
            .setCancelable(false)
            .show();
    }
}
