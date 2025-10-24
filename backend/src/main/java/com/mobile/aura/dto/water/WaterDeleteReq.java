package com.mobile.aura.dto.water;

import com.mobile.aura.dto.SingleDateReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request DTO for deleting water intake record.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WaterDeleteReq extends SingleDateReq {
    // Date field inherited from SingleDateReq
}
