package com.revealprecision.revealserver.batch;

import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.persistence.domain.UserBulk;
import com.revealprecision.revealserver.persistence.domain.UserBulkException;
import com.revealprecision.revealserver.persistence.repository.UserBulkExceptionRepository;
import com.revealprecision.revealserver.persistence.repository.UserBulkRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.file.transform.IncorrectTokenCountException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@StepScope
@Component
public class CustomSkipPolicy implements SkipPolicy {

  private final UserBulkExceptionRepository userBulkExceptionRepository;
  private final UserBulkRepository userBulkRepository;
  @Value("#{jobParameters['userBulkId']}")
  private String userBulkId;

  @Override
  public boolean shouldSkip(Throwable t, int skipCount) throws SkipLimitExceededException {
    if (t.getCause() instanceof IncorrectTokenCountException) {
      UserBulk userBulk = userBulkRepository.getById(UUID.fromString(userBulkId));
      UserBulkException userBulkException = UserBulkException.builder()
          .message("CSV file got wrong structure")
          .userBulk(userBulk)
          .build();
      userBulkException.setEntityStatus(EntityStatus.ACTIVE);
      userBulkExceptionRepository.save(userBulkException);
      return true;
    }
    return false;
  }
}
