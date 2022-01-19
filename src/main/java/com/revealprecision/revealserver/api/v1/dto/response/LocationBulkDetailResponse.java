package com.revealprecision.revealserver.api.v1.dto.response;

import com.revealprecision.revealserver.enums.BulkEntryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationBulkDetailResponse {
    private String name;
    private BulkEntryStatus status;
}
