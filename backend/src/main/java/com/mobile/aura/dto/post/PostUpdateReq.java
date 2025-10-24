package com.mobile.aura.dto.post;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating an existing post.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostUpdateReq {

    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 5000, message = "Caption must not exceed 5000 characters")
    private String caption;

    @Size(max = 20, message = "Maximum 20 tags allowed")
    private List<String> tags;
}
