-- ============================================
-- Database Initialization Script for Aura
-- ============================================
-- This script creates all required tables for the Aura application.
-- Run this script to set up a fresh database instance.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- User Management Tables
-- ============================================

-- User basic information
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `email` VARCHAR(255) NOT NULL COMMENT 'Email address for login',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT 'Phone number (optional)',
  `password` VARCHAR(100) NOT NULL COMMENT 'Encrypted password',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT 'Display name',
  `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT 'Profile picture URL',
  `region_code` VARCHAR(32) DEFAULT NULL COMMENT 'Region code',
  `city` VARCHAR(64) DEFAULT NULL COMMENT 'City name',
  `status` TINYINT(4) NOT NULL DEFAULT 1 COMMENT 'Account status: 1=active, 2=suspended, 0=unverified',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User account table';

-- User basic profile information
DROP TABLE IF EXISTS `user_profile`;
CREATE TABLE `user_profile` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT(20) NOT NULL COMMENT 'Reference to user.id',
  `bio` VARCHAR(500) DEFAULT NULL COMMENT 'User biography',
  `gender` TINYINT(4) DEFAULT NULL COMMENT 'Gender: 0=female, 1=male, 2=other',
  `birthday` DATE DEFAULT NULL COMMENT 'Date of birth',
  `age` TINYINT(4) DEFAULT NULL COMMENT 'Age (calculated or manual)',
  `location` VARCHAR(128) DEFAULT NULL COMMENT 'User location',
  `interests` VARCHAR(512) DEFAULT NULL COMMENT 'User interests (comma-separated)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User extended profile information';

-- User health profile (weight, fitness goals, etc.)
DROP TABLE IF EXISTS `user_health_profile`;
CREATE TABLE `user_health_profile` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT(20) NOT NULL COMMENT 'Reference to user.id',
  `height_cm` INT(11) DEFAULT NULL COMMENT 'Height in centimeters',
  `initial_weight_kg` DECIMAL(5,2) DEFAULT NULL COMMENT 'Initial weight snapshot',
  `initial_weight_at` DATE DEFAULT NULL COMMENT 'Initial weight date',
  `latest_weight_kg` DECIMAL(5,2) DEFAULT NULL COMMENT 'Latest weight snapshot',
  `latest_weight_at` DATE DEFAULT NULL COMMENT 'Latest weight date',
  `target_weight_kg` DECIMAL(5,2) DEFAULT NULL COMMENT 'Target weight in kilograms',
  `target_deadline` DATE DEFAULT NULL COMMENT 'Target completion date',
  `diet_rule` TINYINT(4) DEFAULT NULL COMMENT 'Dietary preference',
  `activity_lvl` TINYINT(4) DEFAULT NULL COMMENT 'Activity level 1-5',
  `goal_water_intake_ml` INT(11) DEFAULT NULL COMMENT 'Daily water intake goal in milliliters',
  `quick_records_ml` VARCHAR(64) DEFAULT NULL COMMENT 'Quick record amounts (comma-separated, max 4)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User health profile and fitness goals';

-- User social statistics (cached data)
DROP TABLE IF EXISTS `user_social_stats`;
CREATE TABLE `user_social_stats` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT(20) NOT NULL COMMENT 'Reference to user.id',
  `follow_count` INT(11) NOT NULL DEFAULT 0 COMMENT 'Following count (cached)',
  `fans_count` INT(11) NOT NULL DEFAULT 0 COMMENT 'Fans/followers count (cached)',
  `post_count` INT(11) NOT NULL DEFAULT 0 COMMENT 'Total posts published (cached)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User social statistics cache';

-- ============================================
-- Authentication & Security Tables
-- ============================================

-- Refresh token storage for JWT
DROP TABLE IF EXISTS `auth_refresh_token`;
CREATE TABLE `auth_refresh_token` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT(20) NOT NULL COMMENT 'User ID',
  `device_id` VARCHAR(64) NOT NULL COMMENT 'Device identifier',
  `token_hash` CHAR(64) NOT NULL COMMENT 'Hashed refresh token (SHA-256)',
  `expires_at` DATETIME NOT NULL COMMENT 'Token expiration time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_device` (`user_id`, `device_id`),
  KEY `idx_token_hash` (`token_hash`),
  KEY `idx_expires` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Refresh token storage';

