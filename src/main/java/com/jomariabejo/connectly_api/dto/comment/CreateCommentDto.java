package com.jomariabejo.connectly_api.dto.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "A new comment. The post comes from the {postId} path segment and the author from the bearer token.")
public class CreateCommentDto {
    @NotBlank(message = "Comment is required")
    @Schema(example = "Nice post!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String text;
}
