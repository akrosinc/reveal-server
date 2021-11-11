package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.persistence.domain.model.PlanPayload;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table
@Audited
@NamedNativeQuery(name = "Plan.findByIdentifier",query = "select * from plan where payload ->> 'identifier' = ?", resultClass = Plan.class)
public class Plan extends AbstractAuditableEntity {

    @NotNull(message = "Cannot save with empty payload")
    @Type(type = "json")
    @Column (columnDefinition = "json")
    private PlanPayload payload;

    @Override
    public String toString() {
        return "\"plan\": {" +
                ", \"id\": \"" + id + "\"" +
                ", \"createdDatetime\": \"" + createdDatetime + "\"" +
                ", \"modifiedDatetime\": \"" + modifiedDatetime + "\"" +
                ", \"payload\": \"" + payload + "\"" +
                "}";
    }
}