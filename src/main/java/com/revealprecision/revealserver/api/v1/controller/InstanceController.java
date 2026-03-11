package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.request.InstanceRequest;
import com.revealprecision.revealserver.api.v1.dto.response.IdentifierNameResponse;
import com.revealprecision.revealserver.api.v1.dto.response.InstanceResponse;
import com.revealprecision.revealserver.api.v1.dto.response.InstanceUserListResponse;
import com.revealprecision.revealserver.persistence.projection.InstanceListProjection;
import com.revealprecision.revealserver.service.InstanceService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/instance")
@RequiredArgsConstructor
public class InstanceController {

  private final InstanceService instanceService;

  @PostMapping
  public ResponseEntity<Void> create(@RequestBody final InstanceRequest instanceRequest) {
    instanceService.create(instanceRequest);
    return ResponseEntity.ok().build();
  }

  @GetMapping
  public ResponseEntity<Page<InstanceListProjection>> get(@RequestParam(value = "",required = false) String searchParam, Pageable pageable) {
    return ResponseEntity.ok(instanceService.searchInstance(searchParam, pageable));
  }

  @GetMapping("/{identifier}")
  public InstanceResponse getById(@PathVariable UUID identifier) {
    return instanceService.getInstanceResponse(identifier);
  }

  @GetMapping("/context")
  public ResponseEntity<IdentifierNameResponse> instanceContext(@RequestParam(value = "identifier",required = false) UUID identifier) {
    return ResponseEntity.ok(instanceService.instanceContext(identifier));
  }

  @PutMapping("/{identifier}")
  public void update(@PathVariable UUID identifier,
      @RequestBody final InstanceRequest instanceRequest) {
    instanceService.update(identifier, instanceRequest);
  }

  @GetMapping("/user/instancelist")
  public ResponseEntity<List<InstanceUserListResponse>> getUsersInstances() {
    return ResponseEntity.status(HttpStatus.OK).body(instanceService.getUsersInstances());
  }

  @GetMapping("/assigned/user/list")
  public ResponseEntity<List<IdentifierNameResponse>> getAssignedInstanceUsers() {
    return ResponseEntity.status(HttpStatus.OK).body(instanceService.getAssignedInstanceUsers());
  }

  @GetMapping("/assigned/area/list")
  public ResponseEntity<List<IdentifierNameResponse>> getAssignedInstanceAreas() {
    return ResponseEntity.status(HttpStatus.OK).body(instanceService.getAssignedInstanceAreas());
  }
  @GetMapping("/assigned/dataset/list")
  public ResponseEntity<List<IdentifierNameResponse>> getAssignedInstanceDatasets() {
    return ResponseEntity.status(HttpStatus.OK).body(instanceService.getAssignedInstanceDatasets());
  }

  @PostMapping("/instances/{instanceId}/select")
  public  ResponseEntity<IdentifierNameResponse> selectInstance(@PathVariable UUID instanceId) {
    return ResponseEntity.ok(instanceService.instanceContext(instanceId));
  }
}
