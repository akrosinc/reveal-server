package com.revealprecision.revealserver.schedule;

import com.revealprecision.revealserver.model.ActionTemplateConfig;
import com.revealprecision.revealserver.model.ActionTemplateConfig.TaskGenerationRecurrence;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.ActionLocationRecaptureLimit;
import com.revealprecision.revealserver.persistence.domain.ActionTemplate;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.repository.TaskRepository;
import com.revealprecision.revealserver.props.TaskGenerationScheduleProperties;
import com.revealprecision.revealserver.service.ActionLocationRecaptureLimitService;
import com.revealprecision.revealserver.service.TaskService;
import com.revealprecision.revealserver.util.RecurrenceWindowUtil;
import com.revealprecision.revealserver.util.RecurrenceWindowUtil.Window;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily schedule that generates tasks from the {@code action_location_recapture_limit} table.
 *
 * <p>For each recapture limit entry a task is created for the entry's action targeting its
 * location, with the entry's {@code count} stored as the {@code limitCounter} in the task's
 * {@code details} jsonb column. The {@code limitCounter} indicates how many times the generated
 * task may be executed before it is closed.</p>
 *
 * <p>The cron expression is supplied by {@link
 * com.revealprecision.revealserver.props.TaskGenerationScheduleProperties} and defaults to a daily
 * run.</p>
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class TaskGenerationSchedule {

  private final ActionLocationRecaptureLimitService actionLocationRecaptureLimitService;
  private final TaskService taskService;
  private final TaskRepository taskRepository;
  private final TaskGenerationScheduleProperties taskGenerationScheduleProperties;

  @Scheduled(cron = "#{taskGenerationScheduleProperties.recaptureLimitSchedule}")
  public void generateRecaptureLimitTasks() {
    log.info("TASK_GENERATION recapture-limit schedule start");

    List<ActionLocationRecaptureLimit> recaptureLimits = actionLocationRecaptureLimitService.getAll();

    log.info("TASK_GENERATION recapture-limit schedule: {} recapture limit(s) to process",
        recaptureLimits.size());

    LocalDateTime now = LocalDateTime.now();

    for (ActionLocationRecaptureLimit recaptureLimit : recaptureLimits) {
      try {
        Action action = recaptureLimit.getAction();
        Location location = recaptureLimit.getLocation();

        if (action == null || location == null) {
          log.warn("TASK_GENERATION skipping recapture limit {}: missing action or location",
              recaptureLimit.getIdentifier());
          continue;
        }

        // Task generation requires an action template with a config to know the recurrence - skip
        // entirely when either is missing rather than assuming a default cadence.
        TaskGenerationRecurrence recurrence = resolveRecurrence(action);
        if (recurrence == null) {
          log.info(
              "TASK_GENERATION skipping recapture limit {}: action {} has no action template / "
                  + "action template config, so no recurrence is configured",
              recaptureLimit.getIdentifier(), action.getIdentifier());
          continue;
        }

        // The schedule runs daily, but the task is only generated once per the action template's
        // recurrence window (minutely / hourly / daily / weekly / monthly). Resolve the window for
        // "now" and skip if a task for this action + location already exists within it.
        Window window = RecurrenceWindowUtil.windowFor(recurrence, now);

        boolean alreadyGenerated = taskRepository.existsForActionAndLocationInWindow(
            action.getIdentifier(), location.getIdentifier(), window.getStart(), window.getEnd());

        if (alreadyGenerated) {
          log.info(
              "TASK_GENERATION skipping recapture limit {}: task already exists for action {} and location {} in {} window [{} - {})",
              recaptureLimit.getIdentifier(), action.getIdentifier(), location.getIdentifier(),
              recurrence, window.getStart(), window.getEnd());
          continue;
        }

        taskService.generateRecaptureLimitTask(action, location, recaptureLimit.getCount(),
            taskGenerationScheduleProperties.getRecaptureLimitTaskOwner());

      } catch (Exception e) {
        log.error("TASK_GENERATION failed to generate task for recapture limit {}: {}",
            recaptureLimit.getIdentifier(), e.getMessage(), e);
      }
    }

    log.info("TASK_GENERATION recapture-limit schedule end");
  }

  /**
   * Resolves the task-generation recurrence from the action's linked action template config.
   * Returns {@code null} when the action has no action template, the template has no config, or
   * the config has no recurrence - in which case no task should be generated.
   */
  private TaskGenerationRecurrence resolveRecurrence(Action action) {
    ActionTemplate template = action.getActionTemplate();
    if (template == null) {
      return null;
    }
    ActionTemplateConfig config = template.getActionTemplateConfig();
    if (config == null) {
      return null;
    }
    return config.getRecurrence();
  }
}
