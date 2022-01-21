package com.revealprecision.revealserver.persistence.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.revealprecision.revealserver.enums.BusinessStatusEnum;
import com.revealprecision.revealserver.enums.TaskPriorityEnum;
import com.revealprecision.revealserver.enums.TaskStatusEnum;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Getter
@Setter
@Builder
@SQLDelete(sql = "UPDATE task SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
public class Task extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  @NotNull(message = "identifier can not be null")
  private UUID identifier;

  @NotNull(message = "focus can not be null")
  private String focus;

  @NotNull(message = "code can not be null")
  private String code;

  @NotNull(message = "status can not be null")
  @Enumerated(EnumType.STRING)
  private TaskStatusEnum status;

  @NotNull(message = "priority can not be null")
  @Enumerated(EnumType.STRING)
  private TaskPriorityEnum priority;

  @NotNull(message = "authoredOn can not be null")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS[X]", timezone = "${spring.jackson.time-zone}")
  private LocalDateTime authoredOn;

  private String description;

  @NotNull(message = "lastModified can not be null")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS[X]", timezone = "${spring.jackson.time-zone}")
  private LocalDateTime lastModified;

  @NotNull(message = "businessStatus can not be null")
  @Enumerated(EnumType.STRING)
  private BusinessStatusEnum businessStatus;

  @NotNull(message = "executionPeriodStart can not be null")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private Date executionPeriodStart;

  @NotNull(message = "executionPeriodEnd can not be null")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private Date executionPeriodEnd;

  @NotNull(message = "groupIdentifier can not be null")
  private String groupIdentifier;


  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plan_identifier", referencedColumnName = "identifier", nullable = false)
  private Plan plan;
  @ManyToOne
  @JoinColumn(name = "instantiates_uri", referencedColumnName = "identifier", nullable = false)
  private Form instantiatesUriForm;

}
