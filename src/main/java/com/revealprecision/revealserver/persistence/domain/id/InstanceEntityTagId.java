package com.revealprecision.revealserver.persistence.domain.id;

import java.io.Serializable;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public  class InstanceEntityTagId implements Serializable {

  @Column(name = "instance_id")
  private UUID instanceId;

  @Column(name = "entity_tag_id")
  private UUID entityTagId;
}