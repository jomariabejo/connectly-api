package com.jomariabejo.connectly_api.dto.post_like;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LikeDto {
    private Long id;
    private Long postId;
    private LocalDateTime createdAt;

    public LikeDto(Long id, Long postId, LocalDateTime createdAt) {
        this.id = id;
        this.postId = postId;
        this.createdAt = createdAt;
    }

}
