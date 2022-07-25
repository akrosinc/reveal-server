package com.revealprecision.revealserver.persistence.domain.metadata.infra;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class MetadataObj implements Serializable {

  private String tag;

  private String tagKey;

  private UUID entityTagId;

  private String type;

  private String dataType;

  private TagData current;

  private List<TagData> history;

  private boolean isActive = true;

  private boolean dateScope;

  private String dateForDateScope;

  private Long captureNumber;
}
