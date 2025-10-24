package com.mobile.aura.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mobile.aura.domain.user.UserFeature;
import com.mobile.aura.domain.user.UserHealthProfile;
import com.mobile.aura.domain.user.UserProfile;
import com.mobile.aura.domain.user.UserSocialStats;
import com.mobile.aura.mapper.UserFeatureMapper;
import com.mobile.aura.mapper.UserHealthProfileMapper;
import com.mobile.aura.mapper.UserProfileMapper;
import com.mobile.aura.mapper.UserSocialStatsMapper;
import com.mobile.aura.service.UserFeatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserFeatureServiceImpl implements UserFeatureService {

    private final UserFeatureMapper userFeatureMapper;
    private final UserProfileMapper profileMapper;
    private final UserHealthProfileMapper healthProfileMapper;
    private final UserSocialStatsMapper socialStatsMapper;

    @Override
    public Long createSnapshot(Long userId) {
        UserFeature feature = new UserFeature();
        feature.setUserId(userId);
        feature.setSnapshotTime(LocalDateTime.now());

        // Get user profile data
        UserProfile profile = profileMapper.selectById(userId);
        if (profile != null) {
            feature.setAge(profile.getAge());
            feature.setRegion(profile.getLocation());
            feature.setInterests(limit(profile.getInterests()));
        }

        // Get user health profile data
        UserHealthProfile healthProfile = healthProfileMapper.selectOne(
                new LambdaQueryWrapper<UserHealthProfile>()
                        .eq(UserHealthProfile::getUserId, userId)
        );
        if (healthProfile != null) {
            feature.setActivityLvl(healthProfile.getActivityLvl());
        }

        // Get user social stats
        UserSocialStats socialStats = socialStatsMapper.selectOne(
                new LambdaQueryWrapper<UserSocialStats>()
                        .eq(UserSocialStats::getUserId, userId)
        );
        if (socialStats != null) {
            feature.setFollowCount(socialStats.getFollowCount());
        }

        // Insert into database
        userFeatureMapper.insert(feature);

        log.debug("Created user feature snapshot for userId={}, featureId={}", userId, feature.getId());

        return feature.getId();
    }

    private static String limit(String s) {
        return s == null ? null : (s.length() <= 256 ? s : s.substring(0, 256));
    }
}
