package com.jomariabejo.connectly_api.mapper;

import com.jomariabejo.connectly_api.dto.post.CreatePostDto;
import com.jomariabejo.connectly_api.dto.post.PostResponseDto;
import com.jomariabejo.connectly_api.model.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PostMapper {

    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "privacy", expression = "java(com.jomariabejo.connectly_api.model.PrivacyLevel.fromString(createPostDto.getPrivacy()))")
    Post postDtoToPost(CreatePostDto createPostDto);

    @Mapping(target = "privacy", expression = "java(post.getPrivacy() != null ? post.getPrivacy().getValue() : \"public\")")
    PostResponseDto postToPostResponseDto(Post post);

    CreatePostDto postToCreatePostDto(Post post);
}
