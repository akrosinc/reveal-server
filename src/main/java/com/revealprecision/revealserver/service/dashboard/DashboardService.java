package com.revealprecision.revealserver.service.dashboard;

import com.revealprecision.revealserver.api.v1.dto.factory.LocationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.models.RowData;
import com.revealprecision.revealserver.api.v1.dto.response.FeatureSetResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.enums.ApplicableReportsEnum;
import com.revealprecision.revealserver.enums.LookupUtil;
import com.revealprecision.revealserver.enums.ReportTypeEnum;
import com.revealprecision.revealserver.exceptions.WrongEnumException;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.projection.PlanLocationDetails;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.PlanService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class DashboardService {

  private final LocationService locationService;
  private final PlanService planService;
  private final MDADashboardService mdaDashboardService;
  private final IRSDashboardService irsDashboardService;

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



    switch (reportTypeEnum) {

      case MDA_FULL_COVERAGE:
        mdaDashboardService.initDataStoresIfNecessary();;
        break;
      case IRS_FULL_COVERAGE:
        irsDashboardService.initDataStoresIfNecessary();
    }

    Map<UUID, RowData> rowDataMap = locationDetails.stream().flatMap(loc -> Objects.requireNonNull(
                getRowData(loc.getParentLocation(), reportTypeEnum, plan, loc))
            .stream()).filter(Objects::nonNull)
        .collect(Collectors.toMap(RowData::getLocationIdentifier, row -> row));

    FeatureSetResponse response = new FeatureSetResponse();
    response.setType("FeatureCollection");
    List<LocationResponse> locationResponses = locationDetails.stream()
        .map(loc -> LocationResponseFactory.fromPlanLocationDetails(loc, parentIdentifier))
        .collect(Collectors.toList());

    switch (reportTypeEnum) {

      case MDA_FULL_COVERAGE:
        locationResponses = mdaDashboardService.setGeoJsonProperties(rowDataMap, locationResponses);
        break;
      case IRS_FULL_COVERAGE:
        locationResponses = irsDashboardService.setGeoJsonProperties(rowDataMap, locationResponses);
    }

    response.setFeatures(locationResponses);
    response.setIdentifier(parentIdentifier);
    return response;
  }


  private List<RowData> getRowData(Location parentLocation, ReportTypeEnum reportTypeEnum,
      Plan plan,
      PlanLocationDetails loc) {
    switch (reportTypeEnum) {

      case MDA_FULL_COVERAGE:
        if (loc.isHasChildren()) {
          switch (loc.getLocation().getGeographicLevel().getName()) {
            case LocationConstants.STRUCTURE:
              return mdaDashboardService.getMDAFullCoverageStructureLevelData(plan,
                  loc.getLocation(),
                  parentLocation.getIdentifier());
            case LocationConstants.OPERATIONAL:
              return mdaDashboardService.getMDAFullCoverageOperationalAreaLevelData(plan,
                  loc.getLocation());
            default:
              return mdaDashboardService.getMDAFullCoverageData(plan, loc.getLocation());
          }
        } else {
          return mdaDashboardService.getMDAFullWithinStructureLevelData(plan, parentLocation);
        }

      case IRS_FULL_COVERAGE:
      if (loc.isHasChildren()) {
        switch (loc.getLocation().getGeographicLevel().getName()) {
          case LocationConstants.STRUCTURE:
            return irsDashboardService.getIRSFullCoverageStructureLevelData(plan,
                loc.getLocation(),
                parentLocation.getIdentifier());
          case LocationConstants.OPERATIONAL:
            return irsDashboardService.getIRSFullDataOperational(plan,
                loc.getLocation());
          default:
            return irsDashboardService.getIRSFullData(plan, loc.getLocation());
        }
      } else {
        return irsDashboardService.getIRSFullCoverageStructureLevelData(plan,
            loc.getLocation(),
            parentLocation.getIdentifier());
      }


    }
    return null;
  }


}
