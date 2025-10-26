package com.aura.starter.network.models;

public class UserStatisticsResponse {
    private Long joinedDays;
    private Long mealCount;
    private Long healthyDays;
    private Long postCount;

    public UserStatisticsResponse() {}

    public UserStatisticsResponse(Long joinedDays, Long mealCount, Long healthyDays, Long postCount) {
        this.joinedDays = joinedDays;
        this.mealCount = mealCount;
        this.healthyDays = healthyDays;
        this.postCount = postCount;
    }

    public Long getJoinedDays() {
        return joinedDays;
    }

    public void setJoinedDays(Long joinedDays) {
        this.joinedDays = joinedDays;
    }

    public Long getMealCount() {
        return mealCount;
    }

    public void setMealCount(Long mealCount) {
        this.mealCount = mealCount;
    }

    public Long getHealthyDays() {
        return healthyDays;
    }

    public void setHealthyDays(Long healthyDays) {
        this.healthyDays = healthyDays;
    }

    public Long getPostCount() {
        return postCount;
    }

    public void setPostCount(Long postCount) {
        this.postCount = postCount;
    }
}
