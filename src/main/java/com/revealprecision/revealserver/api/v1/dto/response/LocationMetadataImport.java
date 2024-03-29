package com.revealprecision.revealserver.api.v1.dto.response;

import com.revealprecision.revealserver.messaging.message.MetaDataEvent;
import com.revealprecision.revealserver.persistence.domain.metadata.infra.MetadataList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LocationMetadataImport {

  private UUID identifier;

  private UUID locationIdentifier;

  private String locationName;

  private List<MetaDataEvent> entityValue;
}
