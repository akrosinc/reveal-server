package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.model.CostContainer;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.UUID;
import javax.persistence.Column;
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
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@FieldNameConstants
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class CostByLocation {

  @Id
  @GeneratedValue
  private UUID identifier;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "location_identifier")
  private Location location;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "action_identifier")
  private Action action;

  @Type(type = "jsonb")
  @Column(name = "cost_container", columnDefinition = "jsonb")
  private CostContainer costContainer;

  private Double calculatedValue;

}
