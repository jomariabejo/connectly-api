package com.jomariabejo.connectly_api.dto;

public class CommentFilterDto {
    private String content;
    private Long createdById;
    private Long postId;

    public CommentFilterDto() {}

    public CommentFilterDto(String content, Long createdById, Long postId) {
        this.content = content;
        this.createdById = createdById;
        this.postId = postId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Long createdById) {
        this.createdById = createdById;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }
}
