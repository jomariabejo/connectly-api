package com.jomariabejo.connectly_api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FollowResponseDto {
    private Long id;
    private Long followerId;
    private Long followingId;
    private Boolean approved;
    private String requestStatus;
}
