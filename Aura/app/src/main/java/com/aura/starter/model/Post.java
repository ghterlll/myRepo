package com.aura.starter.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Post implements Serializable {
    public long createdAt;
    public String id, author, title, content, tags;
    public int likes = 0;
    public boolean liked = false, bookmarked = false;
    public String imageUri;
    public List<String> comments = new ArrayList<>();

    public Post(String id, String author, String title, String content, String tags, String imageUri) {
        this.createdAt = System.currentTimeMillis();
        this.id=id; this.author=author; this.title=title; this.content=content; this.tags=tags; this.imageUri=imageUri;
    }
}
