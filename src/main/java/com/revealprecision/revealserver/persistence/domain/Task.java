package com.revealprecision.revealserver.persistence.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.revealprecision.revealserver.enums.TaskPriorityEnum;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
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

  @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  @JoinColumn(name = "lookup_task_status_identifier", referencedColumnName = "identifier", nullable = false)
  private LookupTaskStatus lookupTaskStatus;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "action_identifier", referencedColumnName = "identifier", nullable = false)
  private Action action;

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

  @NotNull(message = "executionPeriodStart can not be null")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate executionPeriodStart;

  @NotNull(message = "executionPeriodEnd can not be null")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate executionPeriodEnd;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinTable(name = "task_location",
      joinColumns = @JoinColumn(name = "task_identifier"),
      inverseJoinColumns = @JoinColumn(name = "location_identifier")
  )
  private Location location;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinTable(name = "task_person",
      joinColumns = @JoinColumn(name = "task_identifier"),
      inverseJoinColumns = @JoinColumn(name = "person_identifier")
  )
  private Person person;


}
