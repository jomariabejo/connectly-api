package com.jomariabejo.connectly_api.dto.post_like;

import com.jomariabejo.connectly_api.dto.user.UserResponseDto;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PostLikeResponseDto {
    private Long id;
    private UserResponseDto user;
    private Long postId;
    private LocalDateTime createdAt;
}
