package com.aura.starter.network.models;

public class FileUploadResponse {
    private String url;
    private String key;
    private String mimeType;
    private Long bytes;

    public FileUploadResponse() {}

    public FileUploadResponse(String url, String key, String mimeType, Long bytes) {
        this.url = url;
        this.key = key;
        this.mimeType = mimeType;
        this.bytes = bytes;
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

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getBytes() {
        return bytes;
    }

    public void setBytes(Long bytes) {
        this.bytes = bytes;
    }
}

