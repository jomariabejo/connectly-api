package com.jomariabejo.connectly_api.mapper;

import com.jomariabejo.connectly_api.dto.comment.CommentResponseDto;
import com.jomariabejo.connectly_api.dto.comment.CreateCommentDto;
import com.jomariabejo.connectly_api.dto.user.UserResponseDto;
import com.jomariabejo.connectly_api.model.Comment;
import com.jomariabejo.connectly_api.model.Post;
import com.jomariabejo.connectly_api.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    Comment commentDtoTocomment(CreateCommentDto createCommentDto);

    CommentResponseDto commentToCommentResponseDto(Comment comment);

    CreateCommentDto commentToCreateCommentDto(Comment comment);

    /**
     * Maps the comment's author to its safe projection.
     *
     * <p>Without this MapStruct tries to build {@link UserResponseDto} field by field and fails on
     * {@code Set<Role> -> Set<String>}. Delegating to the constructor also keeps one definition of
     * which user fields are safe to expose.
     */
    default UserResponseDto userToUserResponseDto(User user) {
        return user == null ? null : UserResponseDto.from(user);
    }
}
