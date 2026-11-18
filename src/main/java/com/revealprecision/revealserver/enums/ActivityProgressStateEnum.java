package com.revealprecision.revealserver.enums;

/**
 * State of a location for a plan, evaluated from the location's parent tasks. Canceled and Not
 * Relevant are intentionally absent for now.
 */
public enum ActivityProgressStateEnum {
  NOT_STARTED,
  IN_PROGRESS,
  COMPLETE
}
