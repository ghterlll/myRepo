package com.aura.starter.network;

import com.aura.starter.network.models.*;

import java.util.List;
import java.util.Map;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {
    
    // ==================== login ====================
    
    @POST("api/v1/user/register")
    Call<ApiResponse<Void>> registerWithOtp(@Body RegisterWithOtpRequest request);

    @POST("api/v1/user/register/code")
    Call<ApiResponse<Void>> sendRegistrationCode(@Body SendRegistrationCodeRequest request);

    @POST("api/v1/user/register/verify")
    Call<ApiResponse<TokenResponse>> verifyRegistration(@Body VerifyRegistrationRequest request);
    
    @POST("api/v1/user/auth/login")
    Call<ApiResponse<TokenResponse>> login(@Body LoginRequest request);
    
    @POST("api/v1/user/auth/refresh")
    Call<ApiResponse<TokenResponse>> refreshToken(@Body RefreshTokenRequest request);
    
    // ==================== user info ====================

    @GET("api/v1/user/me")
    Call<ApiResponse<UserProfileResponse>> getMyProfile();

    @PATCH("api/v1/user/me/profile")
    Call<ApiResponse<Void>> updateMyProfile(@Body UserProfileUpdateRequest request);

    @GET("api/v1/user/me/statistics")
    Call<ApiResponse<UserStatisticsResponse>> getMyStatistics();
    
    // ==================== intake record ====================
    
    // Meal module (per backend: /api/v1/meal/...)
    @POST("api/v1/meal/from-source")
    Call<ApiResponse<MealLogIdResponse>> addMealFromSource(@Body MealAddRequest request);

    @POST("api/v1/meal/free-input")
    Call<ApiResponse<MealLogIdResponse>> addMealFreeInput(@Body MealAddRequest request);
    
    @GET("api/v1/meal/day")
    Call<ApiResponse<DailySummaryResponse>> getDailySummary(@Query("date") String date);
    
    @DELETE("api/v1/meal/{id}")
    Call<ApiResponse<Void>> deleteMeal(@Path("id") Long id);
    
    // ==================== food library ====================
    
    @GET("api/v1/food/my")
    Call<ApiResponse<FoodSearchResponse>> searchFoods(
            @Query("q") String query,
            @Query("category") String category,
            @Query("limit") Integer limit,
            @Query("offset") Integer offset,
            @Query("scope") String scope // e.g. "system" | "custom" | "all"
    );
    
    @POST("api/v1/food/my/foods")
    Call<ApiResponse<UserFoodItemResponse>> createUserFoodItem(@Body UserFoodItemRequest request);
    
    @GET("api/v1/food/my")
    Call<ApiResponse<UserFoodItemListResponse>> getUserFoodItems();
    
    // ==================== water record ====================
    
    @POST("api/v1/water/increment")
    Call<ApiResponse<Void>> addWater(@Body WaterAddRequest request);
    
    @GET("api/v1/water/day")
    Call<ApiResponse<WaterDailySummaryResponse>> getWaterDailySummary(@Query("date") String date);
    
    // Backend expects body for DELETE /day; keep signature if UI uses query; adjust later if needed
    @DELETE("api/v1/water/day")
    Call<ApiResponse<Void>> deleteWater(@Query("date") String date);
    
    @GET("api/v1/water/range")
    Call<ApiResponse<WaterRangeResponse>> getWaterRange(@Query("from") String from, @Query("to") String to);
    
    // ==================== weight records ====================
    
    @POST("api/v1/weight")
    Call<ApiResponse<Void>> logWeight(@Body WeightLogRequest request);
    
    @GET("api/v1/weight/range")
    Call<ApiResponse<WeightHistoryResponse>> getWeightHistory(
            @Query("start") String start,
            @Query("end") String end
    );
    
    @GET("api/v1/weight/latest")
    Call<ApiResponse<WeightLogResponse>> getLatestWeight();

    @PATCH("api/v1/weight/initial")
    Call<ApiResponse<Void>> updateInitialWeight(@Body UpdateInitialWeightRequest request);
    
    // ==================== exercise records ====================
    
    @POST("api/v1/exercise/add")
    Call<ApiResponse<Void>> addExercise(@Body ExerciseAddRequest request);
    
    @GET("api/v1/exercise/daily-workout")
    Call<ApiResponse<DailyWorkoutResponse>> getDailyWorkout(@Query("date") String date);
    
    // ==================== daily overview ====================
    
    @GET("api/v1/overview/day")
    Call<ApiResponse<DayOverviewResponse>> getDayOverview(@Query("date") String date);
    
    // ==================== file upload ====================
    
    @Multipart
    @POST("api/v1/files/food/image")
    Call<ApiResponse<FileUploadResponse>> uploadFoodImage(@Part MultipartBody.Part file);
    
    @Multipart
    @POST("api/v1/files/avatar")
    Call<ApiResponse<FileUploadResponse>> uploadAvatar(@Part MultipartBody.Part file);
    
    @Multipart
    @POST("api/v1/files/post/image")
    Call<ApiResponse<FileUploadResponse>> uploadPostImage(@Part MultipartBody.Part file);
    
    @Multipart
    @POST("api/v1/files/post/images/batch")
    Call<ApiResponse<FileUploadBatchResponse>> uploadPostImagesBatch(@Part List<MultipartBody.Part> files);
    
    @DELETE("api/v1/files")
    Call<ApiResponse<Void>> deleteFile(@Query("key") String key);
    
    @GET("api/v1/files/presigned-url")
    Call<ApiResponse<PresignedUrlResponse>> getPresignedUrl(
            @Query("key") String key,
            @Query("expirationMinutes") int expirationMinutes
    );
    
    @GET("api/v1/files/exists")
    Call<ApiResponse<FileExistsResponse>> fileExists(@Query("key") String key);

    // ==================== test interface ====================
    
    @GET("api/test/ping")
    Call<PingResp> ping();

    @GET("api/test/hello")
    Call<String> hello();


    // ==================== posts ====================

    @POST("api/v1/post")
    Call<ApiResponse<Map<String, Long>>> createPost(@Body PostCreateRequest request);

    @GET("api/v1/post")
    Call<ApiResponse<PageResponse<PostCardResponse>>> listPosts(
            @Query("limit") Integer limit,
            @Query("cursor") String cursor
    );

    @GET("api/v1/post/feed/followings")
    Call<ApiResponse<PageResponse<PostCardResponse>>> listFollowingPosts(
            @Query("limit") Integer limit,
            @Query("cursor") String cursor
    );

    @GET("api/v1/post/{postId}")
    Call<ApiResponse<PostDetailResponse>> getPostDetail(@Path("postId") Long postId);

    @PATCH("api/v1/post/{postId}")
    Call<ApiResponse<Void>> updatePost(@Path("postId") Long postId, @Body PostUpdateRequest request);

    @DELETE("api/v1/post/{postId}")
    Call<ApiResponse<Void>> deletePost(@Path("postId") Long postId);

    @POST("api/v1/post/{postId}/publish")
    Call<ApiResponse<Void>> publishPost(@Path("postId") Long postId);

    @POST("api/v1/post/{postId}/hide")
    Call<ApiResponse<Void>> hidePost(@Path("postId") Long postId);

    @PUT("api/v1/post/{postId}/media")
    Call<ApiResponse<Void>> replacePostMedia(@Path("postId") Long postId, @Body List<MediaItem> medias);

    // ==================== likes and bookmarks ====================

    @POST("api/v1/post/{postId}/like")
    Call<ApiResponse<Void>> likePost(@Path("postId") Long postId);

    @DELETE("api/v1/post/{postId}/like")
    Call<ApiResponse<Void>> unlikePost(@Path("postId") Long postId);

    @POST("api/v1/post/{postId}/bookmark")
    Call<ApiResponse<Void>> bookmarkPost(@Path("postId") Long postId);

    @DELETE("api/v1/post/{postId}/bookmark")
    Call<ApiResponse<Void>> unbookmarkPost(@Path("postId") Long postId);

    // ==================== comments ====================

    @POST("api/v1/post/{postId}/comments")
    Call<ApiResponse<Map<String, Long>>> createComment(@Path("postId") Long postId, @Body CommentCreateRequest request);

    @GET("api/v1/post/{postId}/comments")
    Call<ApiResponse<PageResponse<CommentThreadResponse>>> listComments(
            @Path("postId") Long postId,
            @Query("limit") Integer limit,
            @Query("cursor") String cursor,
            @Query("previewSize") Integer previewSize
    );

    @DELETE("api/v1/post/comments/{commentId}")
    Call<ApiResponse<Void>> deleteComment(@Path("commentId") Long commentId);

    @GET("api/v1/post/comments/{rootCommentId}/replies")
    Call<ApiResponse<PageResponse<CommentResponse>>> listCommentReplies(
            @Path("rootCommentId") Long rootCommentId,
            @Query("limit") Integer limit,
            @Query("cursor") String cursor
    );

    // ==================== search ====================

    @GET("api/v1/post/search")
    Call<ApiResponse<PageResponse<PostCardResponse>>> searchPosts(
            @Query("keyword") String keyword,
            @Query("category") String category,
            @Query("limit") Integer limit,
            @Query("cursor") String cursor
    );

    @GET("api/v1/tag")
    Call<ApiResponse<PageResponse<TagResponse>>> searchTags(
            @Query("q") String query,
            @Query("limit") Integer limit,
            @Query("cursor") String cursor
    );

    @GET("api/v1/tag/{tagId}/posts")
    Call<ApiResponse<PageResponse<PostCardResponse>>> getPostsByTag(
            @Path("tagId") Long tagId,
            @Query("limit") Integer limit,
            @Query("cursor") String cursor
    );
}
