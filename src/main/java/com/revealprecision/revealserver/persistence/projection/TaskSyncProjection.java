package com.revealprecision.revealserver.persistence.projection;

/**
 * Lightweight row for assignment driven task sync. The task_facade jsonb is returned as text so it
 * can be deserialised without loading the Task entity graph.
 */
public interface TaskSyncProjection {

  String getIdentifier();

  Long getServerVersion();

  String getTaskFacade();

  String getLocationIdentifier();
}
