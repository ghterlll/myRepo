package com.aura.starter.network.models;

import java.util.List;

public class CommentThreadResponse {
    private CommentResponse root;
    private List<CommentResponse> replies;

    public CommentResponse getRoot() { return root; }
    public void setRoot(CommentResponse root) { this.root = root; }
    
    public List<CommentResponse> getReplies() { return replies; }
    public void setReplies(List<CommentResponse> replies) { this.replies = replies; }
}
