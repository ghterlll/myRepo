package com.mobile.aura.domain.health;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.dto.water.WaterDayResp;
import com.mobile.aura.support.BizException;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Water intake domain model (Aggregate Root).
 * Represents daily water intake records with business logic.
 */
@Data
@TableName("water_intake")
public class WaterIntake {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_AMOUNT_ML = 100_000;
    private static final int MIN_AMOUNT_ML = 0;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate intakeDate;
    private Integer amountMl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Factory method to create a new water intake record.
     *
     * @param userId user ID
     * @param date intake date
     * @param amountMl amount in milliliters
     * @return new WaterIntake instance
     */
    public static WaterIntake create(Long userId, LocalDate date, int amountMl) {
        validateAmount(amountMl);

        WaterIntake intake = new WaterIntake();
        intake.userId = userId;
        intake.intakeDate = date != null ? date : LocalDate.now();
        intake.amountMl = amountMl;
        return intake;
    }

    /**
     * Update the water intake amount (overwrites existing value).
     *
     * @param amountMl new amount in milliliters
     */
    public void updateAmount(int amountMl) {
        validateAmount(amountMl);
        this.amountMl = amountMl;
    }

    /**
     * Increment the water intake amount (adds to existing value).
     *
     * @param incrementMl amount to add in milliliters
     */
    public void incrementAmount(int incrementMl) {
        if (incrementMl < 1) {
            throw new BizException(CommonStatusEnum.INVALID_PARAM);
        }

        int newAmount = (this.amountMl != null ? this.amountMl : 0) + incrementMl;
        validateAmount(newAmount);
        this.amountMl = newAmount;
    }

    /**
     * Convert this entity to a response DTO.
     *
     * @return WaterDayResp DTO
     */
    public WaterDayResp toResponse() {
        return new WaterDayResp(
                DATE_FORMATTER.format(this.intakeDate),
                this.amountMl != null ? this.amountMl : 0
        );
    }

    /**
     * Build a range response from a map of water intake records.
     * Missing dates are filled with 0 ml.
     *
     * @param dataMap map of date to water intake records
     * @param from start date
     * @param to end date
     * @return list of WaterDayResp for the date range
     */
    public static List<WaterDayResp> toRangeResponse(Map<LocalDate, WaterIntake> dataMap,
                                                      LocalDate from,
                                                      LocalDate to) {
        List<WaterDayResp> items = new ArrayList<>();

        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            WaterIntake intake = dataMap.get(date);
            if (intake != null) {
                items.add(intake.toResponse());
            } else {
                items.add(new WaterDayResp(DATE_FORMATTER.format(date), 0));
            }
        }

        return items;
    }

    /**
     * Normalize date range (swap if from > to, set defaults).
     *
     * @param from start date string
     * @param to end date string
     * @return array of [fromDate, toDate]
     */
    public static LocalDate[] normalizeDateRange(String from, String to) {
        LocalDate fromDate = (from == null || from.isBlank())
                ? LocalDate.now().minusDays(6)
                : LocalDate.parse(from, DATE_FORMATTER);

        LocalDate toDate = (to == null || to.isBlank())
                ? LocalDate.now()
                : LocalDate.parse(to, DATE_FORMATTER);

        if (toDate.isBefore(fromDate)) {
            LocalDate temp = fromDate;
            fromDate = toDate;
            toDate = temp;
        }

        return new LocalDate[]{fromDate, toDate};
    }

    /**
     * Validate water intake amount is within acceptable range.
     *
     * @param amountMl amount in milliliters
     * @throws BizException if amount is invalid
     */
    private static void validateAmount(int amountMl) {
        if (amountMl < MIN_AMOUNT_ML || amountMl > MAX_AMOUNT_ML) {
            throw new BizException(CommonStatusEnum.INVALID_PARAM);
        }
    }
}
