package com.revealprecision.revealserver.amdr.persistence.domain;

public enum AmdrProcessingStatus {
  FAILED,
  SUCCESSFUL,
  PARTIALLY_PROCESSED,
  AWAITING_PARASITOLOGY,
  BUSY,
  SAVED_BUSY_PROCESSING,
  SAVING_DATA,
  UNPROCESSED,
  PROCESSED
}
