package com.jomariabejo.connectly_api.dto.comment;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCommentDto {
    @NotBlank(message = "Comment is required")
    private String text;
}
