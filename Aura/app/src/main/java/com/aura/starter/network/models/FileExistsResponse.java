package com.aura.starter.network.models;

public class FileExistsResponse {
    private boolean exists;

    public FileExistsResponse() {}

    public FileExistsResponse(boolean exists) {
        this.exists = exists;
    }

    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }
}

