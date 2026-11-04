package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.ActionLocationRecaptureLimit;
import com.revealprecision.revealserver.persistence.domain.ActionLocationRecaptureLimit.Fields;
import com.revealprecision.revealserver.persistence.domain.Instance;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.repository.ActionLocationRecaptureLimitRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ActionLocationRecaptureLimitService {

  private final ActionLocationRecaptureLimitRepository actionLocationRecaptureLimitRepository;
  private final InstanceService instanceService;
  private final LocationService locationService;
  private final ActionService actionService;

  public ActionLocationRecaptureLimit getByIdentifier(UUID identifier) {
    return actionLocationRecaptureLimitRepository.findById(identifier)
        .orElseThrow(() -> new NotFoundException(Pair.of(Fields.identifier, identifier),
            ActionLocationRecaptureLimit.class));
  }

  public List<ActionLocationRecaptureLimit> getAll() {
    return actionLocationRecaptureLimitRepository.findAll();
  }

  public List<ActionLocationRecaptureLimit> getByInstance(UUID instanceIdentifier) {
    return actionLocationRecaptureLimitRepository.findByInstance_Identifier(instanceIdentifier);
  }

  public List<ActionLocationRecaptureLimit> getByLocation(UUID locationIdentifier) {
    return actionLocationRecaptureLimitRepository.findByLocation_Identifier(locationIdentifier);
  }

  public List<ActionLocationRecaptureLimit> getByAction(UUID actionIdentifier) {
    return actionLocationRecaptureLimitRepository.findByAction_Identifier(actionIdentifier);
  }

  public List<ActionLocationRecaptureLimit> getByInstanceAndLocation(UUID instanceIdentifier,
      UUID locationIdentifier) {
    return actionLocationRecaptureLimitRepository.findByInstance_IdentifierAndLocation_Identifier(
        instanceIdentifier, locationIdentifier);
  }

  // Upserts by instance+location+action so a recapture limit is never duplicated for the same combination
  public ActionLocationRecaptureLimit create(UUID instanceIdentifier, UUID locationIdentifier,
      UUID actionIdentifier, Integer count) {
    Instance instance = instanceService.findById(instanceIdentifier);
    Location location = locationService.findByIdentifier(locationIdentifier);
    Action action = actionService.getByIdentifier(actionIdentifier);

    ActionLocationRecaptureLimit limit = actionLocationRecaptureLimitRepository
        .findByInstance_IdentifierAndLocation_IdentifierAndAction_Identifier(
            instance.getIdentifier(), location.getIdentifier(), action.getIdentifier())
        .orElseGet(ActionLocationRecaptureLimit::new);
    limit.setInstance(instance);
    limit.setLocation(location);
    limit.setAction(action);
    limit.setCount(count);

    return actionLocationRecaptureLimitRepository.save(limit);
  }

  public ActionLocationRecaptureLimit update(UUID identifier, UUID instanceIdentifier,
      UUID locationIdentifier, UUID actionIdentifier, Integer count) {
    ActionLocationRecaptureLimit limit = getByIdentifier(identifier);
    limit.setInstance(instanceService.findById(instanceIdentifier));
    limit.setLocation(locationService.findByIdentifier(locationIdentifier));
    limit.setAction(actionService.getByIdentifier(actionIdentifier));
    limit.setCount(count);
    return actionLocationRecaptureLimitRepository.save(limit);
  }

  public void delete(UUID identifier) {
    ActionLocationRecaptureLimit limit = getByIdentifier(identifier);
    actionLocationRecaptureLimitRepository.delete(limit);
  }
}
