package com.mobile.aura.domain.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.dto.SingleDateReq;
import com.mobile.aura.dto.weight.WeightPointResp;
import com.mobile.aura.dto.weight.WeightSubmitReq;
import com.mobile.aura.support.BizException;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * User weight log domain model (Aggregate Root).
 * Represents daily weight records with business logic.
 */
@Data
@TableName("user_weight_log")
public class UserWeightLog {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Double weightKg;
    private LocalDate recordedAt;
    private String note;
    private LocalDateTime createdAt;

    /**
     * Factory method to create a new weight log record.
     *
     * @param userId user ID
     * @param req weight submission request
     * @return new UserWeightLog instance
     */
    public static UserWeightLog create(Long userId, WeightSubmitReq req) {
        LocalDate date = SingleDateReq.parseDateOrToday(req.getDate());
        validateWeight(req.getWeightKg());

        UserWeightLog log = new UserWeightLog();
        log.userId = userId;
        log.recordedAt = date;
        log.weightKg = req.getWeightKg();
        log.note = req.getNote();
        log.createdAt = LocalDateTime.now();
        return log;
    }

    /**
     * Update weight and note from request.
     *
     * @param req weight submission request
     */
    public void update(WeightSubmitReq req) {
        validateWeight(req.getWeightKg());
        this.weightKg = req.getWeightKg();
        this.note = req.getNote();
    }

    /**
     * Convert to response DTO.
     *
     * @return WeightPointResp
     */
    public WeightPointResp toResponse() {
        return new WeightPointResp(
                this.recordedAt.format(DATE_FORMATTER),
                this.weightKg
        );
    }

    /**
     * Validate weight value.
     *
     * @param weight weight in kilograms
     */
    private static void validateWeight(Double weight) {
        if (weight == null || weight <= 0) {
            throw new BizException(CommonStatusEnum.INVALID_PARAM);
        }
    }
}
