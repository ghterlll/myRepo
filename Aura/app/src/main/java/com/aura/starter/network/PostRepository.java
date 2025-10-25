package com.aura.starter.network;

import android.util.Log;

import com.aura.starter.network.models.*;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostRepository {
    private static final String TAG = "PostRepository";
    private final ApiService apiService;
    private static PostRepository instance;

    private PostRepository() {
        apiService = ApiClient.get().create(ApiService.class);
    }

    public static synchronized PostRepository getInstance() {
        if (instance == null) {
            instance = new PostRepository();
        }
        return instance;
    }

    // ==================== Posts ====================

    public void createPost(PostCreateRequest request, ResultCallback<PostIdResponse> callback) {
        apiService.createPost(request).enqueue(new Callback<ApiResponse<PostIdResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<PostIdResponse>> call, Response<ApiResponse<PostIdResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PostIdResponse>> call, Throwable t) {
                Log.e(TAG, "Create post failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void listPosts(Integer limit, Long cursor, ResultCallback<PageResponse<PostCardResponse>> callback) {
        apiService.listPosts(limit, cursor).enqueue(new Callback<ApiResponse<PageResponse<PostCardResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<PageResponse<PostCardResponse>>> call, Response<ApiResponse<PageResponse<PostCardResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PageResponse<PostCardResponse>>> call, Throwable t) {
                Log.e(TAG, "List posts failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void getPostDetail(Long postId, ResultCallback<PostDetailResponse> callback) {
        apiService.getPostDetail(postId).enqueue(new Callback<ApiResponse<PostDetailResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<PostDetailResponse>> call, Response<ApiResponse<PostDetailResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PostDetailResponse>> call, Throwable t) {
                Log.e(TAG, "Get post detail failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // ==================== Likes ====================

    public void likePost(Long postId, ResultCallback<Void> callback) {
        apiService.likePost(postId).enqueue(new Callback<ApiResponse<Void>>() {
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
                Log.e(TAG, "Like post failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void unlikePost(Long postId, ResultCallback<Void> callback) {
        apiService.unlikePost(postId).enqueue(new Callback<ApiResponse<Void>>() {
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
                Log.e(TAG, "Unlike post failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // ==================== Bookmarks ====================

    public void bookmarkPost(Long postId, ResultCallback<Void> callback) {
        apiService.bookmarkPost(postId).enqueue(new Callback<ApiResponse<Void>>() {
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
                Log.e(TAG, "Bookmark post failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void unbookmarkPost(Long postId, ResultCallback<Void> callback) {
        apiService.unbookmarkPost(postId).enqueue(new Callback<ApiResponse<Void>>() {
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
                Log.e(TAG, "Unbookmark post failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // ==================== Comments ====================

    public void createComment(Long postId, CommentCreateRequest request, ResultCallback<CommentIdResponse> callback) {
        apiService.createComment(postId, request).enqueue(new Callback<ApiResponse<CommentIdResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<CommentIdResponse>> call, Response<ApiResponse<CommentIdResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<CommentIdResponse>> call, Throwable t) {
                Log.e(TAG, "Create comment failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void listComments(Long postId, Integer limit, Long cursor, Integer previewSize, ResultCallback<PageResponse<CommentThreadResponse>> callback) {
        apiService.listComments(postId, limit, cursor, previewSize).enqueue(new Callback<ApiResponse<PageResponse<CommentThreadResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<PageResponse<CommentThreadResponse>>> call, Response<ApiResponse<PageResponse<CommentThreadResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PageResponse<CommentThreadResponse>>> call, Throwable t) {
                Log.e(TAG, "List comments failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // ==================== Search ====================

    public void searchTags(String query, Integer limit, Long cursor, ResultCallback<PageResponse<TagResponse>> callback) {
        apiService.searchTags(query, limit, cursor).enqueue(new Callback<ApiResponse<PageResponse<TagResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<PageResponse<TagResponse>>> call, Response<ApiResponse<PageResponse<TagResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PageResponse<TagResponse>>> call, Throwable t) {
                Log.e(TAG, "Search tags failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void getPostsByTag(Long tagId, Integer limit, Long cursor, ResultCallback<PageResponse<PostCardResponse>> callback) {
        apiService.getPostsByTag(tagId, limit, cursor).enqueue(new Callback<ApiResponse<PageResponse<PostCardResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<PageResponse<PostCardResponse>>> call, Response<ApiResponse<PageResponse<PostCardResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PageResponse<PostCardResponse>>> call, Throwable t) {
                Log.e(TAG, "Get posts by tag failed", t);
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
