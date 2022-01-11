package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.UserBulkDetailResponse;
import com.revealprecision.revealserver.enums.BulkEntryStatus;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.persistence.projection.UserBulkProjection;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserBulkDetailResponseFactory {

    public static UserBulkDetailResponse fromProjection(UserBulkProjection projection) {
        String message;
        BulkEntryStatus status;
        if (projection.getEntityStatus() == null) {
            status = BulkEntryStatus.ERROR;
            message = projection.getMessage();
        } else {
            if (projection.getEntityStatus() == EntityStatus.ACTIVE) {
                status = BulkEntryStatus.SUCCESSFUL;
                message = null;
            } else {
                message = projection.getMessage();
                status = BulkEntryStatus.ERROR;
            }
        }
        return UserBulkDetailResponse.builder()
                .message(message)
                .username(projection.getUsername())
                .status(status)
                .build();
    }

    public static Page<UserBulkDetailResponse> fromProjectionPage(Page<UserBulkProjection> entries, Pageable pageable) {
        var response = entries.getContent()
                .stream()
                .map(UserBulkDetailResponseFactory::fromProjection).collect(Collectors.toList());
        return new PageImpl<>(response, pageable, entries.getTotalElements());
    }
}
