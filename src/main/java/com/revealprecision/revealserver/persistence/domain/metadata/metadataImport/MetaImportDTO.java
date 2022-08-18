package com.revealprecision.revealserver.persistence.domain.metadata.metadataImport;

import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetaImportDTO {

  LocationHierarchy locationHierarchy;
  String hierarchyError;

  Location location;
  String locationError;

  Map<EntityTagEvent, Object> rawEntityData = new HashMap<>();
  Map<EntityTagEvent, Object> convertedEntityData = new HashMap<>();
  Map<EntityTagEvent, String> errors = new HashMap<>();
}
