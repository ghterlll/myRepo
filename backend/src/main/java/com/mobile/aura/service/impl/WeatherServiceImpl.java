package com.mobile.aura.service.impl;

import com.mobile.aura.service.WeatherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class WeatherServiceImpl implements WeatherService {

    private static final String OPEN_METEO_API = "https://api.open-meteo.com/v1/forecast";
    private final RestTemplate restTemplate = new RestTemplate();

    // City coordinates cache (you can expand this or use a geocoding service)
    private static final Map<String, double[]> CITY_COORDS = new HashMap<>();
    static {
        // Major cities in China
        CITY_COORDS.put("beijing", new double[]{39.9042, 116.4074});
        CITY_COORDS.put("shanghai", new double[]{31.2304, 121.4737});
        CITY_COORDS.put("guangzhou", new double[]{23.1291, 113.2644});
        CITY_COORDS.put("shenzhen", new double[]{22.5431, 114.0579});
        CITY_COORDS.put("chengdu", new double[]{30.5728, 104.0668});
        CITY_COORDS.put("hangzhou", new double[]{30.2741, 120.1551});
        CITY_COORDS.put("wuhan", new double[]{30.5928, 114.3055});
        CITY_COORDS.put("xian", new double[]{34.2658, 108.9541});
        CITY_COORDS.put("chongqing", new double[]{29.4316, 106.9123});
        CITY_COORDS.put("tianjin", new double[]{39.3434, 117.3616});

        // International cities
        CITY_COORDS.put("new york", new double[]{40.7128, -74.0060});
        CITY_COORDS.put("london", new double[]{51.5074, -0.1278});
        CITY_COORDS.put("tokyo", new double[]{35.6762, 139.6503});
        CITY_COORDS.put("paris", new double[]{48.8566, 2.3522});
        CITY_COORDS.put("sydney", new double[]{-33.8688, 151.2093});
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getCurrentWeather(double latitude, double longitude) {
        try {
            String url = String.format(
                "%s?latitude=%.4f&longitude=%.4f&current_weather=true",
                OPEN_METEO_API, latitude, longitude
            );

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("current_weather")) {
                Object currentWeatherObj = response.get("current_weather");
                if (currentWeatherObj instanceof Map) {
                    Map<String, Object> currentWeather = (Map<String, Object>) currentWeatherObj;
                    Object weatherCodeObj = currentWeather.get("weathercode");
                    if (weatherCodeObj instanceof Number) {
                        Integer weatherCode = ((Number) weatherCodeObj).intValue();
                        return mapWeatherCode(weatherCode);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch weather for coordinates ({}, {}): {}", latitude, longitude, e.getMessage());
        }
        return "Unknown";
    }

    @Override
    public String getCurrentWeatherByCity(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            return "Unknown";
        }

        String normalizedCity = cityName.toLowerCase().trim();
        double[] coords = CITY_COORDS.get(normalizedCity);

        if (coords != null) {
            return getCurrentWeather(coords[0], coords[1]);
        }

        log.warn("City not found in cache: {}", cityName);
        return "Unknown";
    }

    /**
     * Map WMO Weather codes to simple weather descriptions
     * Reference: https://open-meteo.com/en/docs
     */
    private String mapWeatherCode(Integer code) {
        if (code == null) return "Unknown";

        return switch (code) {
            case 0 -> "Clear";
            case 1, 2, 3 -> "Cloudy";
            case 45, 48 -> "Foggy";
            case 51, 53, 55, 56, 57 -> "Drizzle";
            case 61, 63, 65, 66, 67 -> "Rainy";
            case 71, 73, 75, 77 -> "Snowy";
            case 80, 81, 82 -> "Showers";
            case 85, 86 -> "Snow Showers";
            case 95, 96, 99 -> "Thunderstorm";
            default -> "Unknown";
        };
    }
}
