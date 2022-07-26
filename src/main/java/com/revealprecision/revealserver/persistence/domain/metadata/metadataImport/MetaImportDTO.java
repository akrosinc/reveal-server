package com.revealprecision.revealserver.persistence.domain.metadata.metadataImport;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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

  UUID locationIdentifier;

  String locationName;

  String geographicLevel;

  Map<String, String> entityTags = new HashMap<>();

}
