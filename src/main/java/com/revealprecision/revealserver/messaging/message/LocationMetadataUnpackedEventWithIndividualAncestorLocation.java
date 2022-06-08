package com.revealprecision.revealserver.messaging.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class LocationMetadataUnpackedEventWithIndividualAncestorLocation extends LocationMetadataUnpackedEventWithAncestorLocationList {

  private LocationAncestor locationAncestor;
}
