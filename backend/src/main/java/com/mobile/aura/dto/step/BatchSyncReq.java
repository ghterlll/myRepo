package com.mobile.aura.dto.step;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for batch syncing step counts.
 * Used for syncing historical data or compensating failed syncs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchSyncReq {

    /**
     * List of step sync requests
     * Maximum 30 items per batch
     */
    @NotEmpty(message = "Items cannot be empty")
    @Size(max = 30, message = "Maximum 30 items per batch")
    @Valid
    private List<StepSyncReq> items;
}
