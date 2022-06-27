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

  public static final String WITHIN_STRUCTURE_LEVEL = "Within Structure";
  public static final String STRUCTURE_LEVEL = "Structure";
  public static final String DIRECTLY_ABOVE_STRUCTURE_LEVEL = "Directly Above Structure";
  public static final String ALL_OTHER_LEVELS = "All Other Levels";

  public FeatureSetResponse getDataForReport(String reportType, UUID planIdentifier,
      UUID parentIdentifier) {

    ReportTypeEnum reportTypeEnum = LookupUtil.lookup(ReportTypeEnum.class, reportType);
    Plan plan = planService.findPlanByIdentifier(planIdentifier);
    List<String> applicableReportTypes = ApplicableReportsEnum.valueOf(
        plan.getInterventionType().getCode()).getReportName();
    if (!applicableReportTypes.contains(reportTypeEnum.name())) {
      throw new WrongEnumException(
          "Report type: '" + reportType + "' is not applicable to plan with identifier: '"
              + planIdentifier + "'");
    }
    Location parentLocation = null;
    if (parentIdentifier != null) {
      parentLocation = locationService.findByIdentifier(parentIdentifier);
    }

    List<PlanLocationDetails> locationDetails = getPlanLocationDetails(
        planIdentifier, parentIdentifier, plan, parentLocation);

    initialDataStores(reportTypeEnum);

    String reportLevel = getReportLevel(plan, parentLocation);

    Map<UUID, RowData> rowDataMap = locationDetails.stream().flatMap(loc -> Objects.requireNonNull(
                getRowData(loc.getParentLocation(), reportTypeEnum, plan, loc, reportLevel))
            .stream()).filter(Objects::nonNull)
        .collect(Collectors.toMap(RowData::getLocationIdentifier, row -> row));

    return getFeatureSetResponse(parentIdentifier, locationDetails, rowDataMap, reportLevel,
        reportTypeEnum);
  }

  private List<RowData> getRowData(Location parentLocation, ReportTypeEnum reportTypeEnum,
      Plan plan,
      PlanLocationDetails loc, String reportLevel) {

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
                loc.getLocation(),
                parentLocation.getIdentifier());

          case DIRECTLY_ABOVE_STRUCTURE_LEVEL:
            return irsDashboardService.getIRSFullDataOperational(plan,
                loc.getLocation());

          case ALL_OTHER_LEVELS:
            return irsDashboardService.getIRSFullData(plan, loc.getLocation());
        }

    }
    return null;
  }

  public FeatureSetResponse getFeatureSetResponse(UUID parentIdentifier,
      List<PlanLocationDetails> locationDetails,
      Map<UUID, RowData> rowDataMap, String reportLevel, ReportTypeEnum reportTypeEnum) {
    switch (reportTypeEnum) {

      case MDA_FULL_COVERAGE:
        return mdaDashboardService.getFeatureSetResponse(parentIdentifier, locationDetails,
            rowDataMap, reportLevel);

      case IRS_FULL_COVERAGE:
        return irsDashboardService.getFeatureSetResponse(parentIdentifier, locationDetails,
            rowDataMap, reportLevel);
    }
    return null;
  }


  private void initialDataStores(ReportTypeEnum reportTypeEnum) {
    switch (reportTypeEnum) {

      case MDA_FULL_COVERAGE:
        mdaDashboardService.initDataStoresIfNecessary();
        ;
        break;
      case IRS_FULL_COVERAGE:
        irsDashboardService.initDataStoresIfNecessary();
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


  private String getReportLevel(Plan plan, Location parentLocation) {

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

    if (parentLocation == null) {
      return ALL_OTHER_LEVELS;
    } else {
      if (parentLocation.getGeographicLevel().getName().equals(LocationConstants.STRUCTURE)) {
        return WITHIN_STRUCTURE_LEVEL;
      } else if (geoLevelDirectlyAboveStructure != null) {
        String reportLevel = ALL_OTHER_LEVELS;

        if (parentLocation.getGeographicLevel().getName().equals(geoLevelDirectlyAboveStructure)) {
          reportLevel = STRUCTURE_LEVEL;
        }
        if (parentLocation.getGeographicLevel().getName()
            .equals(parentOfGeoLevelDirectlyAboveStructure)) {
          reportLevel = DIRECTLY_ABOVE_STRUCTURE_LEVEL;
        }
        return reportLevel;
      } else {
        return ALL_OTHER_LEVELS;
      }
    }

  }


}
