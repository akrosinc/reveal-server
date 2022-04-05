package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.enums.ActionTitleEnum;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/action")
public class ActionController {

  @GetMapping("/actionTitles")
  public ResponseEntity<List<String>> getAllActionTitles() {
    return ResponseEntity.ok(
        Stream.of(ActionTitleEnum.values()).map(ActionTitleEnum::getActionTitle)
            .collect(Collectors.toList()));
  }
}
