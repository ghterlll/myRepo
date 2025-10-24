package com.mobile.aura.dto.relation;

import com.mobile.aura.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request DTO for listing followers/followings with pagination.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RelationListReq extends PageRequest {
}
