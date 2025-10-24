package com.aura.starter.network.models;

public class RegisterWithOtpRequest {
    private String email;
    private String password;
    private String nickname;
    private String phone;
    private String regionCode;
    private String city;

    public RegisterWithOtpRequest(String email, String password, String nickname, String phone, String regionCode, String city) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.phone = phone;
        this.regionCode = regionCode;
        this.city = city;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getRegionCode() { return regionCode; }
    public void setRegionCode(String regionCode) { this.regionCode = regionCode; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
}
