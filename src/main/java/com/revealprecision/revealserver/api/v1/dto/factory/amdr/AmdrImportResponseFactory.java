package com.revealprecision.revealserver.api.v1.dto.factory.amdr;

import com.revealprecision.revealserver.amdr.api.v1.dto.response.AmdrImportResponse;
import com.revealprecision.revealserver.amdr.persistence.domain.AmdrImport;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AmdrImportResponseFactory {

  public static AmdrImportResponse fromEntity(AmdrImport metadataImport){


    AmdrImportResponse build = AmdrImportResponse.builder()
        .filename(metadataImport.getFilename())
        .identifier(metadataImport.getIdentifier())
        .uploadDatetime(metadataImport.getUploadedDatetime())
        .status(metadataImport.getStatus())
        .uploadedBy(metadataImport.getUploadedBy())
        .build();

    return build;
  }

  public static Page<AmdrImportResponse> fromEntityPage(
      Page<AmdrImport> metadataImports,
      Pageable pageable) {
    var response = metadataImports.getContent().stream()
        .map(AmdrImportResponseFactory::fromEntity)
        .collect(Collectors.toList());
    return new PageImpl<>(response, pageable, metadataImports.getTotalElements());
  }
}
