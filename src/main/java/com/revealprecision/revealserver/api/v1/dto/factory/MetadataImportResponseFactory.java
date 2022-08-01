package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.MetadataFileImportResponse;
import com.revealprecision.revealserver.api.v1.dto.response.UserBulkResponse;
import com.revealprecision.revealserver.persistence.domain.MetadataImport;
import com.revealprecision.revealserver.persistence.domain.UserBulk;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MetadataImportResponseFactory {
  public static MetadataFileImportResponse fromEntity(MetadataImport metadataImport) {
    return MetadataFileImportResponse.builder()
        .filename(metadataImport.getFilename())
        .identifier(metadataImport.getIdentifier())
        .uploadDatetime(metadataImport.getUploadedDatetime())
        .status(metadataImport.getStatus())
        .uploadedBy(metadataImport.getUploadedBy())
        .build();
  }

  public static Page<MetadataFileImportResponse> fromEntityPage(Page<MetadataImport> metadataImports, Pageable pageable) {
    var response = metadataImports.getContent().stream().map(MetadataImportResponseFactory::fromEntity)
        .collect(Collectors.toList());
    return new PageImpl<>(response, pageable, metadataImports.getTotalElements());
  }
}
