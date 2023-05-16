package com.revealprecision.revealserver.persistence.domain.metadata.metadataImport;

import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Setter
@Getter
public
class SheetData{
  Map<EntityTagEvent, Object> rawEntityData = new HashMap<>();
  Map<EntityTagEvent, Object> convertedEntityData = new HashMap<>();
  Map<EntityTagEvent, String> errors = new HashMap<>();
}