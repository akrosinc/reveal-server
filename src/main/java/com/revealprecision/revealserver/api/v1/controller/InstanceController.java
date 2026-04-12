package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.InstanceResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.LocationHierarchyResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.PlanResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.GlobalUserRequest;
import com.revealprecision.revealserver.api.v1.dto.request.InstanceRequest;
import com.revealprecision.revealserver.api.v1.dto.response.ComplexTagDto;
import com.revealprecision.revealserver.api.v1.dto.response.ComplexTagResponse;
import com.revealprecision.revealserver.api.v1.dto.response.CountResponse;
import com.revealprecision.revealserver.api.v1.dto.response.GeoTreeResponse;
import com.revealprecision.revealserver.api.v1.dto.response.IdentifierNameResponse;
import com.revealprecision.revealserver.api.v1.dto.response.InstanceContextResponse;
import com.revealprecision.revealserver.api.v1.dto.response.InstanceResponse;
import com.revealprecision.revealserver.api.v1.dto.response.InstanceUserListResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationHierarchyResponse;
import com.revealprecision.revealserver.api.v1.dto.response.PlanResponse;
import com.revealprecision.revealserver.api.v1.dto.response.UserRolesResponse;
import com.revealprecision.revealserver.config.InstanceContext;
import com.revealprecision.revealserver.enums.SummaryEnum;
import com.revealprecision.revealserver.persistence.projection.InstanceListProjection;
import com.revealprecision.revealserver.service.InstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

  @Operation(summary = "Create Instances",
      description = "Create Instances",
      tags = {"Instance"}
  )
  @PostMapping
  public ResponseEntity<Void> create(@RequestBody final InstanceRequest instanceRequest) {
    instanceService.create(instanceRequest);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @Operation(summary = "Fetch Instances",
      description = "Fetch Instances",
      tags = {"Instance"}
  )
  @GetMapping
  public ResponseEntity<Page<InstanceListProjection>> get(@RequestParam(value = "searchParam",required = false) String searchParam, Pageable pageable) {
    return ResponseEntity.ok(instanceService.searchInstance(searchParam, pageable));
  }

  @Operation(summary = "Fetch Instances",
      description = "Fetch Instances",
      tags = {"Instance"}
  )
  @GetMapping("/count")
  public ResponseEntity<CountResponse> getCount(@RequestParam(value = "",required = false) String searchParam) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(new CountResponse(instanceService.getCountFindAll(searchParam)));
  }

  @GetMapping("/{identifier}")
  public InstanceResponse getById(@PathVariable UUID identifier) {
    return instanceService.getInstanceResponse(identifier);
  }

  @GetMapping("/context")
  public ResponseEntity<InstanceContextResponse> instanceContext(@RequestParam(value = "identifier",required = false) UUID identifier) {
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

  @GetMapping("/user/{userId}/instancelist")
  public ResponseEntity<List<InstanceUserListResponse>> getInstancesByUserId(@PathVariable UUID userId) {
    return ResponseEntity.status(HttpStatus.OK).body(instanceService.getInstancesByUserId(userId));
  }

  @GetMapping("/assigned/user/list")
  public ResponseEntity<List<IdentifierNameResponse>> getAssignedInstanceUsers() {
    return ResponseEntity.status(HttpStatus.OK).body(instanceService.getAssignedInstanceUsers());
  }

  @GetMapping("/assigned/area/list")
  public ResponseEntity<List<IdentifierNameResponse>> getAssignedInstanceAreas() {
    return ResponseEntity.status(HttpStatus.OK).body(instanceService.getAssignedInstanceAreas());
  }

  @GetMapping("/assigned/area/tree")
  public ResponseEntity<List<GeoTreeResponse>> getAssignedInstanceAreasTree() {
    return ResponseEntity.status(HttpStatus.OK).body(instanceService.getAssignedInstanceAreasTree());
  }
  @GetMapping("/assigned/dataset/list")
  public ResponseEntity<List<IdentifierNameResponse>> getAssignedInstanceDatasets() {
    return ResponseEntity.status(HttpStatus.OK).body(instanceService.getAssignedInstanceDatasets());
  }

  @GetMapping("/assigned/complextags/list")
  public ResponseEntity<List<ComplexTagResponse>> getAssignedInstanceComplexTags() {
    return ResponseEntity.status(HttpStatus.OK).body(instanceService.getAssignedInstanceComplexTags());
  }

  @PostMapping("/instances/{instanceId}/select")
  public  ResponseEntity<InstanceContextResponse> selectInstance(@PathVariable UUID instanceId) {
    return ResponseEntity.ok(instanceService.instanceContext(instanceId));
  }

  @GetMapping("/user/{userId}/arealist")
  public ResponseEntity<List<GeoTreeResponse>> getLocationsByUserId(@PathVariable UUID userId) {
    return ResponseEntity.status(HttpStatus.OK).body(instanceService.getLocationsByUserId(userId));
  }

  @GetMapping("/user/{userId}/datalist")
  public ResponseEntity<List<IdentifierNameResponse>> getDatasetsByUserId(@PathVariable UUID userId) {
    return ResponseEntity.status(HttpStatus.OK).body(instanceService.getDatasetsByUserId(userId));
  }

  @GetMapping("/user/{userId}/roles")
  public ResponseEntity<UserRolesResponse> getRolesByUserId(@PathVariable UUID userId) {
    return ResponseEntity.status(HttpStatus.OK).body(instanceService.getRolesByUserId(userId));
  }

  @GetMapping("/hierarchy")
  public ResponseEntity<LocationHierarchyResponse> getInstanceHierarchy(@RequestParam(value = "instanceIdentifier", required = false) UUID instanceIdentifier) {
    return ResponseEntity.status(HttpStatus.OK).body(instanceService.getInstanceHierarchyTreeResponse(instanceIdentifier));
  }

  @PostMapping("/user")
  public ResponseEntity<Void> addUser(@RequestBody final GlobalUserRequest globalUserRequest) {
    instanceService.addUser(globalUserRequest);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @PostMapping("/{instanceIdentifier}/plan/activate")
  public ResponseEntity<Void> activateInstancePlan(@PathVariable UUID instanceIdentifier) {
    instanceService.activateInstancePlan(instanceIdentifier);
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  @GetMapping("/hierarchy/withgroups")
  public ResponseEntity<LocationHierarchyResponse> getInstanceHierarchyWithGroups(@RequestParam(value = "instanceIdentifier", required = false) UUID instanceIdentifier) {
    return ResponseEntity.status(HttpStatus.OK).body(instanceService.getInstanceHierarchyWithGroups(instanceIdentifier));
  }

  @GetMapping("/reports")
  public ResponseEntity<Page<InstanceListProjection>> getInstancesForReports(@RequestParam(name = "reportType", defaultValue = "") String reportType,
      Pageable pageable ) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(instanceService.getInstanceForReports(reportType, pageable));
  }

  @GetMapping("/tags/complex")
  public ResponseEntity<Page<ComplexTagResponse>> getComplexTags(Pageable pageable,
      @RequestParam(value = "isPublic" , required = false) Boolean isPublic,
      @RequestParam(value = "hierarchyIdentifier" , required = false) UUID hierarchyIdentifier) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(instanceService.getComplexTags(isPublic, hierarchyIdentifier, pageable));
  }
}
