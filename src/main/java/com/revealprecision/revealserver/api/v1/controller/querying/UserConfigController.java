package com.revealprecision.revealserver.api.v1.controller.querying;

import com.revealprecision.revealserver.api.v1.dto.request.UserConfigResponse;
import com.revealprecision.revealserver.persistence.domain.UserConfig;
import com.revealprecision.revealserver.service.UserConfigService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/rest/dbpull")
public class UserConfigController {

  private final UserConfigService userConfigService;

  @GetMapping(value = "/sync")
  private ResponseEntity<List<UserConfigResponse>> getConfig(){
    return ResponseEntity.ok(userConfigService.getAll().stream().map(userConfig ->
            UserConfigResponse.builder()
                .identifier(userConfig.getId())
                .userName(userConfig.getUsername())
                .build()
        ).collect(Collectors.toList()));
  }

  @PutMapping("/add/{username}")
  private ResponseEntity<UserConfigResponse> save(@PathVariable("username") String username){
    UserConfig userConfig = userConfigService.create(username);
    return ResponseEntity.ok(UserConfigResponse.builder()
            .userName(userConfig.getUsername())
            .identifier(userConfig.getId())
        .build());
  }

  @PostMapping("/update/{username}")
  private ResponseEntity<UserConfigResponse> save(@PathVariable("username") String username, @RequestParam("file") MultipartFile file){
    UserConfig userConfig = userConfigService.update(username,file);
    return ResponseEntity.ok(UserConfigResponse.builder()
        .userName(userConfig.getUsername())
        .identifier(userConfig.getId())
        .build());
  }


}
