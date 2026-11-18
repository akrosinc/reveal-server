package com.revealprecision.revealserver.api.v1.facade.controller;

import com.revealprecision.revealserver.api.v1.facade.models.ActivityProgressDto;
import com.revealprecision.revealserver.api.v1.facade.request.TaskSyncRequest;
import com.revealprecision.revealserver.service.ActivityProgressService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/v2/activity-progress")
@Slf4j
@RequiredArgsConstructor
public class ActivityProgressFacadeController {

  public static final String TOTAL_RECORDS = "total_records";

  private final ActivityProgressService activityProgressService;

  @Operation(summary = "Facade for Android ActivityProgress Resource", description = "Push ActivityProgress rows (upsert by plan and location). Returns the accepted rows with their server versions.", tags = {
      "ActivityProgress-Facade"})
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(value = "/add", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<ActivityProgressDto>> add(
      @RequestBody List<ActivityProgressDto> activityProgresses) {
    return ResponseEntity.ok().body(activityProgressService.save(activityProgresses));
  }

  @Operation(summary = "Facade for Android ActivityProgress Resource", description = "Pull ActivityProgress rows scoped by the caller's selected plan assignments (including descendant locations). Uses the plan list and serverVersion of the request; 'group' is ignored.", tags = {
      "ActivityProgress-Facade"})
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(value = "/sync", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<ActivityProgressDto>> sync(
      @RequestBody TaskSyncRequest syncRequest) {

    List<ActivityProgressDto> rows = activityProgressService.sync(syncRequest.getPlan(),
        syncRequest.getServerVersion());

    if (syncRequest.isReturnCount()) {
      HttpHeaders headers = new HttpHeaders();
      headers.add(TOTAL_RECORDS, String.valueOf(rows.size()));
      return ResponseEntity.ok().headers(headers).body(rows);
    }
    return ResponseEntity.ok().body(rows);
  }
}
