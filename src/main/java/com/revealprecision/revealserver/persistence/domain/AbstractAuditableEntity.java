package com.revealprecision.revealserver.persistence.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.revealprecision.revealserver.enums.EntityStatus;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import javax.persistence.PostRemove;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EntityStatus entityStatus;

  @PrePersist
  public void prePersist() {
    LocalDateTime now = ZonedDateTime.now().toLocalDateTime();
    this.createdDatetime = now;
    this.modifiedDatetime = now;
    this.entityStatus = EntityStatus.ACTIVE;
  }

  @PreUpdate
  public void preUpdate() {
    this.modifiedDatetime = ZonedDateTime.now().toLocalDateTime();
  }

  @PostRemove
  public void postDelete() {
    this.entityStatus = EntityStatus.DELETED;
  }

  public void setEntityStatus(EntityStatus entityStatus) {
    this.entityStatus = entityStatus;
  }
}