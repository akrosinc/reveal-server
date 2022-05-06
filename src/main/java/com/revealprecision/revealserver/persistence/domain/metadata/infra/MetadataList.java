package com.revealprecision.revealserver.persistence.domain.metadata.infra;

import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MetadataList implements Serializable {

  private List<MetadataObj> metadataObjs;
}
