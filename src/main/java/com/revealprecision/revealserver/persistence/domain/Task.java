package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.enums.TaskPriorityEnum;
import com.revealprecision.revealserver.enums.TaskStatusEnum;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Audited
@Getter
@Setter
public class Task extends AbstractAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_seq_generator")
    @SequenceGenerator(name = "task_seq_generator", sequenceName = "task_seq")
    private Long id;
    private String identifier;
    private String planIdentifier;
    private String focus;
    private String code;
    private TaskStatusEnum status;
    private TaskPriorityEnum priority;
    private LocalDateTime authoredOn;
    private String description;
    private LocalDateTime lastModified;
    private Enum businessStatus;
    private Date executionPeriodStart;
    private Date executionPeriodEnd;
    private String groupIdentifier;
    private String instantiatesUri;
}
