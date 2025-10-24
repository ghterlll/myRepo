package com.mobile.aura.dto.user;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ExposureQueryReq {
    private LocalDate startDate;    // Filter by date range
    private LocalDate endDate;
    private String city;            // Filter by city
    private String weather;         // Filter by weather
    private String device;          // Filter by device
    private Integer weekday;        // Filter by weekday (1-7)
    private Integer page = 1;       // Pagination
    private Integer pageSize = 20;  // Default 20 records per page
}
