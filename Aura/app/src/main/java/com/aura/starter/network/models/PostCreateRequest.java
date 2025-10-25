package com.aura.starter.network.models;

import java.util.List;

public class PostCreateRequest {
    private String title;
    private String caption;
    private Boolean publish;
    private List<String> tags;
    private List<MediaItem> medias;

    public PostCreateRequest(String title, String caption, Boolean publish, List<String> tags, List<MediaItem> medias) {
        this.title = title;
        this.caption = caption;
        this.publish = publish;
        this.tags = tags;
        this.medias = medias;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    
    public Boolean getPublish() { return publish; }
    public void setPublish(Boolean publish) { this.publish = publish; }
    
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    
    public List<MediaItem> getMedias() { return medias; }
    public void setMedias(List<MediaItem> medias) { this.medias = medias; }
}
