package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.request.InstanceRequest;
import com.revealprecision.revealserver.api.v1.dto.response.InstanceResponse;
import com.revealprecision.revealserver.service.InstanceService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
  public void create(@RequestBody final InstanceRequest instanceRequest) {
    instanceService.create(instanceRequest);
  }

  @GetMapping
  public Page<InstanceResponse> get(@RequestParam("") String searchParam, Pageable pageable) {
    return instanceService.searchInstance(searchParam, pageable);
  }

  @GetMapping("/{identifier}")
  public InstanceResponse getById(@PathVariable UUID identifier) {
    return instanceService.getInstanceResponse(identifier);
  }

  @PutMapping("/{identifier}")
  public void update(@PathVariable UUID identifier,
      @RequestBody final InstanceRequest instanceRequest) {
    instanceService.update(identifier, instanceRequest);
  }
}
