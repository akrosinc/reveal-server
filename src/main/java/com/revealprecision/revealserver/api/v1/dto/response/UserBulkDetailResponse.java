package com.revealprecision.revealserver.api.v1.dto.response;

import com.revealprecision.revealserver.enums.BulkEntryStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBulkDetailResponse {

    private String username;
    private String message;
    private BulkEntryStatus status;
}
