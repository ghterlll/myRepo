package com.mobile.aura.dto.tag;

import com.mobile.aura.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request DTO for listing posts by tag with pagination.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TagPostsReq extends PageRequest {
}
