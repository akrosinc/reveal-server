package com.revealprecision.revealserver.messaging.message;

import com.revealprecision.revealserver.persistence.domain.metadata.infra.TagData;
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
public class MetaDataEvent extends Message {

  private String tag;

  private TagData tagData;

  private String type;

  private String dataType;

  private boolean isActive;
}
