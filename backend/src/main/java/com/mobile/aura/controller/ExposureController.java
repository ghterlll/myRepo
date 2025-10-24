package com.mobile.aura.controller;

import com.mobile.aura.dto.user.ExposureCreateReq;
import com.mobile.aura.dto.user.ExposureDto;
import com.mobile.aura.dto.user.ExposureQueryReq;
import com.mobile.aura.dto.ResponseResult;
import com.mobile.aura.service.ExposureService;
import com.mobile.aura.support.JwtAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/exposure")
public class ExposureController {
    private final ExposureService exposureService;

    @PostMapping
    public ResponseResult<?> create(@RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
                                    @RequestBody ExposureCreateReq req){
        exposureService.createExposure(userId, req);
        return ResponseResult.success();
    }

    @GetMapping
    public ResponseResult<List<ExposureDto>> getExposures(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            ExposureQueryReq req) {
        if (req == null) {
            req = new ExposureQueryReq();
        }
        List<ExposureDto> exposures = exposureService.getExposures(userId, req);
        return ResponseResult.success(exposures);
    }
}
