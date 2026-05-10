package com.jomariabejo.connectly_api.dto;

public class PostFilterDto {
    private String title;
    private String content;
    private String postType;
    private String privacy;
    private Long createdById;

    public PostFilterDto() {}

    public PostFilterDto(String title, String content, String postType, String privacy, Long createdById) {
        this.title = title;
        this.content = content;
        this.postType = postType;
        this.privacy = privacy;
        this.createdById = createdById;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPostType() {
        return postType;
    }

    public void setPostType(String postType) {
        this.postType = postType;
    }

    public String getPrivacy() {
        return privacy;
    }

    public void setPrivacy(String privacy) {
        this.privacy = privacy;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Long createdById) {
        this.createdById = createdById;
    }
}
