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
import com.revealprecision.revealserver.props.LocationImportProperties;
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
  @Autowired
  private LocationImportProperties locationImportProperties;

  @Value("#{jobParameters['locationBulkId']}")
  private String locationBulkId;
  private LocationBulk locationBulk;

  @BeforeStep
  private void setUpData() {
    locationBulk = locationBulkRepository.getById(UUID.fromString(locationBulkId));
  }

  @Override
  public void write(List<? extends Location> items) throws Exception {
    Set<String> hashes = new HashSet<>();
    List<Location> itemsToSave = new ArrayList<>(items);
    List<LocationElastic> locations = new ArrayList<>();
    Set<UUID> failedLocationIds = new HashSet<>();
    MessageDigest digest = MessageDigest.getInstance("SHA-256");

    long count = itemsToSave.stream().takeWhile(location -> location.getIdentifier() != null)
        .count();

    if (count < itemsToSave.size()) {
      throw new Exception("External Id or Identifier not provided for one or more locations");
    }

    itemsToSave.forEach(location -> {
      String hash = ElasticModelUtil.bytesToHex(digest.digest(location
          .getGeometry()
          .toString()
          .getBytes(StandardCharsets.UTF_8)));
      hashes.add(hash);

      location.setIdentifier(location.getIdentifier());
      location.setHashValue(hash);
      location.setLocationProperty(location.getLocationProperty());

      if (location.getExternalId() != null) {
        location.setExternalId(location.getExternalId());
      }
      LocationElastic loc = new LocationElastic();
      loc.setHashValue(hash);
      loc.setId(location.getIdentifier().toString());
      loc.setLevel(location.getGeographicLevel().getName());
      loc.setName(location.getName());
      loc.setGeometry(location.getGeometry());
      locations.add(loc);
    });

    List<String> existingNames = new ArrayList<>();
    if (locationImportProperties.isAllowDuplicates()){
      existingNames = locationRepository.findAllByHashes(hashes);
      if (!existingNames.isEmpty()) {
        List<LocationBulkException> duplicates = new ArrayList<>();
        existingNames.forEach(el -> {
          LocationBulkException duplicate = LocationBulkException.builder()
              .message("Already exist")
              .locationBulk(locationBulk)
              .name(el)
              .build();
          duplicate.setEntityStatus(EntityStatus.ACTIVE);
          duplicates.add(duplicate);
        });
        locationBulkExceptionRepository.saveAll(duplicates);
      }
      List<String> finalExistingNames = existingNames;
      locations.removeIf(el -> finalExistingNames.contains(el.getName()));
    }

    try {
      locationElasticRepository.saveAll(locations);
    } catch (BulkFailureException e) {
      Map<UUID, String> locNames = itemsToSave.stream()
          .collect(Collectors.toMap(Location::getIdentifier, Location::getName));

      List<LocationBulkException> exceptions = new ArrayList<>();
      for (Map.Entry<String, String> entry : e.getFailedDocuments().entrySet()) {
        failedLocationIds.add(UUID.fromString(entry.getKey()));
        String errorMessage;
        if (entry.getValue().split("reason")[2].replace("=", "").length() > 251) {
          errorMessage = entry.getValue().split("reason")[2].replace("=", "").substring(0, 251)
              .concat("...");
        } else {
          errorMessage = entry.getValue().split("reason")[2].replace("=", "");
        }
        log.error("Error: {}",entry.getValue());
        LocationBulkException locationBulkException = LocationBulkException.builder()
            .message(errorMessage)
            .locationBulk(locationBulk)
            .name(locNames.get(UUID.fromString(entry.getKey())))
            .build();
        locationBulkException.setEntityStatus(EntityStatus.ACTIVE);
        exceptions.add(locationBulkException);
      }

      locationBulkExceptionRepository.saveAll(exceptions);
    }
    List<String> finalExistingNames1 = existingNames;
    itemsToSave.removeIf(
        el -> failedLocationIds.contains(el.getIdentifier()) || finalExistingNames1.contains(
            el.getName()));
    locationRepository.saveAll(itemsToSave);
  }
}
