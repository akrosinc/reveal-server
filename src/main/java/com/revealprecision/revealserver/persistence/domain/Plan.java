package com.revealprecision.revealserver.persistence.domain;

import com.fasterxml.jackson.annotation.JsonRawValue;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table
@Audited
public class Plan extends AbstractAuditableEntity {

    @NotNull(message = "Cannot save with empty payload")
    @Column (name = "payload", columnDefinition = "json")
    @JsonRawValue
    private String payload;

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