package com.revealprecision.revealserver.amdr.api.v1.controller;

import com.revealprecision.revealserver.amdr.api.v1.dto.response.AdmrImportResultsResponse;
import com.revealprecision.revealserver.amdr.api.v1.dto.response.AmdrImportResponse;
import com.revealprecision.revealserver.amdr.service.AmdrService;
import com.revealprecision.revealserver.exceptions.FileFormatException;
import com.revealprecision.revealserver.service.StorageService;
import java.io.IOException;
import java.util.UUID;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/amdr")
public class AdmrController {

  private final AmdrService amdrService;
  private final StorageService storageService;

  @GetMapping("/downloadAmdrImportTemplate")
  public ResponseEntity<?> downloadAllLocationsUpToGeoLevel()
      throws IOException {

    return ResponseEntity.status(HttpStatus.OK)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header("Content-disposition", "attachment;filename=Location.xlsx")
        .body(
            amdrService.downloadAmdrImportTemplate());
  }

  @GetMapping("/amdrKeys")
  public ResponseEntity<?> amdrKeys() {
    return ResponseEntity.status(HttpStatus.OK)
        .body(
            amdrService.getAmdrKeys());
  }

  @PostMapping(value = "/upload")
  public ResponseEntity<?> importAmdrData(
      @RequestParam("file") MultipartFile file) throws FileFormatException {
    try {
      String path = storageService.saveXlsx(file);
      amdrService.saveImportFile(path,
          file.getOriginalFilename());
    } catch (FileFormatException e){
      return ResponseEntity.badRequest().body(e.getMessage());
    }
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @PostMapping(value = "/uploadRaw")
  public ResponseEntity<?> importAmdrRawData(
      @RequestParam("file") MultipartFile file) throws FileFormatException, IOException {
    try {
      String path = storageService.saveXlsx(file);
      amdrService.saveImportRawFile(path,
          file.getOriginalFilename());
    } catch (FileFormatException e){
      return ResponseEntity.badRequest().body(e.getMessage());
    }
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @GetMapping(value = "/importResults")
  public AdmrImportResultsResponse importAmdrRawData(
      @RequestParam(value = "importId", required = false) @Nullable UUID importId) throws FileFormatException, IOException {
     return amdrService.getImportResults(importId);
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE,value = "/amdrImport")
  public ResponseEntity<Page<AmdrImportResponse>> getMetadataImportList(Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(amdrService.getAmdrImportList(pageable));
  }


}
