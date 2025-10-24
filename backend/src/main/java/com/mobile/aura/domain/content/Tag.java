package com.mobile.aura.domain.content;

import com.baomidou.mybatisplus.annotation.*;
import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.dto.tag.TagDtos.TagResp;
import com.mobile.aura.support.BizException;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Locale;
import java.util.Optional;

/**
 * Tag domain entity representing a content classification tag.
 * Follows Rich Domain Model principles with immutable creation and business logic encapsulation.
 * Tags are case-insensitive and stored with normalized lowercase for efficient searching.
 */
@Data
@NoArgsConstructor
@TableName("tags")
public class Tag {

    // Setters required by MyBatis Plus for query results population
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField("name_lc")
    private String nameLc;

    /**
     * Private constructor for controlled instantiation through factory methods.
     *
     * @param name the display name of the tag
     * @param nameLc the lowercase normalized name for case-insensitive operations
     */
    private Tag(String name, String nameLc) {
        this.name = name;
        this.nameLc = nameLc;
    }

    /* --------------------- Factory Methods --------------------- */

    /**
     * Creates a new tag with the given name.
     * Normalizes the name by trimming whitespace and converting to lowercase for storage.
     *
     * @param name the tag name to create
     * @return a new Tag instance ready for persistence
     * @throws BizException if the name is blank after normalization
     */
    public static Tag create(String name) {
        String normalized = normalize(name);
        String lowerCase = normalized.toLowerCase(Locale.ROOT);

        return new Tag(normalized, lowerCase);
    }

    /* --------------------- Business Logic Methods --------------------- */

    /**
     * Updates the tag's name while maintaining lowercase normalization.
     * Validates that the new name is not blank.
     *
     * @param newName the new name for the tag
     * @throws BizException if the new name is blank after normalization
     */
    public void updateName(String newName) {
        String normalized = normalize(newName);

        // MyBatis Plus needs these for updateById
        this.name = normalized;
        this.nameLc = normalized.toLowerCase(Locale.ROOT);
    }

    /**
     * Static validation to ensure a tag exists.
     *
     * @param tag the tag to validate
     * @throws BizException if the tag is null
     */
    public static void ensureExists(Tag tag) {
        if (tag == null) {
            throw new BizException(CommonStatusEnum.NOT_FOUND);
        }
    }

    /**
     * Ensure tag was successfully deleted.
     *
     * @param deletedCount the number of rows deleted
     * @throws BizException if no rows were deleted (tag not found)
     */
    public static void ensureDeleted(int deletedCount) {
        Optional.of(deletedCount)
                .filter(c -> c == 0)
                .ifPresent(c -> {
                    throw new BizException(CommonStatusEnum.NOT_FOUND);
                });
    }

    /* --------------------- DTO Conversion --------------------- */

    /**
     * Converts this domain entity to a response DTO.
     *
     * @return TagResp containing the tag's ID and display name
     */
    public TagResp toTagResp() {
        return new TagResp(this.id, this.name);
    }

    /* --------------------- Utility Methods --------------------- */

    /**
     * Normalizes a tag name by trimming whitespace and validating it's not blank.
     *
     * @param name the name to normalize
     * @return the trimmed name
     * @throws BizException if the name is blank after trimming
     */
    private static String normalize(String name) {
        String trimmed = (name == null) ? "" : name.trim();
        if (trimmed.isBlank()) {
            throw new BizException(CommonStatusEnum.INVALID_PARAM);
        }
        return trimmed;
    }

    /**
     * Build a paginated tag response from query results.
     * Encapsulates pagination logic for tags using name-based cursor.
     *
     * @param tags query results (limit + 1 items)
     * @param limit the page size limit
     * @return PageResponse with tags, cursor, and pagination metadata
     */
    public static com.mobile.aura.dto.PageResponse<TagResp> toPageResponse(
            java.util.List<Tag> tags,
            int limit) {
        java.util.List<TagResp> items = tags.stream()
                .map(Tag::toTagResp)
                .toList();

        return com.mobile.aura.dto.PageResponse.paginate(
                items,
                limit,
                tagResp -> tagResp.getName().toLowerCase()
        );
    }

}
