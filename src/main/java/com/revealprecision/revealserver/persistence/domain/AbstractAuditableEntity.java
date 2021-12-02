package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.enums.EntityStatusEnum;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Audited
public abstract class AbstractAuditableEntity {


    protected LocalDateTime createdDatetime;

    protected LocalDateTime modifiedDatetime;

    @CreatedBy
    protected String createdBy;

    @LastModifiedBy
    protected String modifiedBy;


    protected String entityStatus;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = ZonedDateTime.now().toLocalDateTime();
        this.createdDatetime = now;
        this.modifiedDatetime = now;
        this.entityStatus = EntityStatusEnum.ACTIVE.name();
    }

    @PreUpdate
    public void preUpdate() {
        this.modifiedDatetime = ZonedDateTime.now().toLocalDateTime();
    }
}