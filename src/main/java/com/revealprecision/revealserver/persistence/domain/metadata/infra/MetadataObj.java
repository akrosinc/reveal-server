package com.revealprecision.revealserver.persistence.domain.metadata.infra;

import com.revealprecision.revealserver.messaging.message.Message;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Data
public class MetadataObj  implements Serializable  {

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
