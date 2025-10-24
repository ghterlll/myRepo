package com.aura.starter.network.models;

public class RegisterRequest {
    private String phone;  // Backend still expects 'phone' field but we'll put email in it
    private String password;
    private String nickname;
    private String deviceId;

    public RegisterRequest(String email, String password, String nickname, String deviceId) {
        this.phone = email;  // Put email in phone field for backend compatibility
        this.password = password;
        this.nickname = nickname;
        this.deviceId = deviceId;
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
}

