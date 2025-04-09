package com.jomariabejo.connectly_api.mapper;

import com.jomariabejo.connectly_api.dto.post.CreatePostDto;
import com.jomariabejo.connectly_api.dto.post.PostResponseDto;
import com.jomariabejo.connectly_api.model.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PostMapper {

    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    Post postDtoToPost(CreatePostDto createPostDto);

    PostResponseDto postToPostResponseDto(Post post);

    CreatePostDto postToCreatePostDto(Post post);
}
