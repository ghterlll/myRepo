package com.aura.starter.network;

import android.util.Log;

import com.aura.starter.network.models.*;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FileRepository {
    private static final String TAG = "FileRepository";
    private final ApiService apiService;
    private static FileRepository instance;

    private FileRepository() {
        apiService = ApiClient.get().create(ApiService.class);
    }

    public static synchronized FileRepository getInstance() {
        if (instance == null) {
            instance = new FileRepository();
        }
        return instance;
    }

    // ==================== File Upload ====================

    public void uploadPostImage(File imageFile, ResultCallback<FileUploadResponse> callback) {
        String mimeType = getMimeTypeFromFile(imageFile);
        RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), imageFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", imageFile.getName(), requestFile);

        apiService.uploadPostImage(body).enqueue(new Callback<ApiResponse<FileUploadResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<FileUploadResponse>> call, Response<ApiResponse<FileUploadResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<FileUploadResponse>> call, Throwable t) {
                Log.e(TAG, "Upload post image failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void uploadAvatar(File imageFile, ResultCallback<FileUploadResponse> callback) {
        String mimeType = getMimeTypeFromFile(imageFile);
        RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), imageFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", imageFile.getName(), requestFile);

        apiService.uploadAvatar(body).enqueue(new Callback<ApiResponse<FileUploadResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<FileUploadResponse>> call, Response<ApiResponse<FileUploadResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<FileUploadResponse>> call, Throwable t) {
                Log.e(TAG, "Upload avatar failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void uploadFoodImage(File imageFile, ResultCallback<FileUploadResponse> callback) {
        String mimeType = getMimeTypeFromFile(imageFile);
        RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), imageFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", imageFile.getName(), requestFile);

        apiService.uploadFoodImage(body).enqueue(new Callback<ApiResponse<FileUploadResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<FileUploadResponse>> call, Response<ApiResponse<FileUploadResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<FileUploadResponse>> call, Throwable t) {
                Log.e(TAG, "Upload food image failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void deleteFile(String key, ResultCallback<Void> callback) {
        apiService.deleteFile(key).enqueue(new Callback<ApiResponse<Void>>() {
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
                Log.e(TAG, "Delete file failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // ==================== Helper ====================

    /**
     * Determine MIME type from file extension
     * Backend only accepts: image/jpeg, image/png, image/gif, image/webp
     */
    private String getMimeTypeFromFile(File file) {
        String filename = file.getName().toLowerCase();

        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (filename.endsWith(".png")) {
            return "image/png";
        } else if (filename.endsWith(".gif")) {
            return "image/gif";
        } else if (filename.endsWith(".webp")) {
            return "image/webp";
        }

        // Default to jpeg if unknown
        return "image/jpeg";
    }

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
