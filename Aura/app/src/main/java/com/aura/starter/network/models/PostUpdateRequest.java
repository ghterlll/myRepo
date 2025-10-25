package com.aura.starter.network.models;

import java.util.List;

public class PostUpdateRequest {
    private String title;
    private String caption;
    private List<String> tags;

    public PostUpdateRequest(String title, String caption, List<String> tags) {
        this.title = title;
        this.caption = caption;
        this.tags = tags;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
