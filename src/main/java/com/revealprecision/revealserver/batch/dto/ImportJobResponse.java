package com.revealprecision.revealserver.batch.dto;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ImportJobResponse {

    private Integer executedCommits;
    private Integer executedWrittings;
    private Integer skippedWritings;
    private String status;

}