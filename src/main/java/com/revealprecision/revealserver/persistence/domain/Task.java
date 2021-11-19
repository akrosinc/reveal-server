package com.revealprecision.revealserver.persistence.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.revealprecision.revealserver.enums.TaskPriorityEnum;
import com.revealprecision.revealserver.enums.TaskStatusEnum;
import com.revealprecision.revealserver.enums.BusinessStatusEnum;
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
    @NotNull(message = "identifier can not be null")
    private String identifier;

    @NotNull(message = "planIdentifier can not be null")
    private String planIdentifier;

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

    @NotNull(message = "instantiatesUri can not be null")
    private String instantiatesUri;
}
