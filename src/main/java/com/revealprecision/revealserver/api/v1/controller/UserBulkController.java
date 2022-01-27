package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.UserBulkDetailResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.UserBulkResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.response.IdentifierResponse;
import com.revealprecision.revealserver.api.v1.dto.response.UserBulkDetailResponse;
import com.revealprecision.revealserver.api.v1.dto.response.UserBulkResponse;
import com.revealprecision.revealserver.batch.runner.UserBatchRunner;
import com.revealprecision.revealserver.service.StorageService;
import com.revealprecision.revealserver.service.UserBulkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/user/bulk")
@CrossOrigin(originPatterns = "*", origins = "*")
public class UserBulkController {

  private final UserBulkService userBulkService;
  private final UserBatchRunner userBatchRunner;
  private final StorageService storageService;

  @Operation(summary = "Get bulk details",
      description = "Get bulk details",
      tags = {"User bulk"}
  )
  @GetMapping("/{identifier}")
  public ResponseEntity<Page<UserBulkDetailResponse>> getBulkDetails(
      @Parameter(description = "User bulk identifier") @PathVariable("identifier") UUID identifier,
      Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK).body(
        UserBulkDetailResponseFactory.fromProjectionPage(
            userBulkService.getUserBulkDetails(identifier, pageable), pageable));
  }

  @Operation(summary = "Get all user bulks",
      description = "Get all user bulks",
      tags = {"User bulk"}
  )
  @GetMapping()
  public ResponseEntity<Page<UserBulkResponse>> getUserBulks(Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK).body(
        UserBulkResponseFactory.fromEntityPage(userBulkService.getUserBulks(pageable), pageable));
  }

  @Operation(summary = "Import users from CSV",
      description = "Import users from CSV file",
      tags = {"User bulk"}
  )
  @PostMapping()
  public ResponseEntity<IdentifierResponse> importUsers(
      @RequestParam("file") MultipartFile file)
      throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {

    String path = storageService.saveCSV(file);
    UUID identifier = userBulkService.saveBulk(file.getOriginalFilename());
    userBatchRunner.run(identifier.toString(), path);
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(IdentifierResponse.builder().identifier(identifier).build());
  }

  @Operation(summary = "Download excel template for user import",
      description = "Download excel template for user import",
      tags = {"User bulk"}
  )
  @GetMapping("/csv")
  public ResponseEntity<?> downloadTemplate() throws IOException {
    return ResponseEntity.status(HttpStatus.OK)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header("Content-disposition", "attachment;filename=UserTemplate.xlsx")
        .body(storageService.downloadTemplate("UserTemplate.xlsx"));
  }

}