-- Email verification codes
DROP TABLE IF EXISTS `email_code`;
CREATE TABLE `email_code` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT(20) NOT NULL COMMENT 'User ID',
  `email` VARCHAR(255) NOT NULL COMMENT 'Email address',
  `purpose` ENUM('RESET_PASSWORD') NOT NULL COMMENT 'Code purpose',
  `code_hash` VARCHAR(64) NOT NULL COMMENT 'Hashed verification code',
  `expires_at` DATETIME NOT NULL COMMENT 'Code expiration time',
  `used_at` DATETIME DEFAULT NULL COMMENT 'Time when code was used',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_purpose` (`user_id`, `purpose`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Email verification codes';

-- ============================================
-- Social Relationship Tables
-- ============================================

-- User follow relationships
DROP TABLE IF EXISTS `user_follow`;
CREATE TABLE `user_follow` (
  `follower_id` BIGINT(20) NOT NULL COMMENT 'User who follows',
  `followed_id` BIGINT(20) NOT NULL COMMENT 'User being followed',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`follower_id`, `followed_id`),
  KEY `idx_followed` (`followed_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User follow relationships';

-- User block relationships
DROP TABLE IF EXISTS `user_block`;
CREATE TABLE `user_block` (
  `blocker_id` BIGINT(20) NOT NULL COMMENT 'User who blocks',
  `blocked_id` BIGINT(20) NOT NULL COMMENT 'User being blocked',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`blocker_id`, `blocked_id`),
  KEY `idx_blocked` (`blocked_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User block relationships';

-- ============================================
-- Post & Social Content Tables
-- ============================================

-- Posts/Content table
DROP TABLE IF EXISTS `posts`;
CREATE TABLE `posts` (
  `id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `author_id` BIGINT(20) UNSIGNED NOT NULL COMMENT 'Post author user ID',
  `title` VARCHAR(200) NOT NULL DEFAULT '' COMMENT 'Post title',
  `caption` TEXT COMMENT 'Post caption/content',
  `visibility` ENUM('public', 'followers', 'private') NOT NULL DEFAULT 'public' COMMENT 'Post visibility',
  `status` ENUM('draft', 'published', 'hidden', 'deleted') NOT NULL DEFAULT 'draft' COMMENT 'Post status',
  `media_count` TINYINT(3) UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Number of media attachments',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL COMMENT 'Soft delete timestamp',
  PRIMARY KEY (`id`),
  KEY `idx_posts_created` (`created_at`, `id`),
  KEY `idx_posts_author_created` (`author_id`, `created_at`, `id`),
  KEY `idx_posts_status_created` (`status`, `created_at`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='Posts and content';

-- Post statistics (separated for performance)
DROP TABLE IF EXISTS `post_statistics`;
CREATE TABLE `post_statistics` (
  `post_id` BIGINT(20) UNSIGNED NOT NULL COMMENT 'Reference to posts.id',
  `like_count` INT(10) UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Number of likes',
  `comment_count` INT(10) UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Number of comments',
  `bookmark_count` INT(10) UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Number of bookmarks',
  `heat_score` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Post heat score',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`post_id`),
  KEY `idx_stats_heat` (`heat_score`, `post_id`),
  CONSTRAINT `fk_stats_post` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='Post statistics cache';

-- Post media attachments
DROP TABLE IF EXISTS `post_media`;
CREATE TABLE `post_media` (
  `id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `post_id` BIGINT(20) UNSIGNED NOT NULL COMMENT 'Reference to posts.id',
  `media_type` ENUM('image') NOT NULL DEFAULT 'image' COMMENT 'Media type',
  `object_key` VARCHAR(512) DEFAULT NULL COMMENT 'Storage object key',
  `url` VARCHAR(1024) NOT NULL COMMENT 'Media URL',
  `width` INT(10) UNSIGNED DEFAULT NULL COMMENT 'Media width in pixels',
  `height` INT(10) UNSIGNED DEFAULT NULL COMMENT 'Media height in pixels',
  `sort_order` TINYINT(3) UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Display order',
  `blurhash` VARCHAR(128) DEFAULT NULL COMMENT 'BlurHash for image placeholder',
  `checksum` VARCHAR(64) DEFAULT NULL COMMENT 'File checksum',
  `bytes` INT(10) UNSIGNED DEFAULT NULL COMMENT 'File size in bytes',
  `mime_type` VARCHAR(64) DEFAULT NULL COMMENT 'MIME type',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_media_post_sort` (`post_id`, `sort_order`),
  KEY `idx_media_post_order` (`post_id`, `sort_order`),
  CONSTRAINT `fk_media_post` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='Post media attachments';

-- Post comments
DROP TABLE IF EXISTS `post_comment`;
CREATE TABLE `post_comment` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `root_id` BIGINT(20) DEFAULT NULL COMMENT 'Root comment ID (self for top-level)',
  `parent_id` BIGINT(20) DEFAULT NULL COMMENT 'Direct parent comment ID',
  `post_id` BIGINT(20) NOT NULL COMMENT 'Reference to posts.id',
  `author_id` BIGINT(20) NOT NULL COMMENT 'Comment author user ID',
  `content` VARCHAR(1000) NOT NULL COMMENT 'Comment text',
  `reply_count` INT(11) NOT NULL DEFAULT 0 COMMENT 'Number of direct replies',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL COMMENT 'Soft delete timestamp',
  PRIMARY KEY (`id`),
  KEY `idx_cmt_post` (`post_id`, `created_at`, `id`),
  KEY `idx_cmt_author` (`author_id`, `created_at`),
  KEY `idx_cmt_post_root` (`post_id`, `root_id`, `created_at`, `id`),
  KEY `idx_cmt_parent` (`parent_id`, `created_at`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Post comments';

-- Post likes
DROP TABLE IF EXISTS `post_like`;
CREATE TABLE `post_like` (
  `post_id` BIGINT(20) NOT NULL COMMENT 'Reference to posts.id',
  `user_id` BIGINT(20) NOT NULL COMMENT 'User who liked',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`post_id`, `user_id`),
  KEY `idx_like_user` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Post likes';

-- Post bookmarks
DROP TABLE IF EXISTS `post_bookmark`;
CREATE TABLE `post_bookmark` (
  `post_id` BIGINT(20) NOT NULL COMMENT 'Reference to posts.id',
  `user_id` BIGINT(20) NOT NULL COMMENT 'User who bookmarked',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`post_id`, `user_id`),
  KEY `idx_bm_user` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Post bookmarks';

-- ============================================
-- Tag System
-- ============================================

-- Tags table
DROP TABLE IF EXISTS `tags`;
CREATE TABLE `tags` (
  `id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL COMMENT 'Tag name (original case)',
  `name_lc` VARCHAR(64) NOT NULL COMMENT 'Tag name (lowercase for matching)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tags_name_lc` (`name_lc`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='Tags';

-- Post-tag relationships
DROP TABLE IF EXISTS `post_tags`;
CREATE TABLE `post_tags` (
  `post_id` BIGINT(20) UNSIGNED NOT NULL COMMENT 'Reference to posts.id',
  `tag_id` BIGINT(20) UNSIGNED NOT NULL COMMENT 'Reference to tags.id',
  PRIMARY KEY (`post_id`, `tag_id`),
  KEY `idx_pt_tag_post` (`tag_id`, `post_id`),
  CONSTRAINT `fk_pt_post` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pt_tag` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='Post-tag relationships';

-- ============================================
-- Food & Nutrition Tables
-- ============================================

-- User custom food items
DROP TABLE IF EXISTS `user_food_item`;
CREATE TABLE `user_food_item` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT(20) NOT NULL COMMENT 'User ID',
  `name` VARCHAR(128) NOT NULL COMMENT 'Food name',
  `unit_name` VARCHAR(32) NOT NULL DEFAULT 'unit' COMMENT 'Unit of measurement (default: unit)',
  `kcal_per_unit` INT(11) NOT NULL COMMENT 'Calories per unit',
  `image_url` VARCHAR(512) DEFAULT NULL COMMENT 'Food image URL from OSS',
  `enabled` TINYINT(4) NOT NULL DEFAULT 1 COMMENT 'Active status',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_food_name` (`user_id`, `name`),
  KEY `idx_user_food` (`user_id`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User custom food items';

-- Meal logging
DROP TABLE IF EXISTS `meal_log`;
CREATE TABLE `meal_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT(20) NOT NULL COMMENT 'User ID',
  `meal_date` DATE NOT NULL COMMENT 'Meal date',
  `meal_type` TINYINT(4) NOT NULL COMMENT 'Meal type: 0=breakfast, 1=lunch, 2=dinner, 3=snack',
  `source_id` BIGINT(20) DEFAULT NULL COMMENT 'User food item ID (NULL for free input)',
  `item_name` VARCHAR(128) NOT NULL COMMENT 'Food item name',
  `unit_name` VARCHAR(32) NOT NULL DEFAULT 'unit' COMMENT 'Unit of measurement (default: unit)',
  `unit_qty` DECIMAL(8,2) NOT NULL DEFAULT 1.00 COMMENT 'Quantity (default: 1)',
  `kcal` INT(11) NOT NULL COMMENT 'Total calories',
  `image_url` VARCHAR(512) DEFAULT NULL COMMENT 'Food image URL from OSS',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL COMMENT 'Soft delete timestamp',
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`, `meal_date`),
  KEY `idx_user_created` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Meal logs';

-- ============================================
-- Exercise & Activity Tables
-- ============================================

-- Exercise logging (simplified - no separate item tables)
DROP TABLE IF EXISTS `exercise_log`;
CREATE TABLE `exercise_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT(20) NOT NULL COMMENT 'User ID',
  `exercise_date` DATE NOT NULL COMMENT 'Exercise date',
  `exercise_name` VARCHAR(64) NOT NULL DEFAULT 'Running' COMMENT 'Exercise name',
  `minutes` INT(11) NOT NULL COMMENT 'Duration in minutes',
  `distance_km` DECIMAL(6,2) DEFAULT NULL COMMENT 'Distance in kilometers',
  `kcal` INT(11) NOT NULL COMMENT 'Total calories burned',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL COMMENT 'Soft delete timestamp',
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`, `exercise_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Exercise logs';

-- ============================================
-- Health Tracking Tables
-- ============================================

-- Weight logging
DROP TABLE IF EXISTS `user_weight_log`;
CREATE TABLE `user_weight_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT(20) NOT NULL COMMENT 'User ID',
  `weight_kg` DECIMAL(5,2) NOT NULL COMMENT 'Weight in kilograms',
  `recorded_at` DATE NOT NULL COMMENT 'Measurement date',
  `note` VARCHAR(500) DEFAULT NULL COMMENT 'Optional note',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`, `recorded_at`),
  KEY `idx_user_recorded` (`user_id`, `recorded_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Weight logs';

-- Water intake logging
DROP TABLE IF EXISTS `water_intake`;
CREATE TABLE `water_intake` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT(20) NOT NULL COMMENT 'User ID',
  `intake_date` DATE NOT NULL COMMENT 'Intake date',
  `amount_ml` INT(11) NOT NULL COMMENT 'Amount in milliliters',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`, `intake_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Water intake logs';

-- Step count tracking (with idempotency support)
DROP TABLE IF EXISTS `step_count`;
CREATE TABLE `step_count` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT(20) NOT NULL COMMENT 'User ID',
  `record_date` DATE NOT NULL COMMENT 'Record date',
  `steps` INT(11) NOT NULL DEFAULT 0 COMMENT 'Step count',
  `distance_km` DECIMAL(6,2) DEFAULT 0.00 COMMENT 'Distance in kilometers',
  `kcal` INT(11) DEFAULT 0 COMMENT 'Calories burned',
  `active_minutes` INT(11) DEFAULT 0 COMMENT 'Active minutes',
  `data_source` VARCHAR(20) DEFAULT 'Sensor' COMMENT 'Data source: Sensor, GoogleFit, HuaweiHealth, Manual',
  `sync_sequence` BIGINT(20) NOT NULL COMMENT 'Sync sequence number (client timestamp)',
  `version` INT(11) NOT NULL DEFAULT 1 COMMENT 'Optimistic locking version',
  `synced_at` DATETIME DEFAULT NULL COMMENT 'Last sync time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`, `record_date`),
  KEY `idx_user_updated` (`user_id`, `updated_at`),
  KEY `idx_sync_sequence` (`user_id`, `sync_sequence`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Step count tracking with idempotency';

-- ============================================
-- System & Analytics Tables
-- ============================================

-- User features snapshot (separated from content_exposure)
DROP TABLE IF EXISTS `user_features`;
CREATE TABLE `user_features` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT(20) NOT NULL COMMENT 'User ID',
  `age` TINYINT(4) DEFAULT NULL COMMENT 'Age',
  `region` VARCHAR(64) DEFAULT NULL COMMENT 'Permanent residence',
  `activity_lvl` TINYINT(4) DEFAULT NULL COMMENT 'Activity level',
  `interests` VARCHAR(256) DEFAULT NULL COMMENT 'Interest tags',
  `follow_count` INT(11) DEFAULT NULL COMMENT 'Following count',
  `snapshot_time` DATETIME NOT NULL COMMENT 'Feature snapshot timestamp',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_uf_user_time` (`user_id`, `snapshot_time`),
  KEY `idx_uf_snapshot_time` (`snapshot_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User features snapshot for analytics';

-- Content feature snapshot (separated from content_exposure)
DROP TABLE IF EXISTS `content_feature`;
CREATE TABLE `content_feature` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `post_id` BIGINT(20) UNSIGNED NOT NULL COMMENT 'Reference to posts.id',
  `author_id` BIGINT(20) UNSIGNED NOT NULL COMMENT 'Content author ID',
  `publish_time` DATETIME NOT NULL COMMENT 'Content publish time',
  `heat_score` DECIMAL(10,2) DEFAULT NULL COMMENT 'Content heat score',
  `tag_ids` VARCHAR(256) DEFAULT NULL COMMENT 'Comma-separated tag IDs',
  `snapshot_time` DATETIME NOT NULL COMMENT 'Feature snapshot timestamp',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cf_post` (`post_id`),
  KEY `idx_cf_snapshot_time` (`snapshot_time`),
  CONSTRAINT `fk_cf_post` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Content features snapshot for analytics';

-- Content exposure tracking
DROP TABLE IF EXISTS `content_exposure`;
CREATE TABLE `content_exposure` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT(20) NOT NULL COMMENT 'User ID',
  `user_feature_id` BIGINT(20) DEFAULT NULL COMMENT 'Reference to user_features.id',
  `content_feature_id` BIGINT(20) DEFAULT NULL COMMENT 'Reference to content_feature.id',
  `content_id` VARCHAR(50) NOT NULL COMMENT 'Content identifier',
  `exposure_time` DATETIME NOT NULL COMMENT 'Exposure timestamp',
  `platform` VARCHAR(20) NOT NULL COMMENT 'Platform name',
  `weekday` TINYINT(4) DEFAULT NULL COMMENT 'Day of week (1-7)',
  `weather` VARCHAR(16) DEFAULT NULL COMMENT 'Weather condition',
  `device` VARCHAR(32) DEFAULT NULL COMMENT 'Device information',
  `city` VARCHAR(64) DEFAULT NULL COMMENT 'City name',
  PRIMARY KEY (`id`),
  KEY `idx_ce_user_time` (`user_id`, `exposure_time`),
  KEY `idx_ce_content` (`content_id`),
  KEY `idx_ce_city_time` (`city`, `exposure_time`),
  KEY `idx_ce_platform_wd` (`platform`, `weekday`),
  KEY `idx_ce_user_feature` (`user_feature_id`),
  KEY `idx_ce_content_feature` (`content_feature_id`),
  CONSTRAINT `fk_ce_user_feature` FOREIGN KEY (`user_feature_id`) REFERENCES `user_features` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ce_content_feature` FOREIGN KEY (`content_feature_id`) REFERENCES `content_feature` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Content exposure tracking for analytics';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- Test Data
-- ============================================
-- Permanent test user for development
-- Email: test@aura.dev
-- Password: Test123456
-- Note: Password hash is BCrypt encoded "Test123456"

INSERT INTO `user` (`id`, `email`, `password`, `nickname`, `avatar_url`, `status`, `created_at`, `updated_at`)
VALUES (1000, 'test@aura.dev', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Test User', NULL, 1, NOW(), NOW());

INSERT INTO `user_profile` (`user_id`, `bio`, `gender`, `created_at`, `updated_at`)
VALUES (1000, 'Permanent test user for development', 1, NOW(), NOW());

INSERT INTO `user_health_profile` (`user_id`, `height_cm`, `latest_weight_kg`, `created_at`, `updated_at`)
VALUES (1000, 170, 70.0, NOW(), NOW());

INSERT INTO `user_social_stats` (`user_id`, `follow_count`, `fans_count`, `post_count`, `created_at`, `updated_at`)
VALUES (1000, 0, 0, 0, NOW(), NOW());

