package com.revealprecision.revealserver.props;

import com.revealprecision.revealserver.enums.ActionTitleEnum;
import com.revealprecision.revealserver.persistence.domain.Action;
import java.util.Collections;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "task.facade")
@Component
@Setter
@Getter
public class BusinessStatusProperties {

  private final String businessStatusTagName = "business-status";

  public static final String GENERAL = "GENERAL";
  Map<String, String> businessStatusMapping = Collections.singletonMap("GENERAL",
      businessStatusTagName);

  String defaultLocationBusinessStatus = "Not Visited";
  String defaultPersonBusinessStatus = "Not Visited";
  String defaultGroupBusinessStatus = "Not Visited";
  String defaultIndexCaseBusinessStatus = "Index Case Not Visited";

  public String getDefaultBusinessStatus(Action action) {
    if (action.getTitle().equals(ActionTitleEnum.INDEX_CASE.getActionTitle())) {
      return defaultIndexCaseBusinessStatus;
    } else {
      return defaultLocationBusinessStatus;
    }
  }
}
