package com.mobile.aura.dto.external;

import lombok.Data;

/**
 * DTO for ip-api.com JSON response
 * See: <a href="http://ip-api.com/docs/api:json">...</a>
 */
@Data
public class IpApiResponse {
    /**
     * Query status: "success" or "fail"
     */
    private String status;

    /**
     * Country name (e.g., "United States")
     */
    private String country;

    /**
     * Two-letter country code ISO 3166-1 alpha-2 (e.g., "US")
     */
    private String countryCode;

    /**
     * Region/State short code (e.g., "CA", "TX")
     */
    private String region;

    /**
     * Region/State full name (e.g., "California", "Texas")
     */
    private String regionName;

    /**
     * City name (e.g., "Mountain View")
     */
    private String city;
}
