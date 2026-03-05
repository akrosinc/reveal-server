package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.persistence.domain.id.InstanceEntityTagId;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Audited
public class InstanceEntityTag {

  @EmbeddedId
  private InstanceEntityTagId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("instanceId")
  @JoinColumn(name = "instance_id", nullable = false)
  private Instance instance;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("entityTagId")
  @JoinColumn(name = "entity_tag_id", nullable = false)
  private EntityTag entityTag;
}




