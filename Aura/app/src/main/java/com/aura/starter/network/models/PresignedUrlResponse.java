package com.aura.starter.network.models;

public class PresignedUrlResponse {
    private String url;

    public PresignedUrlResponse() {}

    public PresignedUrlResponse(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

