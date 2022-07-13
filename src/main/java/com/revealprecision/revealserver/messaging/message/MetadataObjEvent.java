package com.revealprecision.revealserver.messaging.message;

import com.revealprecision.revealserver.persistence.domain.metadata.infra.MetadataObj;
import com.revealprecision.revealserver.persistence.domain.metadata.infra.TagData;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class MetadataObjEvent extends Message {

  private UUID entityId;

  private UUID locationHierarchy;

  private String locationGeographicLevel;

  private MetadataObj metadataObj;

  private UUID ancestor;

}
