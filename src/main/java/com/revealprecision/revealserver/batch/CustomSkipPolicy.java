package com.revealprecision.revealserver.batch;

import javax.validation.ConstraintViolationException;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;

public class CustomSkipPolicy implements SkipPolicy {

  @Override
  public boolean shouldSkip(Throwable t, int skipCount) throws SkipLimitExceededException {
    if (t instanceof ConstraintViolationException) {
      System.out.println(t.getLocalizedMessage());
      return true;
    }
    return false;
  }
}
