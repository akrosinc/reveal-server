package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.LocationBulkResponse;
import com.revealprecision.revealserver.persistence.domain.LocationBulk;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LocationBulkResponseFactory {

  public static Page<LocationBulkResponse> fromEntityPage(Page<LocationBulk> locationBulks,
      Pageable pageable) {
    var locationBulksContent = locationBulks.getContent().stream()
        .map(LocationBulkResponseFactory::fromEntity).collect(Collectors.toList());
    return new PageImpl<>(locationBulksContent, pageable,
        locationBulks.getTotalElements());
  }


  public static LocationBulkResponse fromEntity(LocationBulk locationBulk) {
    return LocationBulkResponse.builder().identifier(locationBulk.getIdentifier())
        .status(locationBulk.getStatus()).filename(locationBulk.getFilename())
        .uploadDatetime(locationBulk.getUploadedDatetime()).build();
  }

}
