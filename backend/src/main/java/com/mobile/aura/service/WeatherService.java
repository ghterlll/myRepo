package com.mobile.aura.service;

public interface WeatherService {
    /**
     * Get current weather for a city using coordinates
     * @param latitude latitude coordinate
     * @param longitude longitude coordinate
     * @return weather description (e.g., "Sunny", "Rainy", "Cloudy")
     */
    String getCurrentWeather(double latitude, double longitude);

    /**
     * Get current weather for a city by name
     * @param cityName city name
     * @return weather description
     */
    String getCurrentWeatherByCity(String cityName);
}
