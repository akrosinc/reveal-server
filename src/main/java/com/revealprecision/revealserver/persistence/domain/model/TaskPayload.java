package com.revealprecision.revealserver.persistence.domain.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

public class TaskPayload  implements Serializable {
    private String identifier;
    private String planIdentifier;
    private String focus;
    private String code;
    private Enum status; // TaskStatusEnum
    private Enum priority; // TaskPriorityEnum
    private LocalDateTime authoredOn;
    private String description;
    private LocalDateTime lastModified;
    private Enum businessStatus;
    private Date executionPeriodStart;
    private Date executionPeriodEnd;
    private String groupIdentifier;
    private String instantiatesUri;
}
