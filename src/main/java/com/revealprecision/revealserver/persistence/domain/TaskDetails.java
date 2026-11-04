package com.revealprecision.revealserver.persistence.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Extensible metadata stored in the {@code task.details} jsonb column.
 *
 * <p>Currently holds {@code limitCounter}: the number of times a task may be executed before it is
 * automatically closed. Additional fields can be added over time without a schema change.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class TaskDetails implements Serializable {

  private static final long serialVersionUID = 1L;

  private Integer limitCounter;
}
