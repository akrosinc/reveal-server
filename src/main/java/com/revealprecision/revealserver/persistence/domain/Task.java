package com.revealprecision.revealserver.persistence.domain;

import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table
@Audited
public class Task extends AbstractAuditableEntity {
    @NotNull(message = "Cannot save with empty payload")
    @Column (name = "payload")
    private String payload;

    @Override
    public String toString() {
        return "\"task\": {" +
                ", \"id\": \"" + id + "\"" +
                ", \"createdDatetime\": \"" + createdDatetime + "\"" +
                ", \"modifiedDatetime\": \"" + modifiedDatetime + "\"" +
                ", \"payload\": \"" + payload + "\"" +
                "}";
    }
}