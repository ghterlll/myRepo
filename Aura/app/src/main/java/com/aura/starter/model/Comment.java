package com.aura.starter.model;

import java.io.Serializable;

public class Comment implements Serializable {
    public String id;
    public String postId;
    public String author;
    public String content;
    public String timestamp;
    public int likeCount;
    public boolean liked;

    public Comment(String id, String postId, String author, String content, String timestamp) {
        this.id = id;
        this.postId = postId;
        this.author = author;
        this.content = content;
        this.timestamp = timestamp;
        this.likeCount = 0;
        this.liked = false;
    }
}
