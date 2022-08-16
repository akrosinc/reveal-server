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
import com.revealprecision.revealserver.util.ElasticModelUtil;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    List<Location> itemsToSave = new ArrayList<>(items);
    List<LocationElastic> locations = new ArrayList<>();
    Set<UUID> failedLocationIds = new HashSet<>();
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    itemsToSave.forEach(location -> {
      location.setIdentifier(UUID.randomUUID());
      LocationElastic loc = new LocationElastic();
      loc.setHashValue(ElasticModelUtil.bytesToHex(digest.digest(location
          .getGeometry()
          .toString()
          .getBytes(StandardCharsets.UTF_8))));
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
      Map<UUID, String> locNames = itemsToSave.stream().collect(Collectors.toMap(Location::getIdentifier, Location::getName));

      List<LocationBulkException> exceptions = new ArrayList<>();
      for(Map.Entry<String, String> entry : e.getFailedDocuments().entrySet()) {
        failedLocationIds.add(UUID.fromString(entry.getKey()));
        LocationBulkException locationBulkException = LocationBulkException.builder()
            .message(entry.getValue().split("reason")[2].replace("=",""))
            .locationBulk(locationBulk)
            .name(locNames.get(UUID.fromString(entry.getKey())))
            .build();
        locationBulkException.setEntityStatus(EntityStatus.ACTIVE);
        exceptions.add(locationBulkException);
      }

      locationBulkExceptionRepository.saveAll(exceptions);
    }
    itemsToSave.removeIf(el -> failedLocationIds.contains(el.getIdentifier()));
    locationRepository.saveAll(itemsToSave);
  }
}
