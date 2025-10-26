package com.mobile.aura.dto.post;

import com.mobile.aura.dto.PageRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request DTO for searching posts with pagination.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostSearchReq extends PageRequest {

    /**
     * Search keyword (searches in title and caption).
     */
    @NotBlank(message = "Search keyword is required")
    private String keyword;

    /**
     * Category filter (optional).
     */
    private String category;
}
