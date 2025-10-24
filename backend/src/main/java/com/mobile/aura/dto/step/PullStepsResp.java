package com.mobile.aura.dto.step;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for pulling step count updates from server.
 * Used for bidirectional sync (client pulls server changes).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PullStepsResp {

    /**
     * Updated step records from server
     * Contains records that client doesn't have or are newer than client's version
     */
    private List<StepDayResp> updates;

    /**
     * Latest server timestamp (milliseconds)
     * Client should use this value in next pull request for incremental sync
     */
    private Long latestTimestamp;

    /**
     * Whether there are more records to pull
     * True if result set was limited by pagination
     */
    private Boolean hasMore;
}
