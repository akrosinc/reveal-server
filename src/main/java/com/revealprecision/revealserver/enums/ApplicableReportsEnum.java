package com.revealprecision.revealserver.enums;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApplicableReportsEnum {
    IRS(List.of(ReportTypeEnum.IRS_FULL_COVERAGE.name())),
    MDA(List.of(ReportTypeEnum.MDA_FULL_COVERAGE.name(),ReportTypeEnum.MDA_FULL_COVERAGE_OPERATIONAL_AREA_LEVEL.name(),ReportTypeEnum.ONCHOCERCIASIS_SURVEY.name())),
    IRS_LITE(List.of(ReportTypeEnum.IRS_LITE_COVERAGE.name(),ReportTypeEnum.IRS_LITE_COVERAGE_OPERATIONAL_AREA_LEVEL.name())),
    MDA_LITE(List.of(ReportTypeEnum.MDA_LITE_COVERAGE.name())),
    SURVEY(List.of(ReportTypeEnum.SURVEY.name())),
    LSM(List.of(ReportTypeEnum.LSM_HOUSEHOLD_SURVEY.name(),ReportTypeEnum.LSM_HABITAT_SURVEY.name()));
    private final List<String> reportName;
}
