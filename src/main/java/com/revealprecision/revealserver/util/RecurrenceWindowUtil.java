package com.revealprecision.revealserver.util;

import com.revealprecision.revealserver.model.ActionTemplateConfig.TaskGenerationRecurrence;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * Computes the recurrence window {@code [start, end)} that a reference date falls into for a given
 * {@link TaskGenerationRecurrence}.
 *
 * <p>Used by the recapture-limit task generation schedule to decide whether a task has already been
 * generated for the current window: the schedule runs daily, but a task should only be created once
 * per window (one per clock minute for MINUTELY, one per clock hour for HOURLY, one per day for
 * DAILY, one per ISO week for WEEKLY, one per calendar month for MONTHLY).</p>
 */
public final class RecurrenceWindowUtil {

  private RecurrenceWindowUtil() {
  }

  @Getter
  @AllArgsConstructor
  @ToString
  public static class Window {

    private final LocalDateTime start;
    private final LocalDateTime end;
  }

  /**
   * Returns the half-open window {@code [start, end)} that {@code reference} falls into for the
   * supplied recurrence. A null recurrence is treated as {@link TaskGenerationRecurrence#DAILY}.
   */
  public static Window windowFor(TaskGenerationRecurrence recurrence, LocalDateTime reference) {
    TaskGenerationRecurrence effective =
        recurrence == null ? TaskGenerationRecurrence.DAILY : recurrence;

    LocalDate refDate = reference.toLocalDate();

    switch (effective) {
      case MINUTELY: {
        // Clock minute: hh:mm:00 (inclusive) to hh:(mm+1):00 (exclusive)
        LocalDateTime start = reference.truncatedTo(ChronoUnit.MINUTES);
        return new Window(start, start.plusMinutes(1));
      }
      case HOURLY: {
        // Clock hour: hh:00:00 (inclusive) to (hh+1):00:00 (exclusive)
        LocalDateTime start = reference.truncatedTo(ChronoUnit.HOURS);
        return new Window(start, start.plusHours(1));
      }
      case WEEKLY: {
        // ISO week: Monday 00:00 (inclusive) to next Monday 00:00 (exclusive)
        LocalDate weekStart = refDate.with(
            TemporalAdjusters.previousOrSame(WeekFields.ISO.getFirstDayOfWeek()));
        LocalDateTime start = weekStart.atStartOfDay();
        return new Window(start, start.plusWeeks(1));
      }
      case MONTHLY: {
        // Calendar month: 1st 00:00 (inclusive) to 1st of next month 00:00 (exclusive)
        LocalDate monthStart = refDate.with(TemporalAdjusters.firstDayOfMonth());
        LocalDateTime start = monthStart.atStartOfDay();
        return new Window(start, start.plusMonths(1));
      }
      case DAILY:
      default: {
        LocalDateTime start = refDate.atStartOfDay();
        return new Window(start, start.plusDays(1));
      }
    }
  }
}
