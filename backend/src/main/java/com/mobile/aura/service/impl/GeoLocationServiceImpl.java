package com.mobile.aura.service.impl;

import com.mobile.aura.dto.external.IpApiResponse;
import com.mobile.aura.service.GeoLocationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of GeoLocationService that uses ip-api.com for IP-based geolocation
 * Falls back to IP-based geolocation when client doesn't provide location
 */
@Slf4j
@Service
public class GeoLocationServiceImpl implements GeoLocationService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            int index = ip.indexOf(',');
            if (index != -1) {
                ip = ip.substring(0, index);
            }
            return ip.trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }

        ip = request.getRemoteAddr();
        return ip != null ? ip.trim() : "unknown";
    }

    @Override
    public LocationInfo getLocationFromIp(String ip) {
        // Handle localhost and private IPs
        if (ip == null || ip.isEmpty() ||
            ip.equals("0:0:0:0:0:0:0:1") ||
            ip.equals("127.0.0.1") ||
            ip.startsWith("192.168.") ||
            ip.startsWith("10.") ||
            ip.equals("unknown")) {
            log.debug("Localhost or private IP detected: {}, returning default location", ip);
            return LocationInfo.unknown();
        }

        try {
            String url = "http://ip-api.com/json/" + ip + "?fields=status,country,countryCode,region,regionName,city";
            IpApiResponse response = restTemplate.getForObject(url, IpApiResponse.class);

            if (response != null && "success".equals(response.getStatus())) {
                log.info("Location resolved for IP {}: {} - {}", ip, response.getCountryCode(), response.getCity());
                return new LocationInfo(response.getCountryCode(), response.getCity());
            }
        } catch (Exception e) {
            log.warn("Failed to get location for IP {}: {}", ip, e.getMessage());
        }

        return LocationInfo.unknown();
    }

    @Override
    public LocationInfo getLocationFromRequest(HttpServletRequest request) {
        String ip = getClientIp(request);
        return getLocationFromIp(ip);
    }
}
