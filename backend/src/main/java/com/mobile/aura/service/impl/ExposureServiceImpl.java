package com.mobile.aura.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mobile.aura.domain.content.ContentExposure;
import com.mobile.aura.domain.content.ContentFeature;
import com.mobile.aura.domain.content.Post;
import com.mobile.aura.domain.content.PostTag;
import com.mobile.aura.dto.user.ExposureCreateReq;
import com.mobile.aura.dto.user.ExposureDto;
import com.mobile.aura.dto.user.ExposureQueryReq;
import com.mobile.aura.mapper.ContentExposureMapper;
import com.mobile.aura.mapper.ContentFeatureMapper;
import com.mobile.aura.mapper.PostMapper;
import com.mobile.aura.mapper.PostStatisticsMapper;
import com.mobile.aura.mapper.PostTagMapper;
import com.mobile.aura.mapper.UserProfileMapper;
import com.mobile.aura.mapper.UserStatsMapper;
import com.mobile.aura.service.ExposureService;
import com.mobile.aura.service.UserFeatureService;
import com.mobile.aura.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExposureServiceImpl implements ExposureService {
    private final ContentExposureMapper exposureMapper;
    private final ContentFeatureMapper contentFeatureMapper;
    private final UserProfileMapper profileMapper;
    private final UserStatsMapper statsMapper;
    private final PostMapper postMapper;
    private final PostStatisticsMapper postStatisticsMapper;
    private final PostTagMapper postTagMapper;
    private final WeatherService weatherService;
    private final UserFeatureService userFeatureService;

    @Override
    public void createExposure(Long userId, ExposureCreateReq req) {
        ContentExposure row = new ContentExposure();
        row.setUserId(userId);
        row.setContentId(req.getContentId());
        row.setPlatform(req.getPlatform());
        row.setDevice(req.getDevice());
        row.setCity(req.getCity());
        row.setExposureTime(LocalDateTime.now());
        row.setWeekday(java.time.LocalDate.now().getDayOfWeek().getValue());

        // Get weather: use client-provided weather if available, otherwise fetch from API
        String weather = req.getWeather();
        if (weather == null || weather.isBlank()) {
            weather = weatherService.getCurrentWeatherByCity(req.getCity());
        }
        row.setWeather(weather);

        // Create user feature snapshot and link it
        Long userFeatureId = userFeatureService.createSnapshot(userId);
        row.setUserFeatureId(userFeatureId);

        // Create content_feature snapshot and link it
        try {
            Long postId = Long.valueOf(req.getContentId());
            Post post = postMapper.selectById(postId);
            if (post != null) {
                // Fetch tag IDs for this post
                String tagIds = getTagIdsForPost(postId);

                // Fetch heat score from statistics table
                var stats = postStatisticsMapper.selectById(postId);
                var heatScore = stats != null ? stats.getHeatScore() : java.math.BigDecimal.ZERO;

                ContentFeature feature = ContentFeature.createFromPost(post, tagIds, heatScore);
                contentFeatureMapper.insert(feature);
                row.setContentFeatureId(feature.getId());
            }
        } catch (NumberFormatException ignore) {
            // Content ID is not a valid post ID, skip content feature
        }

        exposureMapper.insert(row);
    }

    @Override
    public List<ExposureDto> getExposures(Long userId, ExposureQueryReq req) {
        LambdaQueryWrapper<ContentExposure> qw = new LambdaQueryWrapper<>();
        qw.eq(ContentExposure::getUserId, userId);

        // Apply filters
        if (req.getStartDate() != null) {
            qw.ge(ContentExposure::getExposureTime, req.getStartDate().atStartOfDay());
        }
        if (req.getEndDate() != null) {
            qw.le(ContentExposure::getExposureTime, req.getEndDate().atTime(LocalTime.MAX));
        }
        if (req.getCity() != null && !req.getCity().isBlank()) {
            qw.eq(ContentExposure::getCity, req.getCity());
        }
        if (req.getWeather() != null && !req.getWeather().isBlank()) {
            qw.eq(ContentExposure::getWeather, req.getWeather());
        }
        if (req.getDevice() != null && !req.getDevice().isBlank()) {
            qw.eq(ContentExposure::getDevice, req.getDevice());
        }
        if (req.getWeekday() != null) {
            qw.eq(ContentExposure::getWeekday, req.getWeekday());
        }

        // Order by exposure time descending
        qw.orderByDesc(ContentExposure::getExposureTime);

        // Pagination
        int offset = (req.getPage() - 1) * req.getPageSize();
        qw.last("LIMIT " + req.getPageSize() + " OFFSET " + offset);

        List<ContentExposure> exposures = exposureMapper.selectList(qw);

        // Convert to DTOs (with content_feature join)
        return exposures.stream().map(exp -> {
            ExposureDto dto = new ExposureDto();
            dto.setId(exp.getId());
            dto.setContentId(exp.getContentId());
            dto.setExposureTime(exp.getExposureTime());
            dto.setWeekday(exp.getWeekday());
            dto.setWeather(exp.getWeather());
            dto.setDevice(exp.getDevice());
            dto.setCity(exp.getCity());
            dto.setPlatform(exp.getPlatform());

            // Fetch content feature if available
            if (exp.getContentFeatureId() != null) {
                ContentFeature feature = contentFeatureMapper.selectById(exp.getContentFeatureId());
                if (feature != null) {
                    dto.setAuthorId(feature.getAuthorId());
                    dto.setTags(feature.getTagIds());
                }
            }

            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Get comma-separated tag IDs for a post
     */
    private String getTagIdsForPost(Long postId) {
        List<PostTag> postTags = postTagMapper.selectList(
                new LambdaQueryWrapper<PostTag>().eq(PostTag::getPostId, postId)
        );

        if (postTags == null || postTags.isEmpty()) {
            return "";
        }

        return postTags.stream()
                .map(PostTag::getTagId)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private static String limit(String s,int max){ return s==null?null:(s.length()<=max?s:s.substring(0,max)); }
}

