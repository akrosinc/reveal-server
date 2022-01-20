package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.UserBulkResponse;
import com.revealprecision.revealserver.persistence.domain.UserBulk;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserBulkResponseFactory {

  public static UserBulkResponse fromEntity(UserBulk userBulk) {
    return UserBulkResponse.builder()
        .filename(userBulk.getFilename())
        .identifier(userBulk.getIdentifier())
        .uploadDatetime(userBulk.getUploadedDatetime())
        .status(userBulk.getStatus())
        .uploadedBy(userBulk.getUploadedBy())
        .build();
  }

  public static Page<UserBulkResponse> fromEntityPage(Page<UserBulk> bulks, Pageable pageable) {
    var response = bulks.getContent().stream().map(UserBulkResponseFactory::fromEntity)
        .collect(Collectors.toList());
    return new PageImpl<>(response, pageable, bulks.getTotalElements());
  }
}
