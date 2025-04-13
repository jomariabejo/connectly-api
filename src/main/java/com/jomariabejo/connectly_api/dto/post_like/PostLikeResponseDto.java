package com.jomariabejo.connectly_api.dto.post_like;

import com.jomariabejo.connectly_api.model.Post;
import com.jomariabejo.connectly_api.model.User;

import java.time.LocalDateTime;

public class PostLikeResponseDto {
    private User user;
    private Post post;
    private LocalDateTime createdAt;
}
