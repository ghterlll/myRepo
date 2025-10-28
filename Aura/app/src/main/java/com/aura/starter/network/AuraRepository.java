package com.aura.starter.network;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.aura.starter.LoginActivity;
import com.aura.starter.network.models.*;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Data Repository - Encapsulates all backend API calls
 */
public class AuraRepository {
    private static final String TAG = "AuraRepository";
    private static final int CODE_TOKEN_EXPIRED = 1101;
    
    private final ApiService apiService;
    private final AuthManager authManager;
    private final Context context;

    public AuraRepository(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = ApiClient.get().create(ApiService.class);
        this.authManager = new AuthManager(context);
    }

    // ==================== Authentication ====================

    /**
     * User registration with OTP
     */
    public ApiResponse<Void> registerWithOtp(String email, String password, String nickname) throws IOException {
        RegisterWithOtpRequest request = new RegisterWithOtpRequest(
                email, 
                password, 
                nickname,
                null, // phone
                null, // regionCode
                null  // city
        );
        Response<ApiResponse<Void>> response = apiService.registerWithOtp(request).execute();
        
        // Log detailed response info
        android.util.Log.d("AuraRepository", "Register with OTP HTTP code: " + response.code());
        android.util.Log.d("AuraRepository", "Register with OTP response success: " + response.isSuccessful());
        if (!response.isSuccessful() && response.errorBody() != null) {
            String errorBody = response.errorBody().string();
            android.util.Log.e("AuraRepository", "Register with OTP error body: " + errorBody);
        }
        
        return response.body();
    }

    /**
     * Send registration verification code
     */
    public ApiResponse<Void> sendRegistrationCode(String email) throws IOException {
        SendRegistrationCodeRequest request = new SendRegistrationCodeRequest(email);
        Response<ApiResponse<Void>> response = apiService.sendRegistrationCode(request).execute();
        
        android.util.Log.d("AuraRepository", "Send registration code HTTP code: " + response.code());
        android.util.Log.d("AuraRepository", "Send registration code response success: " + response.isSuccessful());
        if (!response.isSuccessful() && response.errorBody() != null) {
            String errorBody = response.errorBody().string();
            android.util.Log.e("AuraRepository", "Send registration code error body: " + errorBody);
        }
        
        return response.body();
    }

    /**
     * Verify registration code and auto-login
     */
    public ApiResponse<TokenResponse> verifyRegistration(String email, String code) throws IOException {
        VerifyRegistrationRequest request = new VerifyRegistrationRequest(
                email, 
                code, 
                authManager.getDeviceId()
        );
        Response<ApiResponse<TokenResponse>> response = apiService.verifyRegistration(request).execute();
        
        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
            TokenResponse tokenData = response.body().getData();
            // Save tokens
            authManager.saveLoginInfo(
                    tokenData.getAccessToken(),
                    tokenData.getRefreshToken(),
                    null  // userId is parsed from token or fetched via /users/me later
            );
        }
        
