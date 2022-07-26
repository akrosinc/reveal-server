package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.messaging.message.LocationMetadataEvent;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.metadata.LocationMetadata;
import java.util.stream.Collectors;

public class LocationMetadataEventFactory {


  // Proceed with caution here as new updates / removals to the object will prevent rewind of the streams application.
  // In the event of new data being introduced, ensure that null pointers are catered in the streams
  // application if the event comes through, and it does not have the new fields populated
  public static LocationMetadataEvent getLocationMetadataEvent(Plan plan, Location location,
      LocationMetadata savedLocationMetadata) {
    LocationMetadataEvent locationMetadataEvent = new LocationMetadataEvent();
    locationMetadataEvent.setPlanTargetType(
        plan == null ? null : plan.getPlanTargetType().getGeographicLevel().getName());
    locationMetadataEvent.setIdentifier(savedLocationMetadata.getIdentifier());
    if (location != null) {
      locationMetadataEvent.setEntityGeographicLevel(location.getGeographicLevel().getName());
    }
    locationMetadataEvent.setHierarchyIdentifier(plan == null ? null : plan.getLocationHierarchy().getIdentifier());
    locationMetadataEvent.setMetaDataEvents(
        savedLocationMetadata.getEntityValue().getMetadataObjs().stream()
            .map(MetadataEventFactory::getMetaDataEvent).collect(
                Collectors.toList()));
    locationMetadataEvent.setEntityId(savedLocationMetadata.getLocation().getIdentifier());
    return locationMetadataEvent;
  }
}
