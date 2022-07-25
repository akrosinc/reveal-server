package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.messaging.message.PersonMetadataEvent;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.metadata.PersonMetadata;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PersonMetadataEventFactory {

  // Proceed with caution here as new updates / removals to the object will prevent rewind of the streams application.
  // In the event of new data being introduced, ensure that null pointers are catered in the streams
  // application if the event comes through, and it does not have the new fields populated
  public static PersonMetadataEvent getPersonMetadataEvent(Plan plan, List<UUID> locationList,
      PersonMetadata savedLocationMetadata) {
    PersonMetadataEvent personMetadataEvent = new PersonMetadataEvent();

    personMetadataEvent.setIdentifier(savedLocationMetadata.getIdentifier());
    personMetadataEvent.setHierarchyIdentifier(plan.getLocationHierarchy().getIdentifier());
    personMetadataEvent.setPlanIdentifier(plan.getIdentifier());
    personMetadataEvent.setIdentifier(savedLocationMetadata.getIdentifier());
    personMetadataEvent.setMetaDataEvents(
        savedLocationMetadata.getEntityValue().getMetadataObjs().stream()
            .map(MetadataEventFactory::getMetaDataEvent).collect(Collectors.toList()));
    personMetadataEvent.setEntityId(savedLocationMetadata.getPerson().getIdentifier());

    if (locationList !=null) {
      personMetadataEvent.setLocationIdList(
          locationList);
    }

    return personMetadataEvent;
  }
}
