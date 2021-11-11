package com.revealprecision.revealserver.persistence.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.vladmihalcea.hibernate.type.json.JsonType;
import org.hibernate.annotations.TypeDef;
import org.hibernate.envers.Audited;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;


@TypeDef(name = "json", typeClass = JsonType.class)
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Audited
public abstract class AbstractAuditableEntity {
    @Id
    @Column
    protected Long id;

    @Column(name = "created_datetime", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS[X]", timezone = "${spring.jackson.time-zone}")
    protected LocalDateTime createdDatetime = LocalDateTime.now();

    @Column(name = "modified_datetime", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS[X]", timezone = "${spring.jackson.time-zone}")
    protected LocalDateTime modifiedDatetime = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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