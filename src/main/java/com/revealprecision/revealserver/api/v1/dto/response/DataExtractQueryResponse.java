package com.revealprecision.revealserver.api.v1.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataExtractQueryResponse {

    private UUID id;

    private UUID planIdentifier;

    private String queryLabel;

}
