package com.mobile.aura.dto.post;

import com.mobile.aura.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request DTO for listing posts with pagination.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostListReq extends PageRequest {
}
