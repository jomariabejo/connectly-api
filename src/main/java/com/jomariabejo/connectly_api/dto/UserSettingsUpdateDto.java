package com.jomariabejo.connectly_api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserSettingsUpdateDto {

    private Boolean privateAccount;
    private Boolean autoApproveFollowers;
    private Boolean allowFollowing;
    private Boolean autoReactivationEnabled;
}
