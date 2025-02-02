package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.response.DataExtractQueryResponse;
import com.revealprecision.revealserver.service.DataExtractService;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/data-extract")
public class DataExtractController {

  private final DataExtractService dataExtractService;

  @GetMapping("/queryLabels/{planIdentifier}")
  public ResponseEntity<List<DataExtractQueryResponse>> queryLabels(
      @PathVariable("planIdentifier") UUID planIdentifier) {
    List<DataExtractQueryResponse> queryLabels = dataExtractService.getQueryLabels(planIdentifier);
    return ResponseEntity.status(HttpStatus.OK).body(queryLabels);
  }

  @GetMapping("/extract/{planIdentifier}/{queryLabel}")
  public ResponseEntity<Resource> data(
       @PathVariable("planIdentifier") UUID planIdentifier,
      @PathVariable("queryLabel") String queryLabel) throws IOException {

    InputStreamResource resource = dataExtractService.extract(planIdentifier,queryLabel);

    return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header("Content-disposition", "attachment;filename=" + "trev.csv").body(resource);
  }


  @GetMapping("/code/{planIdentifier}/{queryLabel}")
  public ResponseEntity<String> code(
      @PathVariable("planIdentifier") UUID planIdentifier,
      @PathVariable("queryLabel") String queryLabel) throws IOException {

    String code = dataExtractService.getCode(planIdentifier, queryLabel);
    return ResponseEntity.ok(code);
  }

}
