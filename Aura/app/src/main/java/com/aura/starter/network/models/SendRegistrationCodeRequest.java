package com.aura.starter.network.models;

public class SendRegistrationCodeRequest {
    private String email;

    public SendRegistrationCodeRequest(String email) {
        this.email = email;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
