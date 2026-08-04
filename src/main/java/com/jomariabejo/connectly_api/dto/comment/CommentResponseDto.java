package com.jomariabejo.connectly_api.dto.comment;

import com.jomariabejo.connectly_api.model.Comment;
import com.jomariabejo.connectly_api.dto.user.UserResponseDto;
import com.jomariabejo.connectly_api.model.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
public class CommentResponseDto {
    private Long id;
    private UserResponseDto user; // comment author, without credentials
    private String text;
    private LocalDateTime createdAt;

    public CommentResponseDto() {
    }

    public CommentResponseDto(Long id, User user, String text, LocalDateTime createdAt) {
        this.id = id;
        this.user = user == null ? null : UserResponseDto.from(user);
        this.text = text;
        this.createdAt = createdAt;
    }


    public CommentResponseDto(Comment comment) {
        this.id = comment.getId();
        this.user = comment.getUser() == null ? null : UserResponseDto.from(comment.getUser());
        this.text = comment.getText();
        this.createdAt = comment.getCreatedAt();
    }
}
