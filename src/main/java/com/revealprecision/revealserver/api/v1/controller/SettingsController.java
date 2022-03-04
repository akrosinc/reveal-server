package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.SettingConfigurationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.SettingResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.SettingRequest;
import com.revealprecision.revealserver.api.v1.dto.response.SettingConfigurationResponse;
import com.revealprecision.revealserver.api.v1.dto.response.SettingResponse;
import com.revealprecision.revealserver.api.v1.facade.service.SettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

  private final SettingService settingService;

  @Operation(summary = "Fetch Settings Configuration by specifying settingsIdentifier", description = "Fetch Settings Configuration by specifying settingsTypeIdentifier, to support mobile application the search parameter is call identifier (don't be confused)", tags = {
      "Setting"})
  @GetMapping(value = "/sync", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SettingConfigurationResponse> getSettings(
      @Parameter(description = "used to filter the type of configs to fetch") @RequestParam(name = "identifier", defaultValue = "global_configs") String identifier,
      @Parameter(description = "Required by previous implementation, currently not used") @RequestParam(name = "serverVersion") String serverVersion) {
    return ResponseEntity.status(HttpStatus.OK).body(SettingConfigurationResponseFactory
        .fromSettingsAndSettingTypeIdentifier(
            settingService.findExistingSettingsByTypeIdentifier(identifier), identifier));
  }

  @Operation(summary = "Create a setting by supplying key and other required details", description = "Create a setting by supplying key and other required details", tags = {
      "Setting"})
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SettingResponse> createSetting(@RequestBody SettingRequest settingRequest) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(SettingResponseFactory.fromEntity(settingService.createSetting(settingRequest)));
  }

  @Operation(summary = "Update setting values", description = "Update setting values", tags = {
      "Settings"})
  @PutMapping(value = "/{identifier}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SettingResponse> updateSetting(@PathVariable UUID identifier,
      @RequestBody SettingRequest settingRequest) {
    return ResponseEntity.status(HttpStatus.OK).body(SettingResponseFactory
        .fromEntity(settingService.updateSetting(identifier, settingRequest)));
  }

}
