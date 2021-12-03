package com.revealprecision.revealserver.api.dto.response;

import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeographicLevelResponse {

    private UUID identifier;
    private String title;
    private String name;
}
