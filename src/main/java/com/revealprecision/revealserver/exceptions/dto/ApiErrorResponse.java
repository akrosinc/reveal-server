package com.revealprecision.revealserver.exceptions.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private int statusCode;
    private LocalDateTime timestamp;
    private String message;
    private List<ValidationErrorResponse> fieldValidationErrors;
}
