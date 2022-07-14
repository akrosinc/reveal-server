package com.revealprecision.revealserver.enums;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApplicableReportsEnum {
    IRS(List.of(ReportTypeEnum.IRS_FULL_COVERAGE.name())),
    MDA(List.of(ReportTypeEnum.MDA_FULL_COVERAGE.name(),ReportTypeEnum.MDA_FULL_COVERAGE_OPERATIONAL_AREA_LEVEL.name())),
    IRS_LITE(List.of(ReportTypeEnum.IRS_LITE_COVERAGE.name(),ReportTypeEnum.IRS_LITE_COVERAGE_OPERATIONAL_AREA_LEVEL.name())),
    MDA_LITE(List.of(ReportTypeEnum.MDA_LITE_COVERAGE.name()));

    private final List<String> reportName;
}
