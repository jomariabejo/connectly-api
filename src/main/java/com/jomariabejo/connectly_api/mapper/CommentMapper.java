package com.jomariabejo.connectly_api.mapper;

import com.jomariabejo.connectly_api.dto.comment.CommentResponseDto;
import com.jomariabejo.connectly_api.dto.comment.CreateCommentDto;
import com.jomariabejo.connectly_api.model.Comment;
import com.jomariabejo.connectly_api.model.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    Comment commentDtoTocomment(CreateCommentDto createCommentDto);

    CommentResponseDto commentToCommentResponseDto(Comment comment);

    CreateCommentDto commentToCreateCommentDto(Comment comment);
}
