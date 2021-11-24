package com.revealprecision.revealserver.persistence.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Audited
public abstract class AbstractAuditableEntity {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS[X]", timezone = "${spring.jackson.time-zone}")
    protected LocalDateTime createdDatetime = LocalDateTime.now();

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS[X]", timezone = "${spring.jackson.time-zone}")
    protected LocalDateTime modifiedDatetime = LocalDateTime.now();

    @CreatedBy
    protected String createdBy;

    @LastModifiedBy
    protected String modifiedBy;


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
}