package com.revealprecision.revealserver.batch.processor;

import com.revealprecision.revealserver.api.v1.dto.request.LocationRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationBulk;
import com.revealprecision.revealserver.persistence.repository.GeographicLevelRepository;
import com.revealprecision.revealserver.persistence.repository.LocationBulkRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@RequiredArgsConstructor
@StepScope
@Component
public class LocationItemProcessor implements ItemProcessor<LocationRequest, Location> {

  private final LocationBulkRepository locationBulkRepository;
  private final GeographicLevelRepository geographicLevelRepository;
  @Value("#{jobParameters['locationBulkId']}")
  private String locationBulkId;
  private Map<String, GeographicLevel> geographicLevelsMappedByName = new HashMap<>();
  private LocationBulk locationBulk;


  @BeforeStep
  private void setUpData() {
    locationBulk = locationBulkRepository.getById(UUID.fromString(locationBulkId));
    geographicLevelsMappedByName = geographicLevelRepository.findAll().stream().collect(Collectors
        .toMap(geographicLevel -> geographicLevel.getName(), geographicLevel -> geographicLevel));
  }

  @Override
  public Location process(LocationRequest item) {
    if (!isLocationValid(item)) {
      return null;
    }
    var location = Location.builder().geographicLevel(
        geographicLevelsMappedByName.get(item.getProperties().getGeographicLevel()))
        .type(item.getType())
        .geometry(item.getGeometry()).name(item.getProperties().getName())
        .status(item.getProperties().getStatus())
        .externalId(item.getProperties().getExternalId()).build();
    location.setEntityStatus(EntityStatus.ACTIVE);
    location.setLocationBulk(locationBulk);
    return location;
  }

  private boolean isLocationValid(LocationRequest item) {
    //TODO validate.
    return true;
  }
}
