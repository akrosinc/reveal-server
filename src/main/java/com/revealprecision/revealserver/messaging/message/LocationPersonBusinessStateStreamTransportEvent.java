package com.revealprecision.revealserver.messaging.message;

import com.revealprecision.revealserver.persistence.domain.metadata.infra.Metadata;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LocationPersonBusinessStateStreamTransportEvent extends Message{

  private UUID locationId;

  private UUID planIdentifier;

  private UUID personIdentifier;

  private String personBusinessState;

  private Metadata metadata;

  private LocalDateTime updateTime;
  private List<MetaDataEvent> metaDataEvents;


}
