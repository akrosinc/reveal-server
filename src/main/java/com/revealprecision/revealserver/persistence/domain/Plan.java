package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.enums.PlanInterventionTypeEnum;
import com.revealprecision.revealserver.enums.PlanStatusEnum;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Getter
@Setter
@RequiredArgsConstructor
@NamedNativeQueries(
    @NamedNativeQuery(name = "findByIdentifier",
        query = "select * from plan where identifier = ? where entity_status='ACTIVE'",
        resultClass = Plan.class)
)
@SQLDelete(sql = "UPDATE plan SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
public class Plan extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;
  private String name;
  private String title;
  private Date effectivePeriodStart;
  private Date effectivePeriodEnd;
  @Enumerated(EnumType.STRING)
  private PlanStatusEnum status;
  @Enumerated(EnumType.STRING)
  private PlanInterventionTypeEnum interventionType;
}