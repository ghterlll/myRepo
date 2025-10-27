package com.aura.starter.network.models;

public class CommentCreateRequest {
    private String content;
    private Long parentId;

    public CommentCreateRequest(String content, Long parentId) {
        this.content = content;
        this.parentId = parentId;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
}
