package com.revealprecision.revealserver.persistence.domain.metadata.infra;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TagData implements Serializable {

  private TagValue value;

  private Metadata meta;

}
