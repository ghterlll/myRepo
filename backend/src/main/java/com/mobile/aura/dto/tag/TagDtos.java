package com.mobile.aura.dto.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

public class TagDtos {

    @Data
    public static class TagCreateReq {
        @NotBlank(message = "Tag name is required")
        @Size(max = 64, message = "Tag name must not exceed 64 characters")
        private String name;
    }

    @Data
    public static class TagUpdateReq {
        @Size(max = 64, message = "Tag name must not exceed 64 characters")
        private String name;
    }

    @AllArgsConstructor
    @Getter
    public static class TagResp {
        private Long id;
        private String name;
    }

    @Data
    public static class PostTagReplaceReq {
        @Size(max = 20, message = "Cannot add more than 20 tags at once")
        private List<@NotBlank(message = "Tag name cannot be blank") @Size(max = 64) String> names;

        @Size(max = 20, message = "Cannot add more than 20 tags at once")
        private List<@Positive(message = "Tag ID must be positive") Long> tagIds;
    }
}
