package com.aura.starter.network;

import android.util.Log;

import com.aura.starter.network.models.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private final ApiService apiService;
    private static UserRepository instance;

    private UserRepository() {
        apiService = ApiClient.get().create(ApiService.class);
    }

    public static synchronized UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    // ==================== User Profile ====================

    public void getMyProfile(ResultCallback<UserProfileResponse> callback) {
        apiService.getMyProfile().enqueue(new Callback<ApiResponse<UserProfileResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserProfileResponse>> call, Response<ApiResponse<UserProfileResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserProfileResponse>> call, Throwable t) {
                Log.e(TAG, "Get my profile failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void updateMyProfile(UserProfileUpdateRequest request, ResultCallback<Void> callback) {
        apiService.updateMyProfile(request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e(TAG, "Update my profile failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // ==================== User Statistics ====================

    public void getMyStatistics(ResultCallback<UserStatisticsResponse> callback) {
        apiService.getMyStatistics().enqueue(new Callback<ApiResponse<UserStatisticsResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserStatisticsResponse>> call, Response<ApiResponse<UserStatisticsResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserStatisticsResponse>> call, Throwable t) {
                Log.e(TAG, "Get my statistics failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // ==================== Helper ====================

    private <T> String getErrorMessage(Response<ApiResponse<T>> response) {
        if (response.body() != null && response.body().getMessage() != null) {
            return response.body().getMessage();
        }
        return "Error: " + response.code();
    }

    // Callback interface
    public interface ResultCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }
}
