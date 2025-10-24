package com.aura.starter.network.models;

public class FileUploadResponse {
    private String url;
    private String key;

    public FileUploadResponse() {}

    public FileUploadResponse(String url, String key) {
        this.url = url;
        this.key = key;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}

