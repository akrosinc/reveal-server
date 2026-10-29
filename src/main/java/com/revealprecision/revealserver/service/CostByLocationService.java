package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.request.CostByLocationRequest;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.CostByLocation;
import com.revealprecision.revealserver.persistence.domain.CostByLocation.Fields;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.repository.CostByLocationRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CostByLocationService {

  private final CostByLocationRepository costByLocationRepository;
  private final LocationService locationService;
  private final ActionService actionService;

  public CostByLocation getByIdentifier(UUID identifier) {
    return costByLocationRepository.findById(identifier)
        .orElseThrow(() -> new NotFoundException(Pair.of(Fields.identifier, identifier),
            CostByLocation.class));
  }

  public List<CostByLocation> getByLocation(UUID locationIdentifier) {
    return costByLocationRepository.findByLocation_Identifier(locationIdentifier);
  }

  public List<CostByLocation> getByAction(UUID actionIdentifier) {
    return costByLocationRepository.findByAction_Identifier(actionIdentifier);
  }

  public CostByLocation create(CostByLocationRequest request) {
    Location location = locationService.findByIdentifier(request.getLocationIdentifier());
    Action action = actionService.getByIdentifier(request.getActionIdentifier());

    CostByLocation costByLocation = CostByLocation.builder()
        .location(location)
        .action(action)
        .costContainer(request.getCostContainer())
        .calculatedValue(request.getCalculatedValue())
        .build();

    return costByLocationRepository.save(costByLocation);
  }

  public CostByLocation update(UUID identifier, CostByLocationRequest request) {
    CostByLocation costByLocation = getByIdentifier(identifier);
    costByLocation.setLocation(locationService.findByIdentifier(request.getLocationIdentifier()));
    costByLocation.setAction(actionService.getByIdentifier(request.getActionIdentifier()));
    costByLocation.setCostContainer(request.getCostContainer());
    costByLocation.setCalculatedValue(request.getCalculatedValue());
    return costByLocationRepository.save(costByLocation);
  }

  public void delete(UUID identifier) {
    CostByLocation costByLocation = getByIdentifier(identifier);
    costByLocationRepository.delete(costByLocation);
  }
}