        return response.body();
    }

    /**
     * User login
     */
    public ApiResponse<TokenResponse> login(String email, String password) throws IOException {
        LoginRequest request = new LoginRequest(
                email,
                password,
                authManager.getDeviceId()
        );
        Response<ApiResponse<TokenResponse>> response = apiService.login(request).execute();

        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
            TokenResponse tokenData = response.body().getData();

            // Parse userId from JWT token
            Long userId = com.aura.starter.util.JwtParser.extractUserId(tokenData.getAccessToken());

            // Save tokens with parsed userId
            authManager.saveLoginInfo(
                    tokenData.getAccessToken(),
                    tokenData.getRefreshToken(),
                    userId  // userId extracted from JWT
            );
        }

        return response.body();
    }

    /**
     * Refresh token
     */
    public ApiResponse<TokenResponse> refreshToken() throws IOException {
        RefreshTokenRequest request = new RefreshTokenRequest(
                authManager.getRefreshToken(),
                authManager.getDeviceId()
        );
        Response<ApiResponse<TokenResponse>> response = apiService.refreshToken(request).execute();

        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
            TokenResponse tokenData = response.body().getData();

            // Parse userId from new JWT token (in case it changed)
            Long userId = com.aura.starter.util.JwtParser.extractUserId(tokenData.getAccessToken());

            authManager.saveLoginInfo(
                    tokenData.getAccessToken(),
                    tokenData.getRefreshToken(),
                    userId != null ? userId : authManager.getUserId()  // Use parsed userId or keep existing
            );
        }

        return response.body();
    }

    // ==================== User Information ====================

    /**
     * Get current user profile
     */
    public ApiResponse<UserProfileResponse> getMyProfile() throws IOException {
        Response<ApiResponse<UserProfileResponse>> response = apiService.getMyProfile().execute();
        return response.body();
    }

    /**
     * Update user profile
     */
    public ApiResponse<Void> updateMyProfile(UserProfileUpdateRequest request) throws IOException {
        Response<ApiResponse<Void>> response = apiService.updateMyProfile(request).execute();
        return response.body();
    }

    // ==================== Meal Logs ====================

    /**
     * Add meal log
     */
    public ApiResponse<MealLogIdResponse> addMeal(MealAddRequest request) throws IOException {
        Response<ApiResponse<MealLogIdResponse>> response;
        // Route to proper backend endpoint
        if (request.getSourceType() != null && request.getSourceType() == 1 && request.getSourceId() != null) {
            // from-source (user/custom/system item with id)
            response = apiService.addMealFromSource(request).execute();
        } else {
            // free-input
            response = apiService.addMealFreeInput(request).execute();
        }
        return response.body();
    }

    /**
     * Get daily meal summary
     */
    public ApiResponse<DailySummaryResponse> getDailySummary(String date) throws IOException {
        Response<ApiResponse<DailySummaryResponse>> response = apiService.getDailySummary(date).execute();
        return response.body();
    }

    /**
     * Delete meal log
     */
    public ApiResponse<Void> deleteMeal(Long id) throws IOException {
        Response<ApiResponse<Void>> response = apiService.deleteMeal(id).execute();
        return response.body();
    }

    // ==================== Food Library ====================

    /**
     * Search foods
     */
    public ApiResponse<FoodSearchResponse> searchFoods(String query, String category, Integer limit, Integer offset) throws IOException {
        Response<ApiResponse<FoodSearchResponse>> response = apiService.searchFoods(query, category, limit, offset, "system").execute();
        return response.body();
    }

    /**
     * Create user custom food item
     */
    public ApiResponse<UserFoodItemResponse> createUserFoodItem(UserFoodItemRequest request) throws IOException {
        Response<ApiResponse<UserFoodItemResponse>> response = apiService.createUserFoodItem(request).execute();
        return response.body();
    }

    /**
     * Get user custom food items
     */
    public ApiResponse<UserFoodItemListResponse> getUserFoodItems() throws IOException {
        Response<ApiResponse<UserFoodItemListResponse>> response = apiService.getUserFoodItems().execute();
        return response.body();
    }

    // ==================== Water Intake Records ====================

    /**
     * Add water intake record
     */
    public ApiResponse<Void> addWater(String intakeDate, int amountMl) throws IOException {
        WaterAddRequest request = new WaterAddRequest(intakeDate, amountMl);
        Response<ApiResponse<Void>> response = apiService.addWater(request).execute();
        return response.body();
    }

    /**
     * Get daily water summary
     */
    public ApiResponse<WaterDailySummaryResponse> getWaterDailySummary(String date) throws IOException {
        Response<ApiResponse<WaterDailySummaryResponse>> response = apiService.getWaterDailySummary(date).execute();
        return response.body();
    }

    // ==================== Weight Records ====================

    /**
     * Log weight
     * @param date Date yyyy-MM-dd (null for today)
     * @param weightKg Weight in kg
     */
    public ApiResponse<Void> logWeight(String date, java.math.BigDecimal weightKg) throws IOException {
        WeightLogRequest request = new WeightLogRequest(date, weightKg);
        Response<ApiResponse<Void>> response = apiService.logWeight(request).execute();
        return response.body();
    }
    
    /**
     * Log weight with note
     */
    public ApiResponse<Void> logWeight(String date, java.math.BigDecimal weightKg, String note) throws IOException {
        WeightLogRequest request = new WeightLogRequest(date, weightKg, note);
        Response<ApiResponse<Void>> response = apiService.logWeight(request).execute();
        return response.body();
    }

    /**
     * Get weight history (range query)
     * @param start Start date yyyy-MM-dd (nullable)
     * @param end End date yyyy-MM-dd (nullable)
     */
    public ApiResponse<WeightHistoryResponse> getWeightHistory(String start, String end) throws IOException {
        Response<ApiResponse<WeightHistoryResponse>> response = apiService.getWeightHistory(start, end).execute();
        return response.body();
    }

    /**
     * Get latest weight
     */
    public ApiResponse<WeightLogResponse> getLatestWeight() throws IOException {
        Response<ApiResponse<WeightLogResponse>> response = apiService.getLatestWeight().execute();
        return response.body();
    }

    // ----- Weight profile edits -----
    public ApiResponse<Void> updateInitialWeight(String date, java.math.BigDecimal weightKg) throws IOException {
        UpdateInitialWeightRequest req = new UpdateInitialWeightRequest(date, weightKg);
        Response<ApiResponse<Void>> resp = apiService.updateInitialWeight(req).execute();
        android.util.Log.d("AuraRepository", "updateInitialWeight HTTP code: " + resp.code());
        if (!resp.isSuccessful() && resp.errorBody()!=null) {
            android.util.Log.e("AuraRepository", "updateInitialWeight error: " + resp.errorBody().string());
        }
        return resp.body();
    }

    public ApiResponse<Void> updateTargetWeight(java.math.BigDecimal targetKg) throws IOException {
        UserProfileUpdateRequest req = new UserProfileUpdateRequest();
        req.setTargetWeightKg(targetKg);
        Response<ApiResponse<Void>> resp = apiService.updateMyProfile(req).execute();
        android.util.Log.d("AuraRepository", "updateTargetWeight HTTP code: " + resp.code());
        if (!resp.isSuccessful() && resp.errorBody()!=null) {
            android.util.Log.e("AuraRepository", "updateTargetWeight error: " + resp.errorBody().string());
        }
        return resp.body();
    }

    // ==================== Exercise Records ====================

    /**
     * Add exercise record
     */
    public ApiResponse<Void> addExercise(ExerciseAddRequest request) throws IOException {
        Response<ApiResponse<Void>> response = apiService.addExercise(request).execute();
        return response.body();
    }

    /**
     * Get daily workout summary
     */
    public ApiResponse<DailyWorkoutResponse> getDailyWorkout(String date) throws IOException {
        Response<ApiResponse<DailyWorkoutResponse>> response = apiService.getDailyWorkout(date).execute();
        return response.body();
    }

    // ==================== Daily Overview ====================

    /**
     * Get daily data overview
     */
    public ApiResponse<DayOverviewResponse> getDayOverview(String date) throws IOException {
        Response<ApiResponse<DayOverviewResponse>> response = apiService.getDayOverview(date).execute();
        return response.body();
    }

    // ==================== Helper Methods ====================

    /**
     * Get AuthManager instance
     */
    public AuthManager getAuthManager() {
        return authManager;
    }

    /**
     * Generic error logging
     */
    private void logError(String method, Throwable e) {
        Log.e(TAG, method + " failed", e);
    }

    /**
     * Check if response indicates token expiration
     */
    public boolean isTokenExpired(ApiResponse<?> response) {
        return response != null && response.getCode() == CODE_TOKEN_EXPIRED;
    }

    /**
     * Handle token expiration: logout and redirect to login
     * Should be called from UI thread
     */
    public void handleTokenExpired() {
        Log.w(TAG, "Token expired, logging out and redirecting to login");
        authManager.logout();
        
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    /**
     * Convenience method to check and handle token expiration in one call
     * @return true if token was expired and handled, false otherwise
     */
    public boolean checkAndHandleTokenExpired(ApiResponse<?> response) {
        if (isTokenExpired(response)) {
            handleTokenExpired();
            return true;
        }
        return false;
    }

    // ==================== file upload methods ====================

    /**
     * Upload food image to OSS
     * @param imageFile The image file to upload
     * @return FileUploadResponse with URL and key
     */
    public ApiResponse<FileUploadResponse> uploadFoodImage(java.io.File imageFile) throws IOException {
        // Debug: Check token status
        String currentToken = authManager.getAccessToken();
        Log.d(TAG, "uploadFoodImage - Current token: " + (currentToken != null ? "exists" : "null"));
        Log.d(TAG, "uploadFoodImage - Token length: " + (currentToken != null ? currentToken.length() : 0));
        
        // Ensure token is set in ApiClient
        authManager.initTokenToApiClient();
        
        // Determine correct MIME type based on file extension
        String mimeType = getImageMimeType(imageFile.getName());
        okhttp3.MediaType mediaType = okhttp3.MediaType.parse(mimeType);
        okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(mediaType, imageFile);
        okhttp3.MultipartBody.Part filePart = okhttp3.MultipartBody.Part.createFormData("file", imageFile.getName(), requestBody);
        
        Response<ApiResponse<FileUploadResponse>> response = apiService.uploadFoodImage(filePart).execute();
        Log.d(TAG, "uploadFoodImage HTTP code: " + response.code());
        if (!response.isSuccessful() && response.errorBody() != null) {
            String errorBody = response.errorBody().string();
            Log.e(TAG, "uploadFoodImage error body: " + errorBody);
        }
        
        ApiResponse<FileUploadResponse> result = response.body();
        
        // Check for token expiration
        if (result != null && isTokenExpired(result)) {
            handleTokenExpired();
        }
        
        return result;
    }

    /**
     * Upload avatar image to OSS
     * @param imageFile The image file to upload
     * @return FileUploadResponse with URL and key
     */
    public ApiResponse<FileUploadResponse> uploadAvatar(java.io.File imageFile) throws IOException {
        okhttp3.MediaType mediaType = okhttp3.MediaType.parse("image/*");
        okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(mediaType, imageFile);
        okhttp3.MultipartBody.Part filePart = okhttp3.MultipartBody.Part.createFormData("file", imageFile.getName(), requestBody);
        
        Response<ApiResponse<FileUploadResponse>> response = apiService.uploadAvatar(filePart).execute();
        Log.d(TAG, "uploadAvatar HTTP code: " + response.code());
        if (!response.isSuccessful() && response.errorBody() != null) {
            String errorBody = response.errorBody().string();
            Log.e(TAG, "uploadAvatar error body: " + errorBody);
        }
        return response.body();
    }

    /**
     * Upload post image to OSS
     * @param imageFile The image file to upload
     * @return FileUploadResponse with URL and key
     */
    public ApiResponse<FileUploadResponse> uploadPostImage(java.io.File imageFile) throws IOException {
        okhttp3.MediaType mediaType = okhttp3.MediaType.parse("image/*");
        okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(mediaType, imageFile);
        okhttp3.MultipartBody.Part filePart = okhttp3.MultipartBody.Part.createFormData("file", imageFile.getName(), requestBody);
        
        Response<ApiResponse<FileUploadResponse>> response = apiService.uploadPostImage(filePart).execute();
        Log.d(TAG, "uploadPostImage HTTP code: " + response.code());
        if (!response.isSuccessful() && response.errorBody() != null) {
            String errorBody = response.errorBody().string();
            Log.e(TAG, "uploadPostImage error body: " + errorBody);
        }
        return response.body();
    }

    /**
     * Delete a file from OSS
     * @param key The file key to delete
     */
    public ApiResponse<Void> deleteFile(String key) throws IOException {
        Response<ApiResponse<Void>> response = apiService.deleteFile(key).execute();
        Log.d(TAG, "deleteFile HTTP code: " + response.code());
        if (!response.isSuccessful() && response.errorBody() != null) {
            String errorBody = response.errorBody().string();
            Log.e(TAG, "deleteFile error body: " + errorBody);
        }
        return response.body();
    }

    /**
     * Get presigned URL for private file access
     * @param key The file key
     * @param expirationMinutes Expiration time in minutes
     * @return PresignedUrlResponse with temporary URL
     */
    public ApiResponse<PresignedUrlResponse> getPresignedUrl(String key, int expirationMinutes) throws IOException {
        Response<ApiResponse<PresignedUrlResponse>> response = apiService.getPresignedUrl(key, expirationMinutes).execute();
        Log.d(TAG, "getPresignedUrl HTTP code: " + response.code());
        if (!response.isSuccessful() && response.errorBody() != null) {
            String errorBody = response.errorBody().string();
            Log.e(TAG, "getPresignedUrl error body: " + errorBody);
        }
        return response.body();
    }

    /**
     * Check if a file exists in OSS
     * @param key The file key to check
     * @return FileExistsResponse with existence status
     */
    public ApiResponse<FileExistsResponse> fileExists(String key) throws IOException {
        Response<ApiResponse<FileExistsResponse>> response = apiService.fileExists(key).execute();
        Log.d(TAG, "fileExists HTTP code: " + response.code());
        if (!response.isSuccessful() && response.errorBody() != null) {
            String errorBody = response.errorBody().string();
            Log.e(TAG, "fileExists error body: " + errorBody);
        }
        return response.body();
    }
    
    /**
     * Get correct MIME type for image files based on file extension
     */
    private String getImageMimeType(String filename) {
        if (filename == null) return "image/jpeg";
        
        String extension = filename.toLowerCase();
        if (extension.endsWith(".jpg") || extension.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (extension.endsWith(".png")) {
            return "image/png";
        } else if (extension.endsWith(".gif")) {
            return "image/gif";
        } else if (extension.endsWith(".webp")) {
            return "image/webp";
        } else {
            // Default to JPEG for unknown extensions
            return "image/jpeg";
        }
    }
}

