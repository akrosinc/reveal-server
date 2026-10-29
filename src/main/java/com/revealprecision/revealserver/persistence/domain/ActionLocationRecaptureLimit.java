package com.revealprecision.revealserver.persistence.domain;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@FieldNameConstants
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionLocationRecaptureLimit {

  @Id
  @GeneratedValue
  private UUID identifier;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "instance_identifier")
  private Instance instance;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "location_identifier")
  private Location location;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "action_identifier")
  private Action action;

  private Integer count;

}
