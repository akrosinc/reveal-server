package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.enums.ProcessTrackerEnum;
import com.revealprecision.revealserver.enums.TaskProcessEnum;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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

@FieldNameConstants
@Entity
@Audited
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@SQLDelete(sql = "UPDATE condition SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
public class TaskProcessStage extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;

  private UUID taskIdentifier;

  private UUID baseEntityIdentifier;

  @ManyToOne
  @JoinColumn(name = "process_tracker_identifier", referencedColumnName = "identifier")
  private ProcessTracker processTracker;

  @NotNull
  @Enumerated(EnumType.STRING)
  private ProcessTrackerEnum state;

  @NotNull
  @Enumerated(EnumType.STRING)
  private TaskProcessEnum taskProcess;

}
