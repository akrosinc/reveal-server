package com.revealprecision.revealserver.exceptions;

public class RasterProcessingException extends RuntimeException {
  public RasterProcessingException(String message) {
    super(message);
  }

  public RasterProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
