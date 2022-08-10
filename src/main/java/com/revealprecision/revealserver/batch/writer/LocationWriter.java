package com.revealprecision.revealserver.batch.writer;

import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationBulk;
import com.revealprecision.revealserver.persistence.domain.LocationBulkException;
import com.revealprecision.revealserver.persistence.es.LocationElastic;
import com.revealprecision.revealserver.persistence.repository.LocationBulkExceptionRepository;
import com.revealprecision.revealserver.persistence.repository.LocationBulkRepository;
import com.revealprecision.revealserver.persistence.repository.LocationElasticRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.BulkFailureException;

@StepScope
@Slf4j
public class LocationWriter implements ItemWriter<Location> {

  @Autowired
  private LocationElasticRepository locationElasticRepository;
  @Autowired
  private LocationRepository locationRepository;
  @Autowired
  private LocationBulkRepository locationBulkRepository;
  @Autowired
  private LocationBulkExceptionRepository locationBulkExceptionRepository;

  @Value("#{jobParameters['locationBulkId']}")
  private String locationBulkId;
  private LocationBulk locationBulk;

  @BeforeStep
  private void setUpData() {
    locationBulk = locationBulkRepository.getById(UUID.fromString(locationBulkId));
  }

  @Override
  public void write(List<? extends Location> items) throws Exception {
    items = locationRepository.saveAll(items);
    List<LocationElastic> locations = new ArrayList<>();
    items.forEach(location -> {
      LocationElastic loc = new LocationElastic();
      loc.setId(location.getIdentifier().toString());
      loc.setLevel(location.getGeographicLevel().getName());
      loc.setName(location.getName());
      loc.setExternalId(location.getExternalId().toString());
      loc.setGeometry(location.getGeometry());
      locations.add(loc);
    });

    try {
      locationElasticRepository.saveAll(locations);
    } catch (BulkFailureException e) {
      System.out.println(e.getMessage());

      e.getFailedDocuments().values().forEach(err -> log.error("Elasticsearch import location({}) error: {}", err));
      Set<UUID> failedLocations = e.getFailedDocuments().keySet().stream().map(UUID::fromString)
          .collect(
              Collectors.toSet());
      Set<String> failedLocationNames = items.stream()
          .filter(el -> failedLocations.contains(el.getIdentifier()))
          .map(Location::getName)
          .collect(Collectors.toSet());
      locationRepository.deleteFailedLocations(failedLocations);

      List<LocationBulkException> exceptions = new ArrayList<>();
      for (String name : failedLocationNames) {
        LocationBulkException locationBulkException = LocationBulkException.builder().message("GeoJSON is formatted incorrectly")
            .locationBulk(locationBulk).name(name).build();
        locationBulkException.setEntityStatus(EntityStatus.ACTIVE);
        exceptions.add(locationBulkException);
      }
      locationBulkExceptionRepository.saveAll(exceptions);
    }

  }
}
