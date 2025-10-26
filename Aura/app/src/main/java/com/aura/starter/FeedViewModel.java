package com.aura.starter;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.aura.starter.network.PostRepository;
import com.aura.starter.network.models.*;
import com.aura.starter.model.Post;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FeedViewModel extends ViewModel {
    private final PostRepository postRepo = PostRepository.getInstance();
    private final MutableLiveData<List<Post>> displayedPosts = new MutableLiveData<>();
    private final MutableLiveData<String> currentCursor = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<Boolean> hasMorePages = new MutableLiveData<>();
    private int points = 120;
    private final int pageSize = 20; // 按照要求设置为20
    private boolean isRefreshing = false;

    public FeedViewModel() {
        hasMorePages.setValue(true);
        isLoading.setValue(false);
        loadInitialPosts();
    }

    public LiveData<List<Post>> getDisplayedPosts() { return displayedPosts; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getHasMorePages() { return hasMorePages; }
    public LiveData<String> getCurrentCursor() { return currentCursor; }

    /**
     * 加载初始帖子（第一页）
     */
    public void loadInitialPosts() {
        currentCursor.setValue(null); // null表示第一页
        hasMorePages.setValue(true);
        isLoading.setValue(true);
        loadPostsInternal();
    }

    /**
     * 加载更多帖子（分页）
     */
    public void loadMorePosts() {
        if (Boolean.TRUE.equals(isLoading.getValue()) || Boolean.FALSE.equals(hasMorePages.getValue())) {
            return;
        }

        isLoading.setValue(true);
        loadPostsInternal();
    }

    /**
     * 下拉刷新 - 重新加载最新帖子
     */
    public void refreshPosts() {
        if (Boolean.TRUE.equals(isRefreshing)) {
            return;
        }

        isRefreshing = true;
        currentCursor.setValue(null); // 重置游标，获取最新帖子
        hasMorePages.setValue(true);
        isLoading.setValue(true);
        loadPostsInternal();
    }

    /**
     * 内部方法：从后端API加载帖子
     */
    private void loadPostsInternal() {
        String cursor = currentCursor.getValue();
        Integer limit = pageSize;

        // 使用PostRepository从后端获取数据
        postRepo.listPosts(limit, cursor, new PostRepository.ResultCallback<PageResponse<PostCardResponse>>() {
            @Override
            public void onSuccess(PageResponse<PostCardResponse> data) {
                List<Post> currentDisplayed = displayedPosts.getValue();
                if (currentDisplayed == null) {
                    currentDisplayed = new ArrayList<>();
                }

                // 转换后端数据为前端Post模型
                List<Post> newPosts = convertPostCardResponseToPosts(data.getItems());

                if (cursor == null) {
                    // 初始加载或刷新：替换所有数据
                    currentDisplayed.clear();
                    currentDisplayed.addAll(newPosts);
                } else {
                    // 加载更多：追加数据
                    currentDisplayed.addAll(newPosts);
                }

                displayedPosts.setValue(currentDisplayed);

                // 更新分页状态
                String nextCursor = data.getNextCursor();
                currentCursor.setValue(nextCursor);
                hasMorePages.setValue(data.getHasMore());

                isLoading.setValue(false);
                isRefreshing = false;
            }

            @Override
            public void onError(String message) {
                android.util.Log.e("FeedViewModel", "Load posts failed: " + message);
                isLoading.setValue(false);
                isRefreshing = false;

                // 错误时显示空列表
                displayedPosts.setValue(new ArrayList<>());
                hasMorePages.setValue(false);
            }
        });
    }

    /**
     * 转换后端PostCardResponse为前端Post模型
     */
    private List<Post> convertPostCardResponseToPosts(List<PostCardResponse> responses) {
        List<Post> posts = new ArrayList<>();
        for (PostCardResponse response : responses) {
            android.util.Log.d("FeedViewModel", "Converting post - ID: " + response.getId() +
                ", Title: " + response.getTitle() + ", CoverUrl: " + response.getCoverUrl());

            Post post = new Post(
                response.getId().toString(),
                "User" + response.getAuthorId(), // Keep authorId for backward compatibility
                response.getTitle(),
                "", // 后端PostCardResp目前没有content字段
                "fitness,diet", // TODO: 后端需要添加tags字段到PostCardResp
                response.getCoverUrl()
            );
            // Set author nickname from backend
            post.authorNickname = response.getAuthorNickname();

            // 设置创建时间
            try {
                post.createdAt = Long.parseLong(response.getCreatedAt());
            } catch (NumberFormatException e) {
                post.createdAt = System.currentTimeMillis();
            }
            posts.add(post);
        }
        android.util.Log.d("FeedViewModel", "Converted " + posts.size() + " posts");
        return posts;
    }

    public boolean hasMorePages() {
        Boolean hasMore = hasMorePages.getValue();
        return hasMore != null && hasMore;
    }

    // ==================== 社交互动方法 ====================

    public void toggleLike(Long postId) {
        postRepo.likePost(postId, new PostRepository.ResultCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                // 更新本地UI状态 - 这里应该刷新对应帖子的状态
                refreshCurrentPosts();
            }

            @Override
            public void onError(String message) {
                android.util.Log.e("FeedViewModel", "Like post failed: " + message);
            }
        });
    }

    public void toggleBookmark(Long postId) {
        postRepo.bookmarkPost(postId, new PostRepository.ResultCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                // 更新本地UI状态
                refreshCurrentPosts();
            }

            @Override
            public void onError(String message) {
                android.util.Log.e("FeedViewModel", "Bookmark post failed: " + message);
            }
        });
    }

    /**
     * 刷新当前显示的帖子（用于更新点赞等状态）
     */
    private void refreshCurrentPosts() {
        // 重新加载当前页面以更新状态
        loadPostsInternal();
    }

    // ==================== 兼容性方法 ====================

    public int getPoints(){ return points; }
    public void addPoints(int delta){ points += delta; }
}
