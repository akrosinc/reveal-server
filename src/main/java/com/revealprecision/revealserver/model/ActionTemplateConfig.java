package com.revealprecision.revealserver.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Configuration stored in the {@code ActionTemplate} JSONB column.
 *
 * <p>Holds the dynamic cost definition ({@link #costContainer}) describing the formula and its
 * variables. The captured values for those variables are stored separately at the time the template
 * is used (see the capture cost domain).</p>
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ActionTemplateConfig implements Serializable {

  private static final long serialVersionUID = 1L;

  /** Dynamic formula and variable definitions, e.g. {@code a * b * c}. */
  private CostContainer costContainer;

  private TaskGenerationRecurrence recurrence;

  public enum TaskGenerationRecurrence {
    DAILY, WEEKLY, MONTHLY
  }
}
