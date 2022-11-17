package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.response.MetadataFileImportResponse;
import com.revealprecision.revealserver.exceptions.FileFormatException;
import com.revealprecision.revealserver.service.MetadataService;
import com.revealprecision.revealserver.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/metaImport")
public class MetaImportController {

  private final StorageService storageService;
  private final MetadataService metadataService;

  @Operation(summary = "Import users from xlsx",
      description = "Import users from xlsx file",
      tags = {"MetaData import"}
  )
  @PostMapping()
  public ResponseEntity<?> importMetaData(
      @RequestParam("file") MultipartFile file) throws FileFormatException, IOException {
    String path = storageService.saveXlsx(file);
    UUID importIdentifier = metadataService.saveImportFile(path, file.getOriginalFilename());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body("Meta data uploaded successfully with identifier: " + importIdentifier);
  }

  @Operation(summary = "Get List of Metadata Import files uploaded",
      description = "Get List of Metadata Import files",
      tags = {"Metadata import"})
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Page<MetadataFileImportResponse>> getMetadataImportList(Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(metadataService.getMetadataImportList(pageable));
  }

  @Operation(summary = "Get Metadata Import file details",
      description = "Get Metadata Import file details",
      tags = {"Metadata import"})
  @GetMapping("/{metaImportIdentifier}")
  public ResponseEntity<?> getMetadataImportByIdentifier(@PathVariable UUID metaImportIdentifier) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(metadataService.getMetadataImportDetails(metaImportIdentifier));
  }
}
