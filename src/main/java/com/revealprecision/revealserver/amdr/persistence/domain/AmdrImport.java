package com.revealprecision.revealserver.amdr.persistence.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.revealprecision.revealserver.persistence.domain.AbstractAuditableEntity;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

@FieldNameConstants
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(schema = "amdr")
@Where(clause = "entity_status='ACTIVE'")
@Audited
public class AmdrImport extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;

  @Column(nullable = false)
  private String filename;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS[X]", timezone = "${spring.jackson.time-zone}")
  @Column(nullable = false)
  private LocalDateTime uploadedDatetime;

  @Column(nullable = false)
  private String uploadedBy;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AmdrProcessingStatus status;

}
