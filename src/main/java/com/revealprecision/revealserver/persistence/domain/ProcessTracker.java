package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.enums.ProcessTrackerEnum;
import com.revealprecision.revealserver.enums.ProcessType;
import java.util.List;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.checkerframework.common.aliasing.qual.Unique;
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
public class ProcessTracker extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;


  @Unique
  private UUID planIdentifier;

  @NotNull
  @Enumerated(EnumType.STRING)
  private ProcessType processType;

  private UUID processTriggerIdentifier;

  @NotNull
  @Enumerated(EnumType.STRING)
  private ProcessTrackerEnum state;

  @OneToMany(mappedBy = "processTracker", fetch = FetchType.LAZY)
  private List<TaskProcessStage> taskGenerationStages;

}
