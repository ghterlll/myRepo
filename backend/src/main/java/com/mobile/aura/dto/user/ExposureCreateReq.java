// src/main/java/com/example/demo2/dto/ExposureCreateReq.java
package com.mobile.aura.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class ExposureCreateReq {
    private String contentId;
    private String platform;
    private String device;
    private String city;
    private String weather;  // Optional: can be provided by client or fetched by server
}
