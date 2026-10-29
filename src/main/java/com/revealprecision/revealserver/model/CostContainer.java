package com.revealprecision.revealserver.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A fully dynamic description of a cost calculation.
 *
 * <p>The {@link #formula} is an arbitrary expression using single tokens as variables, for example
 * {@code "a * b * c"}. Each token referenced in the formula is described by a {@link CostVariable}
 * held in {@link #variables}, e.g. {@code a = distance}, {@code b = cost_per_km},
 * {@code c = action_count}.</p>
 *
 * <p>This structure is persisted inside the {@code ActionTemplateConfig} JSONB column and carries no
 * captured runtime values. Captured values live in a {@code CapturedCostContainer} (see capture
 * cost domain).</p>
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CostContainer implements Serializable {

  private static final long serialVersionUID = 1L;

  /** The raw expression, e.g. {@code "a * b * c"}. Tokens map to keys in {@link #variables}. */
  private String formula;

  /**
   * Variables referenced by {@link #formula}, keyed by the formula token (e.g. {@code "a"}).
   *
   * <p>Keyed by token for O(1) lookup instead of scanning a list. A {@link LinkedHashMap} keeps the
   * insertion/declaration order stable for predictable JSON output and UI rendering.</p>
   */
  @Builder.Default
  private Map<String, CostVariable> variables = new LinkedHashMap<>();

  /**
   * A single variable in the formula. On the template side only the definition fields are set and
   * {@link #value} stays {@code null}. The same construct is reused at capture time by copying the
   * definition and populating {@link #value} with the value captured from the user.
   */
  @Getter
  @Setter
  @Builder
  @ToString
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CostVariable implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The token used in the formula, e.g. {@code "a"}. Redundant with the map key in
     * {@link CostContainer#variables} but kept so the object is self-describing on its own.
     */
    private String key;

    /** The semantic name of the variable, e.g. {@code "distance"}. */
    private String name;

    /** Optional human-friendly label for UI display. */
    private String label;

    /** Optional unit of measure, e.g. {@code "km"}. */
    private String unit;

    /** Where the value comes from when the template is used. */
    private CostVariableSource source;

    /** Optional fixed/default value used when {@link #source} is {@link CostVariableSource#CONSTANT}. */
    private Double defaultValue;

    /**
     * The value captured from the user (or resolved) when the template is used. Stays {@code null}
     * on the template definition; populated only on captured-cost records.
     */
    private Double value;
  }

  /** Describes how a variable's value is obtained at capture time. */
  public enum CostVariableSource {
    /** Provided by a user at capture time. */
    MANUAL,
    /** Derived from data in the system (e.g. counts, distances). */
    COMPUTED,
    /** A fixed value defined on the template itself. */
    CONSTANT
  }
}
