package com.revealprecision.revealserver.exceptions.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class ValidationErrorResponse {

    private String field;
    private String rejectedValue;
    private String messageKey;
}
