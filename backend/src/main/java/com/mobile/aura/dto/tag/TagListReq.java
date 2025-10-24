package com.mobile.aura.dto.tag;

import com.mobile.aura.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request DTO for listing tags with cursor-based pagination.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TagListReq extends PageRequest {
    /**
     * Search query string.
     */
    private String q;
}
