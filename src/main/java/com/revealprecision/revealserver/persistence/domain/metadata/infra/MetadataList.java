package com.revealprecision.revealserver.persistence.domain.metadata.infra;

import java.io.Serializable;
import java.util.List;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Data
public class MetadataList implements Serializable {

  private List<MetadataObj> metadataObjs;
}
