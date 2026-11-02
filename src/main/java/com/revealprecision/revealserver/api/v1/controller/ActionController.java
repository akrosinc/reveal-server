package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.ActionResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.response.ActionResponse;
import com.revealprecision.revealserver.enums.ActionTitleEnum;
import com.revealprecision.revealserver.service.ActionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/action")
@RequiredArgsConstructor
public class ActionController {

  private static final String FORM_IDENTIFIER = "formIdentifier";
  private static final String ACTION_TEMPLATE_IDENTIFIER = "actionTemplateIdentifier";

  private final ActionService actionService;

  @Operation(summary = "Fetch all action titles",
      description = "Returns the list of available action titles from ActionTitleEnum.",
      tags = {"Action"}
  )
  @GetMapping("/actionTitles")
  public ResponseEntity<List<String>> getAllActionTitles() {
    return ResponseEntity.ok(
        Stream.of(ActionTitleEnum.values()).map(ActionTitleEnum::getActionTitle)
            .collect(Collectors.toList()));
  }

  @Operation(summary = "Update an action's form and action template references",
      description = "Partially updates the form and/or action template on an action, by action id. "
          + "An omitted field is left unchanged; a field explicitly set to null clears that "
          + "reference; a non-null value sets it.",
      tags = {"Action"}
  )
  @PatchMapping("/{actionIdentifier}/references")
  public ResponseEntity<ActionResponse> updateReferences(
      @Parameter(description = "Action identifier") @PathVariable UUID actionIdentifier,
      @RequestBody final Map<String, Object> request) {
    boolean applyForm = request.containsKey(FORM_IDENTIFIER);
    boolean applyActionTemplate = request.containsKey(ACTION_TEMPLATE_IDENTIFIER);
    UUID formIdentifier = toUuid(request.get(FORM_IDENTIFIER), FORM_IDENTIFIER);
    UUID actionTemplateIdentifier =
        toUuid(request.get(ACTION_TEMPLATE_IDENTIFIER), ACTION_TEMPLATE_IDENTIFIER);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ActionResponseFactory.fromEntity(
            actionService.updateReferences(actionIdentifier,
                applyForm, formIdentifier,
                applyActionTemplate, actionTemplateIdentifier)));
  }

  private static UUID toUuid(Object value, String field) {
    if (value == null) {
      return null;
    }
    try {
      return UUID.fromString(value.toString());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          String.format("%s must be a valid UUID or null", field));
    }
  }
}
