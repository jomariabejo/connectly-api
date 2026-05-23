package com.jomariabejo.connectly_api.mapper;

import com.jomariabejo.connectly_api.dto.PaginationDto;
import com.jomariabejo.connectly_api.dto.user.UserResponseDto;
import com.jomariabejo.connectly_api.model.User;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {
    public UserResponseDto toResponseDto(User user) {
        return UserResponseDto.from(user);
    }

    public List<UserResponseDto> toResponseDtos(List<User> users) {
        return users.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    public PaginationDto<UserResponseDto> toResponseDto(PaginationDto<User> users) {
        return new PaginationDto<>(
                toResponseDtos(users.getContent()),
                users.getPageNumber(),
                users.getPageSize(),
                users.getTotalElements(),
                users.getTotalPages()
        );
    }

    public Page<UserResponseDto> toResponseDto(Page<User> users) {
        return users.map(this::toResponseDto);
    }
}
