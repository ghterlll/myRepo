package com.aura.starter.network.models;

import java.util.List;

public class FileUploadBatchResponse {
    private List<FileUploadResponse> files;

    public FileUploadBatchResponse() {}

    public FileUploadBatchResponse(List<FileUploadResponse> files) {
        this.files = files;
    }

    public List<FileUploadResponse> getFiles() {
        return files;
    }

    public void setFiles(List<FileUploadResponse> files) {
        this.files = files;
    }
}

