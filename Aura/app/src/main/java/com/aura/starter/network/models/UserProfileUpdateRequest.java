package com.aura.starter.network.models;

import java.math.BigDecimal;

public class UserProfileUpdateRequest {
    private String nickname;
    private String bio;
    private String avatarUrl;
    private Integer gender;
    private String birthday;
    private Integer heightCm;
    private BigDecimal weightKg;
    private BigDecimal targetWeightKg;

    // Getters and setters
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    
    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }
    
    public Integer getHeightCm() { return heightCm; }
    public void setHeightCm(Integer heightCm) { this.heightCm = heightCm; }
    
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    
    public BigDecimal getTargetWeightKg() { return targetWeightKg; }
    public void setTargetWeightKg(BigDecimal targetWeightKg) { this.targetWeightKg = targetWeightKg; }
}

