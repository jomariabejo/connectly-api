package com.jomariabejo.connectly_api.dto.user;

import com.jomariabejo.connectly_api.model.UserSettings;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class UserSettingsResponseDto {
    private Long id;
    private Long userId;
    private Boolean autoApproveFollowers;
    private Boolean allowFollowing;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserSettingsResponseDto from(UserSettings settings) {
        if (settings == null) {
            return null;
        }

        UserSettingsResponseDto dto = new UserSettingsResponseDto();
        dto.setId(settings.getId());
        dto.setUserId(settings.getUser() != null ? settings.getUser().getId() : null);
        dto.setAutoApproveFollowers(settings.getAutoApproveFollowers());
        dto.setAllowFollowing(settings.getAllowFollowing());
        dto.setCreatedAt(settings.getCreatedAt());
        dto.setUpdatedAt(settings.getUpdatedAt());
        return dto;
    }
}
