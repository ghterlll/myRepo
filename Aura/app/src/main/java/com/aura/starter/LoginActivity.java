package com.aura.starter;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.aura.starter.network.AuraRepository;
import com.aura.starter.network.models.ApiResponse;
import com.aura.starter.network.models.TokenResponse;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    
    private EditText etEmail;
    private EditText etPassword;
    private EditText etNickname;
    private EditText etVerificationCode;
    private Button btnLogin;
    private Button btnRegister;
    private Button btnSendCode;
    private Button btnVerifyCode;
    private TextView tvSwitchMode;
    private ProgressBar progressBar;
    private View registerFields;
    private View verificationFields;
    
    private boolean isRegisterMode = false;
    private boolean isVerificationMode = false;
    private String pendingEmail = "";
    private AuraRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        repository = new AuraRepository(this);

        // Check if already logged in
        if (repository.getAuthManager().getAccessToken() != null) {
            navigateToMain();
            return;
        }

        initViews();
        setupListeners();
    }
    
    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etNickname = findViewById(R.id.etNickname);
        etVerificationCode = findViewById(R.id.etVerificationCode);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnSendCode = findViewById(R.id.btnSendCode);
        btnVerifyCode = findViewById(R.id.btnVerifyCode);
        tvSwitchMode = findViewById(R.id.tvSwitchMode);
        progressBar = findViewById(R.id.progressBar);
        registerFields = findViewById(R.id.registerFields);
        verificationFields = findViewById(R.id.verificationFields);
    }
    
    private void setupListeners() {
        btnLogin.setOnClickListener(v -> login());
        btnRegister.setOnClickListener(v -> register());
        btnSendCode.setOnClickListener(v -> sendVerificationCode());
        btnVerifyCode.setOnClickListener(v -> verifyCode());
        tvSwitchMode.setOnClickListener(v -> switchMode());
    }
    
    private void switchMode() {
        isRegisterMode = !isRegisterMode;
        
        if (isRegisterMode) {
            // Switch to register mode
            registerFields.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.GONE);
            btnRegister.setVisibility(View.VISIBLE);
            tvSwitchMode.setText("Already have an account? Login");
        } else {
            // Switch to login mode
            registerFields.setVisibility(View.GONE);
            btnLogin.setVisibility(View.VISIBLE);
            btnRegister.setVisibility(View.GONE);
            tvSwitchMode.setText("Don't have an account? Register");
        }
    }
    
    private void login() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        if (!validateInput(email, password, null)) {
            return;
        }
        
        showLoading(true);
        
        executor.execute(() -> {
            try {
                Log.d(TAG, "Attempting login for: " + email);
                ApiResponse<TokenResponse> response = repository.login(email, password);
                
                mainHandler.post(() -> {
                    showLoading(false);
                    
                    if (response != null && response.isSuccess()) {
                        Log.d(TAG, "Login successful!");
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                        // Ensure ApiClient carries fresh token for subsequent requests
                        repository.getAuthManager().initTokenToApiClient();
                        navigateToMain();
                    } else {
                        String errorMsg = response != null ? response.getMessage() : "Connection failed";
                        Log.e(TAG, "Login failed: " + errorMsg);
                        Toast.makeText(this, "Login failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Login error", e);
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void register() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String nickname = etNickname.getText().toString().trim();
        
        if (!validateInput(email, password, nickname)) {
            return;
        }
        
        showLoading(true);
        
        executor.execute(() -> {
            try {
                Log.d(TAG, "Attempting registration for: " + email + ", nickname: " + nickname);
                Log.d(TAG, "API Base URL: " + com.aura.starter.network.ApiClient.get().baseUrl());
                ApiResponse<Void> registerResponse = repository.registerWithOtp(email, password, nickname);
                Log.d(TAG, "Register with OTP response: " + registerResponse);
                
                if (registerResponse != null && registerResponse.isSuccess()) {
                    // Registration successful, show verification screen
                    Log.d(TAG, "Registration successful, showing verification screen");
                    pendingEmail = email;
                    mainHandler.post(() -> {
                        showLoading(false);
                        showVerificationScreen();
                        Toast.makeText(this, "Please check your email for verification code", Toast.LENGTH_LONG).show();
                    });
                } else {
                    String errorMsg = registerResponse != null ? registerResponse.getMessage() : "Connection failed";
                    Log.e(TAG, "Registration failed: " + errorMsg);
                    
                    mainHandler.post(() -> {
                        showLoading(false);
                        Toast.makeText(this, "Registration failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Registration error", e);
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void sendVerificationCode() {
        if (TextUtils.isEmpty(pendingEmail)) {
            Toast.makeText(this, "No pending email", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        executor.execute(() -> {
            try {
                Log.d(TAG, "Sending verification code to: " + pendingEmail);
                ApiResponse<Void> response = repository.sendRegistrationCode(pendingEmail);
                
                mainHandler.post(() -> {
                    showLoading(false);
                    
                    if (response != null && response.isSuccess()) {
                        Toast.makeText(this, "Verification code sent!", Toast.LENGTH_SHORT).show();
                    } else {
                        String errorMsg = response != null ? response.getMessage() : "Failed to send code";
                        Toast.makeText(this, "Failed to send code: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Send verification code error", e);
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void verifyCode() {
        String code = etVerificationCode.getText().toString().trim();
        
        if (TextUtils.isEmpty(code)) {
            etVerificationCode.setError("Verification code is required");
            etVerificationCode.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(pendingEmail)) {
            Toast.makeText(this, "No pending email", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        executor.execute(() -> {
            try {
                Log.d(TAG, "Verifying code for: " + pendingEmail);
                ApiResponse<TokenResponse> response = repository.verifyRegistration(pendingEmail, code);
                
                mainHandler.post(() -> {
                    showLoading(false);
                    
                    if (response != null && response.isSuccess()) {
                        Log.d(TAG, "Verification successful!");
                        Toast.makeText(this, "Account verified successfully!", Toast.LENGTH_SHORT).show();
                        repository.getAuthManager().initTokenToApiClient();
                        navigateToMain();
                    } else {
                        String errorMsg = response != null ? response.getMessage() : "Verification failed";
                        Toast.makeText(this, "Verification failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Verification error", e);
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showVerificationScreen() {
        isVerificationMode = true;
        registerFields.setVisibility(View.GONE);
        verificationFields.setVisibility(View.VISIBLE);
        btnLogin.setVisibility(View.GONE);
        btnRegister.setVisibility(View.GONE);
        tvSwitchMode.setVisibility(View.GONE);
    }

    private void hideVerificationScreen() {
        isVerificationMode = false;
        registerFields.setVisibility(View.VISIBLE);
        verificationFields.setVisibility(View.GONE);
        btnLogin.setVisibility(View.VISIBLE);
        btnRegister.setVisibility(View.VISIBLE);
        tvSwitchMode.setVisibility(View.VISIBLE);
    }
    
    private boolean validateInput(String email, String password, String nickname) {
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return false;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Invalid email address");
            etEmail.requestFocus();
            return false;
        }
        
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return false;
        }
        
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return false;
        }
        
        if (isRegisterMode && TextUtils.isEmpty(nickname)) {
            etNickname.setError("Nickname is required");
            etNickname.requestFocus();
            return false;
        }
        
        return true;
    }
    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        btnRegister.setEnabled(!show);
        etEmail.setEnabled(!show);
        etPassword.setEnabled(!show);
        etNickname.setEnabled(!show);
    }
    
    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}

