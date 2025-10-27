package com.mobile.aura.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a new post.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostCreateReq {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 5000, message = "Caption must not exceed 5000 characters")
    private String caption;

    private Boolean publish;

    @Size(max = 20, message = "Maximum 20 tags allowed")
    private List<String> tags;

    private List<MediaItem> medias;

    // Recommendation system fields (optional)
    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;  // Content category (defaults to "health" if not provided)

    private Double geoLat;    // Post location latitude
    private Double geoLon;    // Post location longitude
}
