package com.revealprecision.revealserver.persistence.domain;

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
public class Report {

  @Id
  @GeneratedValue
  UUID id;

  @ManyToOne
  @JoinColumn(name = "plan_id", referencedColumnName = "identifier")
  private Plan plan;

  @ManyToOne
  @JoinColumn(name = "location_id", referencedColumnName = "identifier")
  private Location location;


  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private ReportIndicators reportIndicators;

  public Report update(ReportIndicators reportIndicators) {
    this.reportIndicators = reportIndicators;
    return this;
  }
}
