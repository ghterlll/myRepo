package com.aura.starter.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Manager for local post interaction state (likes, bookmarks)
 *
 * Since backend doesn't provide interaction state in PostCardResp,
 * we maintain local state here and sync with backend APIs.
 */
public class PostInteractionManager {
    private static final String PREF_NAME = "post_interactions";
    private static final String KEY_LIKED_POSTS = "liked_posts";
    private static final String KEY_BOOKMARKED_POSTS = "bookmarked_posts";

    private final SharedPreferences prefs;
    private static PostInteractionManager instance;

    private PostInteractionManager(Context context) {
        this.prefs = context.getApplicationContext()
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized PostInteractionManager getInstance(Context context) {
        if (instance == null) {
            instance = new PostInteractionManager(context);
        }
        return instance;
    }

    // ==================== Like State ====================

    public boolean isLiked(String postId) {
        Set<String> likedPosts = prefs.getStringSet(KEY_LIKED_POSTS, new HashSet<>());
        return likedPosts.contains(postId);
    }

    public void setLiked(String postId, boolean liked) {
        Set<String> likedPosts = new HashSet<>(prefs.getStringSet(KEY_LIKED_POSTS, new HashSet<>()));
        if (liked) {
            likedPosts.add(postId);
        } else {
            likedPosts.remove(postId);
        }
        prefs.edit().putStringSet(KEY_LIKED_POSTS, likedPosts).apply();
    }

    public void toggleLike(String postId) {
        setLiked(postId, !isLiked(postId));
    }

    // ==================== Bookmark State ====================

    public boolean isBookmarked(String postId) {
        Set<String> bookmarkedPosts = prefs.getStringSet(KEY_BOOKMARKED_POSTS, new HashSet<>());
        return bookmarkedPosts.contains(postId);
    }

    public void setBookmarked(String postId, boolean bookmarked) {
        Set<String> bookmarkedPosts = new HashSet<>(prefs.getStringSet(KEY_BOOKMARKED_POSTS, new HashSet<>()));
        if (bookmarked) {
            bookmarkedPosts.add(postId);
        } else {
            bookmarkedPosts.remove(postId);
        }
        prefs.edit().putStringSet(KEY_BOOKMARKED_POSTS, bookmarkedPosts).apply();
    }

    public void toggleBookmark(String postId) {
        setBookmarked(postId, !isBookmarked(postId));
    }

    /**
     * Get all bookmarked post IDs
     */
    public Set<String> getAllBookmarkedIds() {
        return new HashSet<>(prefs.getStringSet(KEY_BOOKMARKED_POSTS, new HashSet<>()));
    }

    /**
     * Clear all interaction state (useful for logout)
     */
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
