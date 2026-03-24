package com.revealprecision.revealserver.persistence.projection;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public interface InstanceListProjection {
  UUID getIdentifier();
  String getInstanceName();
  String getPlanIdentifier();
  String getPlanTitle();
  String getPlanStatus();
  String getInterventionType();
  LocalDateTime getCreatedDatetime();
  LocalDate getStartDate();
  LocalDate getEndDate();
}