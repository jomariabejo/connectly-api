package com.jomariabejo.connectly_api.dto.post;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePostDto {

    @Size(min = 5, max = 100, message = "Title must be between 5 and 100 characters")
    private String title;

    private String content;

    private String postType;

    private String privacy;

    private String metadata;
}
