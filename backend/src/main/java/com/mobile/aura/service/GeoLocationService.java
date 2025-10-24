package com.mobile.aura.service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Service for extracting geographical location from HTTP requests
 */
public interface GeoLocationService {

    /**
     * Extract client IP address from request
     */
    String getClientIp(HttpServletRequest request);

    /**
     * Get location information from IP address
     */
    LocationInfo getLocationFromIp(String ip);

    /**
     * Get location from HTTP request (extracts IP and looks up location)
     */
    LocationInfo getLocationFromRequest(HttpServletRequest request);

    /**
     * Value object holding location information
     */
    record LocationInfo(String regionCode, String city) {
        public static LocationInfo unknown() {
            return new LocationInfo(null, null);
        }
    }
}
