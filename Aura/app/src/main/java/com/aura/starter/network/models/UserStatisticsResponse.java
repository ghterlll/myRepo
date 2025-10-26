package com.aura.starter.network.models;

public class UserStatisticsResponse {
    private Long joinedDays;
    private Long mealCount;
    private Long healthyDays;

    public UserStatisticsResponse() {}

    public UserStatisticsResponse(Long joinedDays, Long mealCount, Long healthyDays) {
        this.joinedDays = joinedDays;
        this.mealCount = mealCount;
        this.healthyDays = healthyDays;
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
}
