package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.persistence.repository.LocationElastic;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/test/")
public class Test {

  private final LocationElastic locationElastic;
  private final LocationRepository locationRepository;
  private final RestHighLevelClient client;

  @GetMapping
  public ResponseEntity<?> test() throws IOException {

    return ResponseEntity.ok().build();
  }
}
