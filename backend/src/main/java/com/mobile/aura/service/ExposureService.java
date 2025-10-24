package com.mobile.aura.service;

import com.mobile.aura.dto.user.ExposureCreateReq;
import com.mobile.aura.dto.user.ExposureDto;
import com.mobile.aura.dto.user.ExposureQueryReq;

import java.util.List;

public interface ExposureService {
    void createExposure(Long userId, ExposureCreateReq req);

    List<ExposureDto> getExposures(Long userId, ExposureQueryReq req);
}