package com.jomariabejo.connectly_api.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "A new post. The author is taken from the bearer token, never from the body.")
public class CreatePostDto {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 100, message = "Title must be between 5 and 100 characters")
    @Schema(example = "My First Post", minLength = 5, maxLength = 100, requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "Content is required")
    @Schema(example = "This is my first post", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "Constrained by a CHECK on the post table", example = "text", allowableValues = {"text", "image", "video"})
    private String postType;

    @Schema(description = "Constrained by a CHECK on the post table; defaults to public", example = "public", allowableValues = {"public", "private"})
    private String privacy;

    @Schema(description = "Free-form JSON stored in the post.metadata JSONB column", example = "{}")
    private String metadata;
}
