package com.revealprecision.revealserver.service.dashboard;


import static com.revealprecision.revealserver.messaging.utils.DataStoreUtils.getQueryableStoreByWaiting;
import static com.revealprecision.revealserver.util.DashboardUtils.getBusinessStatusColor;
import static com.revealprecision.revealserver.util.DashboardUtils.getGeoNameDirectlyAboveStructure;
import static com.revealprecision.revealserver.util.DashboardUtils.getLocationBusinessState;
import static com.revealprecision.revealserver.util.DashboardUtils.getStringValueColumnData;

import com.revealprecision.revealserver.api.v1.dto.factory.LocationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.models.ColumnData;
import com.revealprecision.revealserver.api.v1.dto.models.RowData;
import com.revealprecision.revealserver.api.v1.dto.response.FeatureSetResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.messaging.message.LocationPersonBusinessStateAggregate;
import com.revealprecision.revealserver.messaging.message.LocationPersonBusinessStateCountAggregate;
import com.revealprecision.revealserver.messaging.message.OperationalAreaVisitedCount;
import com.revealprecision.revealserver.messaging.message.PersonBusinessStatusAggregate;
import com.revealprecision.revealserver.messaging.message.TreatedOperationalAreaAggregate;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Report;
import com.revealprecision.revealserver.persistence.domain.ReportIndicators;
import com.revealprecision.revealserver.persistence.projection.PlanLocationDetails;
import com.revealprecision.revealserver.persistence.repository.ReportRepository;
import com.revealprecision.revealserver.props.DashboardProperties;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import com.revealprecision.revealserver.service.PlanLocationsService;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class IRSDashboardService {

  public static final String NOT_SPRAYED_REASON = "Not Sprayed Reason";
  public static final String PHONE_NUMBER = "Phone Number";
  public static final String NOT_VISITED = "Not Visited";
  public static final String N_A = "n/a";
  private final StreamsBuilderFactoryBean getKafkaStreams;
  private final KafkaProperties kafkaProperties;
  private final PlanLocationsService planLocationsService;
  private final LocationRelationshipService locationRelationshipService;
  private final DashboardProperties dashboardProperties;

  private static final String TOTAL_SPRAY_AREAS = "Total spray areas";
  private static final String TARGET_SPRAY_AREAS = "Targeted spray areas";
  private static final String VISITED_AREAS = "Total  Spray Areas Visited";
  public static final String SPRAY_COVERAGE_OF_TARGETED = "Spray Progress (Sprayed / Targeted)";
  private static final String TOTAL_STRUCTURES = "Total structures";
  private static final String TOTAL_STRUCTURES_TARGETED = "Total Structures Targeted";
  private static final String TOTAL_STRUCTURES_FOUND = "Total Structures Found";
  private static final String STRUCTURES_SPRAYED = "Total Structures Sprayed";
  private static final String PERCENTAGE_VISITED_EFFECTIVELY = "Spray Areas Effectively sprayed";
  private static final String STRUCTURE_STATUS = "Structure Status";
  private static final String NO_OF_ROOMS = "No of Rooms";
  private static final String NO_OF_MALES = "No of Males";
  private static final String NO_OF_FEMALES = "No of Females";
  private static final String NO_OF_PREGNANT_WOMEN = "No of Pregnant Women";
  private static final String FOUND_COVERAGE = "Found Coverage (Found/Target)";
  private static final String SPRAY_COVERAGE_OF_FOUND_STRUCTURES = "Spray Coverage of Found(Sprayed/Found)";
  private static final String STRUCTURES_REMAINING_TO_SPRAY_TO_REACH_90 = "Structures remaining to spray to reach 90% spray coverage";
  private static final String REVIEWED_WITH_DECISION = "Reviewed with decision";
  private static final String MOBILIZED = "Mobilized";

  ReadOnlyKeyValueStore<String, Long> countOfAssignedStructures;
  ReadOnlyKeyValueStore<String, Long> structureCounts;
  ReadOnlyKeyValueStore<String, Long> countOfStructuresByBusinessStatus;
  ReadOnlyKeyValueStore<String, OperationalAreaVisitedCount> countOfOperationalArea;
  ReadOnlyKeyValueStore<String, PersonBusinessStatusAggregate> personBusinessStatus;
  ReadOnlyKeyValueStore<String, LocationPersonBusinessStateCountAggregate> structurePeopleCounts;
  ReadOnlyKeyValueStore<String, TreatedOperationalAreaAggregate> treatedOperationalCounts;
  ReadOnlyKeyValueStore<String, LocationPersonBusinessStateAggregate> structurePeople;
  boolean isDatastoresInitialized = false;
  private final ReportRepository planReportRepository;

  public List<RowData> getIRSFullData(Plan plan, Location childLocation) {
    Report reportEntry = planReportRepository.findByPlanAndLocation(plan, childLocation)
        .orElse(null);
    Map<String, ColumnData> columns = new LinkedHashMap<>();
    columns.put(TOTAL_SPRAY_AREAS,
        getTotalAreas(plan, childLocation, getGeoNameDirectlyAboveStructure(plan)));
    columns.put(TARGET_SPRAY_AREAS, getTargetedAreas(plan, childLocation));
    columns.put(VISITED_AREAS, getOperationalAreaVisitedCounts(plan, childLocation));
    columns.put(PERCENTAGE_VISITED_EFFECTIVELY, getSprayedEffectively(plan, childLocation));
    columns.put(TOTAL_STRUCTURES, getTotalStructuresCounts(plan, childLocation, reportEntry));
    columns.put(TOTAL_STRUCTURES_TARGETED, getTotalStructuresTargetedCount(plan, childLocation));
    columns.put(STRUCTURES_SPRAYED, getTotalStructuresSprayed(reportEntry));
    columns.put(SPRAY_COVERAGE_OF_TARGETED, getSprayCoverageOfTargeted(plan, childLocation));
    columns.put(TOTAL_STRUCTURES_FOUND, getTotalStructuresFoundCount(reportEntry));
    columns.put(FOUND_COVERAGE, getFoundCoverage(plan, childLocation, reportEntry));
    columns.put(SPRAY_COVERAGE_OF_FOUND_STRUCTURES,
        getSprayCoverageFoundStructures(reportEntry));
    RowData rowData = new RowData();
    rowData.setLocationIdentifier(childLocation.getIdentifier());
    rowData.setColumnDataMap(columns);
    rowData.setLocationName(childLocation.getName());
    return List.of(rowData);
  }


  public List<RowData> getIRSFullDataOperational(Plan plan, Location childLocation) {
    Report reportEntry = planReportRepository.findByPlanAndLocation(plan, childLocation)
        .orElse(null);
    Map<String, ColumnData> columns = new LinkedHashMap<>();
    columns.put(TOTAL_STRUCTURES, getTotalStructuresCounts(plan, childLocation, reportEntry));
    columns.put(TOTAL_STRUCTURES_FOUND, getTotalStructuresFoundCount(reportEntry));
    columns.put(STRUCTURES_SPRAYED, getTotalStructuresSprayed(reportEntry));
    columns.put(SPRAY_COVERAGE_OF_TARGETED, getSprayCoverageOfTargeted(plan, childLocation));
    columns.put(FOUND_COVERAGE, getFoundCoverage(plan, childLocation, reportEntry));
    columns.put(SPRAY_COVERAGE_OF_FOUND_STRUCTURES,
        getSprayCoverageFoundStructures(reportEntry));
    columns.put(STRUCTURES_REMAINING_TO_SPRAY_TO_REACH_90,
        getStructuresRemainingToReach90(plan, childLocation, reportEntry));
    columns.put(REVIEWED_WITH_DECISION, getReviewedWithDecision(reportEntry));
    columns.put(MOBILIZED, getMobilized(reportEntry));
    RowData rowData = new RowData();
    rowData.setLocationIdentifier(childLocation.getIdentifier());
    rowData.setColumnDataMap(columns);
    rowData.setLocationName(childLocation.getName());
    return List.of(rowData);
  }

  public List<RowData> getIRSFullCoverageStructureLevelData(Plan plan, Location childLocation) {
    Map<String, ColumnData> columns = new HashMap<>();
    Report report = planReportRepository.findByPlanAndLocation(plan, childLocation).orElse(null);
    columns.put(STRUCTURE_STATUS,
        getLocationBusinessState(report));
    columns.put(NO_OF_MALES, getMales(report));
    columns.put(NO_OF_FEMALES, getFemales(report));
    columns.put(NO_OF_ROOMS, getRoomsSprayed(report));
    columns.put(NO_OF_PREGNANT_WOMEN, getPregnantWomen(report));
    columns.put(NOT_SPRAYED_REASON, getNotSprayedReason(report));
    columns.put(PHONE_NUMBER, getHeadPhoneNumber(report));
    RowData rowData = new RowData();
    rowData.setLocationIdentifier(childLocation.getIdentifier());
    rowData.setColumnDataMap(columns);
    rowData.setLocationName(childLocation.getName());
    return List.of(rowData);
  }


  public void initDataStoresIfNecessary() throws InterruptedException {
    if (!isDatastoresInitialized) {
      countOfAssignedStructures = getQueryableStoreByWaiting(getKafkaStreams.getKafkaStreams(),
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.assignedStructureCountPerParent),
              QueryableStoreTypes.keyValueStore()));

      structureCounts = getQueryableStoreByWaiting(getKafkaStreams.getKafkaStreams(),
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.structureCountPerParent),
              QueryableStoreTypes.keyValueStore()));

      countOfStructuresByBusinessStatus = getQueryableStoreByWaiting(
          getKafkaStreams.getKafkaStreams(),
          StoreQueryParameters.fromNameAndType(kafkaProperties.getStoreMap()
                  .get(KafkaConstants.locationBusinessStatusByPlanParentHierarchy),
              QueryableStoreTypes.keyValueStore()));

      countOfOperationalArea = getQueryableStoreByWaiting(getKafkaStreams.getKafkaStreams(),
          StoreQueryParameters.fromNameAndType(kafkaProperties.getStoreMap()
                  .get(KafkaConstants.operationalAreaByPlanParentHierarchy),
              QueryableStoreTypes.keyValueStore()));

      personBusinessStatus = getQueryableStoreByWaiting(getKafkaStreams.getKafkaStreams(),
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.personBusinessStatus),
              QueryableStoreTypes.keyValueStore()));

      structurePeopleCounts = getQueryableStoreByWaiting(getKafkaStreams.getKafkaStreams(),
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.structurePeopleCounts),
              QueryableStoreTypes.keyValueStore()));

      treatedOperationalCounts = getQueryableStoreByWaiting(getKafkaStreams.getKafkaStreams(),
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.operationalTreatedCounts),
              QueryableStoreTypes.keyValueStore()));

      structurePeople = getQueryableStoreByWaiting(getKafkaStreams.getKafkaStreams(),
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.structurePeople),
              QueryableStoreTypes.keyValueStore()));
      isDatastoresInitialized = true;
    }
  }


  private ColumnData getMobilized(Report report) {
    ColumnData columnData = getStringValueColumnData();
    if (report != null && report.getReportIndicators().getMobilized() != null) {
      columnData.setValue(report.getReportIndicators().getMobilized());
    } else {
      columnData.setValue("No");
    }
    return columnData;
  }


  private ColumnData getReviewedWithDecision(Report report) {
    ColumnData columnData = getStringValueColumnData();
    if (report != null) {
      columnData.setValue(report.getReportIndicators().isIrsDecisionFormFilled() ? "yes" : "no");
    }
    return columnData;
  }

  private ColumnData getStructuresRemainingToReach90(Plan plan, Location childLocation,
      Report report) {
    ColumnData columnData = new ColumnData();
    columnData.setIsPercentage(false);
    double sprayedStructures = (double) getTotalStructuresSprayed(report).getValue();
    double totalStructures = (double) getTotalStructuresCounts(plan, childLocation,
        report).getValue();
    double structuresRemaining = Math.round((totalStructures * 0.9) - sprayedStructures);
    if (structuresRemaining < 0) {
      columnData.setValue(0d);
    } else {
      columnData.setValue(structuresRemaining);
    }
    return columnData;
  }

  private ColumnData getSprayCoverageFoundStructures(Report report) {
    ColumnData columnData = new ColumnData();
    columnData.setIsPercentage(true);
    double sprayedStructures = (double) getTotalStructuresSprayed(report).getValue();
    double foundStructures = (double) getTotalStructuresFoundCount(report).getValue();
    if (foundStructures == 0) {
      columnData.setValue(0d);
    } else {
      columnData.setValue((sprayedStructures / foundStructures) * 100);
    }
    return columnData;
  }

  private ColumnData getFoundCoverage(Plan plan, Location childLocation, Report report) {
    ColumnData columnData = new ColumnData();
    columnData.setIsPercentage(true);
    double foundStructures = (double) getTotalStructuresFoundCount(report).getValue();
    double targetedStructures = (double) getTotalStructuresTargetedCount(plan,
        childLocation).getValue();
    if (targetedStructures == 0) {
      columnData.setValue(0d);
    } else {
      columnData.setValue((foundStructures / targetedStructures) * 100);
    }
    return columnData;
  }

  private ColumnData getHeadPhoneNumber(Report report) {
    ColumnData columnData = getStringValueColumnData();
    if (report != null && report.getReportIndicators().getPhoneNumber() != null) {
      ReportIndicators reportIndicators = report.getReportIndicators();
      columnData.setValue(reportIndicators.getPhoneNumber());
    }
    return columnData;
  }

  private ColumnData getNotSprayedReason(Report report) {
    ColumnData columnData = getStringValueColumnData();
    if (report != null && report.getReportIndicators().getNotSprayedReason() != null) {
      ReportIndicators reportIndicators = report.getReportIndicators();
      columnData.setValue(reportIndicators.getNotSprayedReason());
    } else {
      columnData.setValue(N_A);
    }
    return columnData;
  }

  private ColumnData getPregnantWomen(Report report) {
    ColumnData columnData = new ColumnData();
    if (report != null) {
      ReportIndicators reportIndicators = report.getReportIndicators();
      columnData.setValue(reportIndicators.getPregnantWomen());
    } else {
      columnData.setValue(0d);
    }
    return columnData;
  }

  private ColumnData getRoomsSprayed(Report report) {
    ColumnData columnData = new ColumnData();
    if (report != null) {
      ReportIndicators reportIndicators = report.getReportIndicators();
      columnData.setValue(reportIndicators.getSprayedRooms());
    } else {
      columnData.setValue(0d);
    }
    return columnData;
  }

  private ColumnData getFemales(Report report) {
    ColumnData columnData = new ColumnData();
    if (report != null) {
      ReportIndicators reportIndicators = report.getReportIndicators();
      columnData.setValue(reportIndicators.getFemales());
    } else {
      columnData.setValue(0d);
    }
    return columnData;
  }

  private ColumnData getMales(Report report) {
    ColumnData columnData = new ColumnData();
    if (report != null) {
      ReportIndicators reportIndicators = report.getReportIndicators();
      columnData.setValue(reportIndicators.getMales());
    } else {
      columnData.setValue(0d);
    }
    return columnData;
  }

  private ColumnData getTotalStructuresSprayed(Report report) {
    ColumnData columnData = new ColumnData();
    columnData.setValue(0d);
    if (report != null && report.getReportIndicators().getSprayedStructures() != null) {
      columnData.setValue(Double.valueOf(report.getReportIndicators().getSprayedStructures()));
    }
    return columnData;
  }

  private ColumnData getSprayedEffectively(Plan plan, Location childLocation) {

    String operationalAreaVisitedQueryKey =
        childLocation.getIdentifier() + "_" + plan.getIdentifier();
    OperationalAreaVisitedCount operationalAreaVisitedObj = countOfOperationalArea.get(
        operationalAreaVisitedQueryKey);
    double operationalAreaVisitedCount = 0;
    if (operationalAreaVisitedObj != null) {
      operationalAreaVisitedCount = operationalAreaVisitedObj.getOperationalAreaVisitedCountIRS();
    }

    String operationalAreaVisitedEffectivelyQueryKey =
        childLocation.getIdentifier() + "_" + plan.getIdentifier();
    OperationalAreaVisitedCount operationalAreaVisitedEffectivelyObj = countOfOperationalArea.get(
        operationalAreaVisitedEffectivelyQueryKey);
    double operationalAreaVisitedEffectivelyCount = 0;
    if (operationalAreaVisitedEffectivelyObj != null) {
      operationalAreaVisitedEffectivelyCount = operationalAreaVisitedEffectivelyObj.getOperationalAreaVisitedEffectivelyIRSCount();
    }

    double percentageSprayedEffectively = 0;
    if (operationalAreaVisitedCount > 0) {
      percentageSprayedEffectively =
          operationalAreaVisitedEffectivelyCount / operationalAreaVisitedCount * 100;
    }

    ColumnData percentageSprayedEffectivelyColumnData = new ColumnData();
    percentageSprayedEffectivelyColumnData.setValue(percentageSprayedEffectively);
    percentageSprayedEffectivelyColumnData.setIsPercentage(true);
    percentageSprayedEffectivelyColumnData.setMeta(
        "operationalAreaVisitedEffectivelyCount: " + operationalAreaVisitedEffectivelyCount + " / "
            + "operationalAreaVisitedCount: " + operationalAreaVisitedCount);

    return percentageSprayedEffectivelyColumnData;

  }

  private ColumnData getOperationalAreaVisitedCounts(Plan plan, Location childLocation) {
    String operationalAreaVisitedQueryKey =
        childLocation.getIdentifier() + "_" + plan.getIdentifier();
    OperationalAreaVisitedCount operationalAreaVisitedObj = countOfOperationalArea.get(
        operationalAreaVisitedQueryKey);
    double operationalAreaVisitedCount = 0;
    if (operationalAreaVisitedObj != null) {
      operationalAreaVisitedCount = operationalAreaVisitedObj.getOperationalAreaVisitedCountIRS();
    }
    ColumnData operationalAreaVisitedColumnData = new ColumnData();
    operationalAreaVisitedColumnData.setValue(operationalAreaVisitedCount);
    operationalAreaVisitedColumnData.setIsPercentage(false);
    return operationalAreaVisitedColumnData;
  }

  private ColumnData getTargetedAreas(Plan plan, Location childLocation) {

    Long countOfOperationalAreas = planLocationsService.getNumberOfAssignedChildrenByGeoLevelNameWithinLocationAndHierarchyAndPlan(
        plan.getIdentifier(), LocationConstants.OPERATIONAL, childLocation.getIdentifier(),
        plan.getLocationHierarchy().getIdentifier());

    Long countOfOperationalAreasValue = 0L;

    if (countOfOperationalAreas != null) {
      countOfOperationalAreasValue = countOfOperationalAreas;
    }

    ColumnData operationalAreaVisitedColumnData = new ColumnData();
    operationalAreaVisitedColumnData.setValue(countOfOperationalAreasValue);
    operationalAreaVisitedColumnData.setIsPercentage(false);
    return operationalAreaVisitedColumnData;
  }

  private ColumnData getTotalAreas(Plan plan, Location childLocation,
      String geoNameDirectlyAboveStructure) {

    Long totalOperationAreaCounts = locationRelationshipService.getNumberOfChildrenByGeoLevelNameWithinLocationAndHierarchy(
        geoNameDirectlyAboveStructure, childLocation.getIdentifier(),
        plan.getLocationHierarchy().getIdentifier());

    Long totalOperationAreaCountsValue = 0L;

    if (totalOperationAreaCounts != null) {
      totalOperationAreaCountsValue = totalOperationAreaCounts;
    }

    ColumnData operationalAreaVisitedColumnData = new ColumnData();
    operationalAreaVisitedColumnData.setValue(totalOperationAreaCountsValue);
    operationalAreaVisitedColumnData.setIsPercentage(false);
    return operationalAreaVisitedColumnData;
  }

  private ColumnData getTotalStructuresCounts(Plan plan, Location childLocation, Report report) {
    String totalStructuresQueryKey =
        plan.getLocationHierarchy().getIdentifier() + "_" + childLocation.getIdentifier();
    Long totalStructuresCountObj = structureCounts.get(totalStructuresQueryKey);
    double totalStructuresCount = 0;
    if (totalStructuresCountObj != null) {
      totalStructuresCount = totalStructuresCountObj;
    }

    String notEligibleStructuresQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy().getIdentifier() + "_" + "Not Eligible";
    Long notEligibleStructuresCountObj = countOfStructuresByBusinessStatus.get(
        notEligibleStructuresQueryKey);
    double notEligibleStructuresCount = 0;
    if (notEligibleStructuresCountObj != null) {
      notEligibleStructuresCount = notEligibleStructuresCountObj;
    }

    double discoveredStructuresCount =
        (report != null && report.getReportIndicators().getRegisteredStructures() != null)
            ? Double.valueOf(report.getReportIndicators().getRegisteredStructures()) : 0d;

    double totalStructuresExcludingNotEligible =
        totalStructuresCount + discoveredStructuresCount - notEligibleStructuresCount;

    ColumnData totalStructuresColumnData = new ColumnData();
    totalStructuresColumnData.setValue(totalStructuresExcludingNotEligible);
    totalStructuresColumnData.setIsPercentage(false);
    return totalStructuresColumnData;
  }

  private ColumnData getTotalStructuresTargetedCount(Plan plan, Location childLocation) {

    String totalStructuresTargetedQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier();
    Long totalStructuresTargetedCountObj = countOfAssignedStructures.get(
        totalStructuresTargetedQueryKey);
    double totalStructuresInPlanLocationCount = 0;
    if (totalStructuresTargetedCountObj != null) {
      totalStructuresInPlanLocationCount = totalStructuresTargetedCountObj;
    }

    String notEligibleStructuresQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy().getIdentifier() + "_" + "Not Eligible";
    Long notEligibleStructuresCountObj = countOfStructuresByBusinessStatus.get(
        notEligibleStructuresQueryKey);
    double notEligibleStructuresCount = 0;
    if (notEligibleStructuresCountObj != null) {
      notEligibleStructuresCount = notEligibleStructuresCountObj;
    }

    double totalStructuresInTargetedCount =
        totalStructuresInPlanLocationCount - notEligibleStructuresCount;

    ColumnData totalStructuresTargetedColumnData = new ColumnData();
    totalStructuresTargetedColumnData.setValue(totalStructuresInTargetedCount);
    totalStructuresTargetedColumnData.setIsPercentage(false);
    return totalStructuresTargetedColumnData;
  }

  private ColumnData getTotalStructuresFoundCount(Report report) {
    ColumnData columnData = new ColumnData();
    if (report != null && report.getReportIndicators().getFoundStructures() != null) {
      columnData.setValue(Double.valueOf(report.getReportIndicators().getFoundStructures()));
    } else {
      columnData.setValue(0d);
    }
    return columnData;
  }

  private ColumnData getSprayCoverageOfTargeted(Plan plan, Location childLocation) {

    String totalStructuresTargetedQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier();
    Long totalStructuresTargetedCountObj = countOfAssignedStructures.get(
        totalStructuresTargetedQueryKey);
    double totalStructuresInPlanLocationCount = 0;
    if (totalStructuresTargetedCountObj != null) {
      totalStructuresInPlanLocationCount = totalStructuresTargetedCountObj;
    }

    String notEligibleStructuresQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy().getIdentifier() + "_" + "Not Eligible";
    Long notEligibleStructuresCountObj = countOfStructuresByBusinessStatus.get(
        notEligibleStructuresQueryKey);
    double notEligibleStructuresCount = 0;
    if (notEligibleStructuresCountObj != null) {
      notEligibleStructuresCount = notEligibleStructuresCountObj;
    }

    double totalStructuresInTargetedCount =
        totalStructuresInPlanLocationCount - notEligibleStructuresCount;

    String completedStructuresQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy().getIdentifier() + "_" + "Complete";
    Long completedStructuresCountObj = countOfStructuresByBusinessStatus.get(
        completedStructuresQueryKey);
    double completedStructuresCount = 0;
    if (completedStructuresCountObj != null) {
      completedStructuresCount = completedStructuresCountObj;
    }

    double percentageOfSprayedToTargeted = 0;
    if (totalStructuresInTargetedCount > 0) {
      percentageOfSprayedToTargeted =
          completedStructuresCount / totalStructuresInTargetedCount * 100;
    }

    ColumnData totalStructuresTargetedColumnData = new ColumnData();
    totalStructuresTargetedColumnData.setValue(percentageOfSprayedToTargeted);
    totalStructuresTargetedColumnData.setMeta(
        "Total Structures Sprayed: " + completedStructuresCount + " / "
            + "Total Structures Targeted: " + totalStructuresInTargetedCount);
    totalStructuresTargetedColumnData.setIsPercentage(true);
    return totalStructuresTargetedColumnData;
  }


  public FeatureSetResponse getFeatureSetResponse(UUID parentIdentifier,
      List<PlanLocationDetails> locationDetails, Map<UUID, RowData> rowDataMap,
      String reportLevel) {
    FeatureSetResponse response = new FeatureSetResponse();
    response.setType("FeatureCollection");
    List<LocationResponse> locationResponses = locationDetails.stream()
        .map(loc -> LocationResponseFactory.fromPlanLocationDetails(loc, parentIdentifier))
        .collect(Collectors.toList());

    locationResponses = setGeoJsonProperties(rowDataMap, locationResponses);
    response.setDefaultDisplayColumn(
        dashboardProperties.getIrsDefaultDisplayColumns().getOrDefault(reportLevel, null));
    response.setFeatures(locationResponses);
    response.setIdentifier(parentIdentifier);
    return response;
  }


  private List<LocationResponse> setGeoJsonProperties(Map<UUID, RowData> rowDataMap,
      List<LocationResponse> locationResponses) {
    return locationResponses.stream().peek(loc -> {
      loc.getProperties().setColumnDataMap(rowDataMap.get(loc.getIdentifier()).getColumnDataMap());
      loc.getProperties().setId(loc.getIdentifier().toString());
      if (rowDataMap.get(loc.getIdentifier()).getColumnDataMap().get(SPRAY_COVERAGE_OF_TARGETED)
          != null) {
        loc.getProperties().setSprayCoverage(
            rowDataMap.get(loc.getIdentifier()).getColumnDataMap().get(SPRAY_COVERAGE_OF_TARGETED)
                .getValue());
      }
      if (rowDataMap.get(loc.getIdentifier()).getColumnDataMap()
          .get(STRUCTURE_STATUS) != null) {
        String businessStatus = (String) rowDataMap.get(loc.getIdentifier()).getColumnDataMap()
            .get(STRUCTURE_STATUS).getValue();
        loc.getProperties().setBusinessStatus(
            businessStatus);
        loc.getProperties().setStatusColor(getBusinessStatusColor(businessStatus));
      }

    }).collect(Collectors.toList());
  }
}


