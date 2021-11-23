package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.enums.PlanInterventionTypeEnum;
import com.revealprecision.revealserver.enums.PlanStatusEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Audited
@Getter
@Setter
@RequiredArgsConstructor
@NamedNativeQueries(
        @NamedNativeQuery(name = "findByIdentifier",
                          query = "select * from plan where identifier = ?",
                          resultClass = Plan.class)
)
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