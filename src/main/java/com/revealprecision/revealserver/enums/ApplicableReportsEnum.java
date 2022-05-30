package com.revealprecision.revealserver.enums;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApplicableReportsEnum {
    IRS(List.of(ReportTypeEnum.IRS_FULL_COVERAGE.name())),
    MDA(List.of(ReportTypeEnum.MDA_FULL_COVERAGE.name(),ReportTypeEnum.MDA_FULL_COVERAGE_OPERATIONAL_AREA_LEVEL.name()));

    private final List<String> reportName;
}
