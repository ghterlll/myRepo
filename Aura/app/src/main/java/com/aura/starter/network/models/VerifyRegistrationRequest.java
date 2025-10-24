package com.aura.starter.network.models;

public class VerifyRegistrationRequest {
    private String email;
    private String code;
    private String deviceId;

    public VerifyRegistrationRequest(String email, String code, String deviceId) {
        this.email = email;
        this.code = code;
        this.deviceId = deviceId;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
}
