package com.revealprecision.revealserver.messaging.message;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class LocationMetadataEvent extends TMetadataEvent {
  private String planTargetType;
  private String entityGeographicLevel;
  private String locationName;


}
