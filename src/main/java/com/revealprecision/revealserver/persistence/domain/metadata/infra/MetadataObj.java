package com.revealprecision.revealserver.persistence.domain.metadata.infra;

import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MetadataObj implements Serializable {

  private String tag;

  private String type;

  private String dataType;

  private TagData current;

  private List<TagData> history;
}
