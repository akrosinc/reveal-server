package com.revealprecision.revealserver.messaging.message;

import com.revealprecision.revealserver.persistence.domain.metadata.infra.TagData;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetaDataEvent extends Message{
  private String tag;
  private TagData tagData;

  private String type;
}
