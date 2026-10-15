package com.revealprecision.revealserver.props;

import com.revealprecision.revealserver.enums.ActionTitleEnum;
import java.util.EnumSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("task.action.exclude")
@Component
public class NoTaskActionProperties {

  @Setter @Getter
  private Set<ActionTitleEnum> actions = EnumSet.of(
      ActionTitleEnum.RCD,
      ActionTitleEnum.INDEX_CASE,
      ActionTitleEnum.SECONDARY_INDEX_CASE,
      ActionTitleEnum.INDEX_CASE_MEMBER,
      ActionTitleEnum.SECONDARY_INDEX_CASE_MEMBER,
      ActionTitleEnum.RCD_MEMBER,
      ActionTitleEnum.STRUCTURE_SURVEY,
      ActionTitleEnum.ENROLMENT,
      ActionTitleEnum.ENROLMENT_STRUCTURE,
      ActionTitleEnum.FOLLOWUP_STRUCTURE,
      ActionTitleEnum.FOLLOWUP,
      ActionTitleEnum.DESTRUCTION,
      ActionTitleEnum.RAPID_COVERAGE_STRUCTURE,
      ActionTitleEnum.RAPID_COVERAGE
  );

}
