package com.revealprecision.revealserver.persistence.projection;

public interface TaskDataFromEventProjection {

  String getEventId();

  String getTaskIdentifier();

  String getFieldCode();

  String getEventBusinessStatus();

  String getTaskBusinessStatus();

  String getTaskStatus();
}
