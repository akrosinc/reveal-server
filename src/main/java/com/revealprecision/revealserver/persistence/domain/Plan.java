package com.revealprecision.revealserver.persistence.domain;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table
@Audited
public class Plan extends AbstractAuditableEntity {
    @Id
    @GenericGenerator(name = "UUIDGenerator", strategy = "uuid2")
    @GeneratedValue(generator = "UUIDGenerator")
    private UUID id;
    @Column
    private String title;

    public Plan(UUID id, String title) {
        this.id = id;
        this.title = title;
    }

    public Plan() {

    }

    public Plan(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return "Plan{" +
                "createdBy='" + createdBy + '\'' +
                ", createdDatetime=" + createdDatetime +
                ", modifiedBy='" + modifiedBy + '\'' +
                ", modifiedDatetime=" + modifiedDatetime +
                ", id=" + id +
                ", title='" + title + '\'' +
                '}';
    }
}