package com.mobile.aura.dto.post;

import com.mobile.aura.dto.PageRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request DTO for listing comments with pagination.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommentListReq extends PageRequest {

    @Min(value = 0, message = "Preview size must be at least 0")
    @Max(value = 10, message = "Preview size must not exceed 10")
    private Integer previewSize = 3;
}
