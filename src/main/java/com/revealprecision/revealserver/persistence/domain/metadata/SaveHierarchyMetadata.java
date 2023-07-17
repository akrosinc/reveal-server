package com.revealprecision.revealserver.persistence.domain.metadata;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@AllArgsConstructor
public
class SaveHierarchyMetadata {

  String locationIdentifier;

  String hierarchyIdentifier;

  List<String> nodeOrder;
}
