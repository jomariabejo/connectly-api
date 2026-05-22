package com.jomariabejo.connectly_api.dto.post;


import com.jomariabejo.connectly_api.dto.user.UserResponseDto;
import com.jomariabejo.connectly_api.model.Post;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PostResponseDto {

    private Long id;

    private String title;

    private String content;

    private String postType;

    private String metadata;

    private String privacy;

    private LocalDateTime createdAt;

    private UserResponseDto createdBy;

    public PostResponseDto() {
    }

    public PostResponseDto(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.postType = post.getPostType();
        this.metadata = post.getMetadata();
        this.privacy = post.getPrivacy() != null ? post.getPrivacy().getValue() : "public";
        this.createdAt = post.getCreatedAt();
        this.createdBy = UserResponseDto.from(post.getCreatedBy());
    }

    @Override
    public String toString() {
        return "PostResponseDto{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", postType='" + postType + '\'' +
                ", metadata='" + metadata + '\'' +
                ", privacy='" + privacy + '\'' +
                ", createdAt=" + createdAt +
                ", createdBy=" + createdBy +
                '}';
    }
}
