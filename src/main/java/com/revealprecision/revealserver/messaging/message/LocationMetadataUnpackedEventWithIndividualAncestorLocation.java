package com.revealprecision.revealserver.messaging.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LocationMetadataUnpackedEventWithIndividualAncestorLocation extends LocationMetadataUnpackedEventWithAncestorLocationList {

  private LocationAncestor locationAncestor;
}
