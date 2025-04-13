package com.jomariabejo.connectly_api.mapper;

import com.jomariabejo.connectly_api.dto.post_like.CreatePostLikeDto;
import com.jomariabejo.connectly_api.dto.post_like.PostLikeResponseDto;
import com.jomariabejo.connectly_api.model.Like;
import com.jomariabejo.connectly_api.model.Post;
import org.mapstruct.Mapping;

public interface PostLikeMapper {
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    Like postLikeDtoToLike(CreatePostLikeDto createPostLikeDto);

    PostLikeResponseDto postLikeToPostLikeResponseDto(Like like);

    CreatePostLikeDto postLikeToCreatePostLikeDto(Like like);

}
