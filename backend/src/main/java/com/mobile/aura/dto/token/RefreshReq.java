package com.mobile.aura.dto.token;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshReq {
    @NotBlank
    private String refreshToken;
    @NotBlank private String deviceId;
}
