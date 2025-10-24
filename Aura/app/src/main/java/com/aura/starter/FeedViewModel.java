package com.aura.starter;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.aura.starter.data.AppRepository;
import com.aura.starter.model.Post;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FeedViewModel extends ViewModel {
    private final AppRepository repo = AppRepository.get();
    private int points = 120;
    private int currentPage = 0;
    private final int pageSize = 10;
    private boolean hasMorePages = true;
    private final MutableLiveData<List<Post>> displayedPosts = new MutableLiveData<>();
    private final Random random = new Random();

    public FeedViewModel() {
        loadInitialPosts();
    }

    public LiveData<List<Post>> getDisplayedPosts() { return displayedPosts; }

    /**
     * 加载初始帖子（第一页）
     */
    public void loadInitialPosts() {
        currentPage = 0;
        hasMorePages = true;
        loadMorePosts();
    }

    /**
     * 加载更多帖子（分页）
     */
    public void loadMorePosts() {
        if (!hasMorePages) return;

        List<Post> allPosts = repo.posts().getValue();
        if (allPosts == null || allPosts.isEmpty()) return;

        int startIndex = currentPage * pageSize;
        int endIndex = Math.min(startIndex + pageSize, allPosts.size());

        if (startIndex >= allPosts.size()) {
            hasMorePages = false;
            return;
        }

        List<Post> currentDisplayed = displayedPosts.getValue();
        if (currentDisplayed == null) {
            currentDisplayed = new ArrayList<>();
        }

        // 添加新帖子
        for (int i = startIndex; i < endIndex; i++) {
            currentDisplayed.add(allPosts.get(i));
        }

        displayedPosts.setValue(currentDisplayed);
        currentPage++;

        // 检查是否还有更多帖子
        if (endIndex >= allPosts.size()) {
            hasMorePages = false;
        }
    }

    /**
     * 下拉刷新 - 重新加载随机10个帖子
     */
    public void refreshPosts() {
        List<Post> allPosts = repo.posts().getValue();
        if (allPosts == null || allPosts.size() < pageSize) {
            loadInitialPosts();
            return;
        }

        // 随机选择10个帖子
        List<Post> shuffledPosts = new ArrayList<>(allPosts);
        java.util.Collections.shuffle(shuffledPosts, random);

        List<Post> refreshedPosts = shuffledPosts.subList(0, Math.min(pageSize, shuffledPosts.size()));
        displayedPosts.setValue(refreshedPosts);

        currentPage = 1;
        hasMorePages = shuffledPosts.size() > pageSize;
    }

    public boolean hasMorePages() {
        return hasMorePages;
    }

    // 保留原有方法供其他地方使用
    public LiveData<List<Post>> getPosts(){ return repo.posts(); }
    public void addPost(Post p){ repo.addPost(p); }
    public void toggleLike(String id){ repo.toggleLike(id); }
    public void toggleBookmark(String id){ repo.toggleBookmark(id); }
    public void addComment(String id, String c){ repo.addComment(id, c); }
    public void setImage(String id, String uri){ repo.setImage(id, uri); }

    public int getPoints(){ return points; }
    public void addPoints(int delta){ points += delta; }
}
