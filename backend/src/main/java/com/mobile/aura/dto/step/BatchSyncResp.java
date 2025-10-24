package com.mobile.aura.dto.step;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for batch sync operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSyncResp {

    /**
     * Number of successfully synced items
     */
    private Integer successCount;

    /**
     * Number of failed items
     */
    private Integer failedCount;

    /**
     * Detailed results for each item
     */
    private List<StepSyncResp> results;
}
