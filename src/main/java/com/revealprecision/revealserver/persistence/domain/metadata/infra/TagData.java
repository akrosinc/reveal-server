package com.revealprecision.revealserver.persistence.domain.metadata.infra;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TagData implements Serializable {

  private TagValue value;

  private Metadata meta;

}
