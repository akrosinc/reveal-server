package com.revealprecision.revealserver.service.dashboard;

import com.revealprecision.revealserver.api.v1.dto.models.RowData;
import com.revealprecision.revealserver.api.v1.dto.response.FeatureSetResponse;
import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.enums.ApplicableReportsEnum;
import com.revealprecision.revealserver.enums.LookupUtil;
import com.revealprecision.revealserver.enums.ReportTypeEnum;
import com.revealprecision.revealserver.exceptions.WrongEnumException;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.projection.PlanLocationDetails;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.PlanService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class DashboardService {

  private final LocationService locationService;
  private final PlanService planService;
  private final MDADashboardService mdaDashboardService;
  private final IRSDashboardService irsDashboardService;
  private final IRSLiteDashboardService irsLiteDashboardService;
  private final MDALiteDashboardService mdaLiteDashboardService;

  public static final String WITHIN_STRUCTURE_LEVEL = "Within Structure";
  public static final String STRUCTURE_LEVEL = "Structure";
  public static final String DIRECTLY_ABOVE_STRUCTURE_LEVEL = "Directly Above Structure";
  public static final String ALL_OTHER_LEVELS = "All Other Levels";
  public static final String LOWEST_LITE_TOUCH_LEVEL = "Lowest Lite Touch Level";
  public static final String IS_ON_PLAN_TARGET = "In On Plan Target";
  public static final String SUPERVISOR_LEVEL = "Supervisor Level";
  public static final String CDD_LEVEL = "CDD Level";

  public FeatureSetResponse getDataForReport(String reportType, UUID planIdentifier,
      String parentIdentifierString, List<String> filters) {

    ReportTypeEnum reportTypeEnum = LookupUtil.lookup(ReportTypeEnum.class, reportType);
    Plan plan = planService.findPlanByIdentifier(planIdentifier);
    checkSupportedReports(reportType, planIdentifier, reportTypeEnum, plan);
    Location parentLocation = null;
    UUID parentIdentifier = null;

    if (parentIdentifierString != null) {
      try {
        parentIdentifier = UUID.fromString(parentIdentifierString);
      } catch (IllegalArgumentException illegalArgumentException) {
        parentIdentifier = UUID.fromString(parentIdentifierString.split("_")[2]);
      }
    }

    if (parentIdentifier != null) {
      parentLocation = locationService.findByIdentifier(parentIdentifier);
    }

    List<PlanLocationDetails> locationDetails = getPlanLocationDetails(
        planIdentifier, parentIdentifier, plan, parentLocation);

    try {
      initialDataStores(reportTypeEnum);
    } catch (Exception e) {
      e.printStackTrace();
    }

    String reportLevel = getReportLevel(plan, parentLocation, parentIdentifierString);

    Map<UUID, RowData> rowDataMap = locationDetails.stream().flatMap(loc -> Objects.requireNonNull(
                getRowData(loc.getParentLocation(), reportTypeEnum, plan, loc, reportLevel, filters,
                    parentIdentifierString))
            .stream()).filter(Objects::nonNull)
        .collect(Collectors.toMap(RowData::getLocationIdentifier, row -> row));

    return getFeatureSetResponse(parentIdentifier, locationDetails,
        rowDataMap, reportLevel,
        reportTypeEnum, filters);
  }

  private void checkSupportedReports(String reportType, UUID planIdentifier,
      ReportTypeEnum reportTypeEnum,
      Plan plan) {
    List<String> applicableReportTypes = ApplicableReportsEnum.valueOf(
        plan.getInterventionType().getCode()).getReportName();
    if (!applicableReportTypes.contains(reportTypeEnum.name())) {
      throw new WrongEnumException(
          "Report type: '" + reportType + "' is not applicable to plan with identifier: '"
              + planIdentifier + "'");
    }
  }

  private List<RowData> getRowData(Location parentLocation, ReportTypeEnum reportTypeEnum,
      Plan plan,
      PlanLocationDetails loc, String reportLevel, List<String> filters,
      String parentIdentifierString) {

    switch (reportTypeEnum) {
      case MDA_FULL_COVERAGE:

        switch (reportLevel) {
          case WITHIN_STRUCTURE_LEVEL:
            return mdaDashboardService.getMDAFullWithinStructureLevelData(plan, parentLocation);

          case STRUCTURE_LEVEL:
            return mdaDashboardService.getMDAFullCoverageStructureLevelData(plan,
                loc.getLocation(),
                parentLocation.getIdentifier());

          case DIRECTLY_ABOVE_STRUCTURE_LEVEL:
            return mdaDashboardService.getMDAFullCoverageOperationalAreaLevelData(plan,
                loc.getLocation());

          case ALL_OTHER_LEVELS:
            return mdaDashboardService.getMDAFullCoverageData(plan, loc.getLocation());
        }

      case IRS_FULL_COVERAGE:

        switch (reportLevel) {
          case WITHIN_STRUCTURE_LEVEL:
          case STRUCTURE_LEVEL:
            return irsDashboardService.getIRSFullCoverageStructureLevelData(plan,
                loc.getLocation());

          case DIRECTLY_ABOVE_STRUCTURE_LEVEL:
            return irsDashboardService.getIRSFullDataOperational(plan,
                loc.getLocation());

          case ALL_OTHER_LEVELS:
            return irsDashboardService.getIRSFullData(plan, loc.getLocation());
        }
      case IRS_LITE_COVERAGE:
        switch (reportLevel) {
          case DIRECTLY_ABOVE_STRUCTURE_LEVEL:
          case LOWEST_LITE_TOUCH_LEVEL:
          case IS_ON_PLAN_TARGET:
            return irsLiteDashboardService.getIRSFullDataOperational(plan,
                loc.getLocation());

          case ALL_OTHER_LEVELS:
            return irsLiteDashboardService.getIRSFullData(plan, loc.getLocation());
        }
      case MDA_LITE_COVERAGE:
        switch (reportLevel) {
          case CDD_LEVEL:
            return mdaLiteDashboardService.getMDALiteCDDCoverageData(
                plan,
                loc.getLocation(), filters, parentIdentifierString);
          case SUPERVISOR_LEVEL:
            return mdaLiteDashboardService.getMDALiteSupervisorCoverageData(
                plan,
                loc.getLocation(), filters);
          case IS_ON_PLAN_TARGET:
          case LOWEST_LITE_TOUCH_LEVEL:
          case ALL_OTHER_LEVELS:
            return mdaLiteDashboardService.getMDALiteCoverageData(
                plan,
                loc.getLocation(), filters);

        }

    }
    return null;
  }

  public FeatureSetResponse getFeatureSetResponse(UUID parentIdentifier,
      List<PlanLocationDetails> locationDetails,
      Map<UUID, RowData> rowDataMap, String reportLevel, ReportTypeEnum reportTypeEnum,
      List<String> filters) {
    switch (reportTypeEnum) {

      case MDA_FULL_COVERAGE:
        return mdaDashboardService.getFeatureSetResponse(parentIdentifier, locationDetails,
            rowDataMap, reportLevel);

      case IRS_FULL_COVERAGE:
        return irsDashboardService.getFeatureSetResponse(parentIdentifier, locationDetails,
            rowDataMap, reportLevel);

      case IRS_LITE_COVERAGE:
        return irsLiteDashboardService.getFeatureSetResponse(parentIdentifier, locationDetails,
            rowDataMap, reportLevel);
      case MDA_LITE_COVERAGE:
        return mdaLiteDashboardService.getFeatureSetResponse(parentIdentifier, locationDetails,
            rowDataMap, reportLevel, filters);


    }
    return null;
  }

  private void initialDataStores(ReportTypeEnum reportTypeEnum) throws InterruptedException {
    switch (reportTypeEnum) {
      case MDA_FULL_COVERAGE:
        mdaDashboardService.initDataStoresIfNecessary();
        break;
      case IRS_FULL_COVERAGE:
        irsDashboardService.initDataStoresIfNecessary();
        break;
      case IRS_LITE_COVERAGE:
        irsLiteDashboardService.initDataStoresIfNecessary();
        break;
      case MDA_LITE_COVERAGE:
        mdaLiteDashboardService.initDataStoresIfNecessary();
        break;
    }
  }

  private List<PlanLocationDetails> getPlanLocationDetails(UUID planIdentifier,
      UUID parentIdentifier, Plan plan, Location parentLocation) {
    List<PlanLocationDetails> locationDetails = new ArrayList<>();
    if (parentLocation == null ||
        !parentLocation.getGeographicLevel().getName().equals(LocationConstants.STRUCTURE)) {

      if (parentIdentifier == null) {
        locationDetails.add(locationService.getRootLocationByPlanIdentifier(planIdentifier));
      } else {

        int structureNodeIndex = plan.getLocationHierarchy().getNodeOrder()
            .indexOf(LocationConstants.STRUCTURE);
        int locationNodeIndex = plan.getLocationHierarchy().getNodeOrder()
            .indexOf(parentLocation.getGeographicLevel().getName());
        if (locationNodeIndex + 1 < structureNodeIndex) {
          locationDetails = locationService.getAssignedLocationsByParentIdentifierAndPlanIdentifier(
              parentIdentifier, planIdentifier, (locationNodeIndex + 2) == structureNodeIndex);
        } else {

          locationDetails = locationService.getLocationsByParentIdentifierAndPlanIdentifier(
              parentIdentifier, planIdentifier);
        }
      }
    } else {
      PlanLocationDetails planLocations = new PlanLocationDetails();
      planLocations.setParentLocation(parentLocation);
      planLocations.setLocation(parentLocation);
      planLocations.setHasChildren(false);
      planLocations.setAssignedLocations(0L);
      planLocations.setChildrenNumber(0L);
      planLocations.setAssignedTeams(0L);
      locationDetails.add(planLocations);
    }
    return locationDetails;
  }


  private String getReportLevel(Plan plan, Location parentLocation, String parentIdentifierString) {

    String parentOfGeoLevelDirectlyAboveStructure = null;
    if (plan.getLocationHierarchy().getNodeOrder().contains(LocationConstants.STRUCTURE)) {
      parentOfGeoLevelDirectlyAboveStructure = plan.getLocationHierarchy().getNodeOrder().get(
          plan.getLocationHierarchy().getNodeOrder().indexOf(LocationConstants.STRUCTURE) - 2);
    }

    String geoLevelDirectlyAboveStructure = null;
    if (plan.getLocationHierarchy().getNodeOrder().contains(LocationConstants.STRUCTURE)) {
      geoLevelDirectlyAboveStructure = plan.getLocationHierarchy().getNodeOrder().get(
          plan.getLocationHierarchy().getNodeOrder().indexOf(LocationConstants.STRUCTURE) - 1);
    }

    String geoLevelDirectlyAbovePlanTarget = null;
    if (plan.getLocationHierarchy().getNodeOrder()
        .contains(plan.getPlanTargetType().getGeographicLevel().getName())) {
      geoLevelDirectlyAbovePlanTarget = plan.getLocationHierarchy().getNodeOrder().get(
          plan.getLocationHierarchy().getNodeOrder()
              .indexOf(plan.getPlanTargetType().getGeographicLevel().getName()) - 1);
    }

    boolean containsStructure = plan.getLocationHierarchy().getNodeOrder()
        .contains(LocationConstants.STRUCTURE);

    String lowestLevel = plan.getLocationHierarchy().getNodeOrder()
        .get(plan.getLocationHierarchy().getNodeOrder().size() - 1);

    if (parentIdentifierString != null && parentIdentifierString.contains("SUPERVISOR")) {
      return SUPERVISOR_LEVEL;
    } else if (parentIdentifierString != null && parentIdentifierString.contains("CDD")) {
      return CDD_LEVEL;
    } else {
      if (parentLocation == null) {
        return ALL_OTHER_LEVELS;
      } else {
        if (containsStructure) {
          if (parentLocation.getGeographicLevel().getName().equals(LocationConstants.STRUCTURE)) {
            return WITHIN_STRUCTURE_LEVEL;
          } else if (geoLevelDirectlyAboveStructure != null) {
            String reportLevel = ALL_OTHER_LEVELS;

            if (parentLocation.getGeographicLevel().getName()
                .equals(geoLevelDirectlyAboveStructure)) {
              reportLevel = STRUCTURE_LEVEL;
            }
            if (parentLocation.getGeographicLevel().getName()
                .equals(parentOfGeoLevelDirectlyAboveStructure)) {
              if (parentLocation.getGeographicLevel().getName()
                  .equals(geoLevelDirectlyAbovePlanTarget)) {
                reportLevel = IS_ON_PLAN_TARGET;
              } else {
                reportLevel = DIRECTLY_ABOVE_STRUCTURE_LEVEL;
              }
            }
            return reportLevel;
          } else {
            return ALL_OTHER_LEVELS;
          }
        } else {
          if (parentLocation.getGeographicLevel().getName().equals(lowestLevel)) {
            return LOWEST_LITE_TOUCH_LEVEL;
          } else {
            return ALL_OTHER_LEVELS;
          }
        }
      }
    }
  }
}
