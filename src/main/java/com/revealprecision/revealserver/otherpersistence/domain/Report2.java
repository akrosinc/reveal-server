package com.revealprecision.revealserver.otherpersistence.domain;

import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.ReportIndicators;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class Report2 {

  @Id
  @GeneratedValue
  UUID id;


  private UUID plan;

  private UUID location;


  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private ReportIndicators reportIndicators;

  public Report2 update(ReportIndicators reportIndicators) {
    this.reportIndicators = reportIndicators;
    return this;
  }
}
