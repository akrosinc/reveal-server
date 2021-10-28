package com.revealprecision.revealserver.persistence.domain;

import org.hibernate.envers.Audited;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.EntityListeners;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Objects;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Audited
public abstract class AbstractAuditableEntity {

    @Column(name = "created_by", nullable = false) protected String createdBy;
    @Column(name = "created__datetime", nullable = false) protected LocalDateTime createdDatetime = LocalDateTime.now();
    @Column(name = "modified_by", nullable = false) protected String modifiedBy;
    @Column(name = "modified_datetime", nullable = false) protected LocalDateTime modifiedDatetime = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        LocalDateTime now = ZonedDateTime.now().toLocalDateTime();
        this.createdDatetime = now;
        this.modifiedDatetime = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.modifiedDatetime = ZonedDateTime.now().toLocalDateTime();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), createdBy, createdDatetime, modifiedBy, modifiedDatetime);
    }
}