package com.revealprecision.revealserver.service.dashboard;


import static com.revealprecision.revealserver.constants.FormConstants.BUSINESS_STATUS;
import static com.revealprecision.revealserver.messaging.utils.DataStoreUtils.getQueryableStoreByWaiting;
import static com.revealprecision.revealserver.util.DashboardUtils.getBusinessStatusColor;
import static com.revealprecision.revealserver.util.DashboardUtils.getGeoNameDirectlyAboveStructure;
import static com.revealprecision.revealserver.util.DashboardUtils.getStringValueColumnData;

import com.revealprecision.revealserver.api.v1.dto.factory.LocationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.models.ColumnData;
import com.revealprecision.revealserver.api.v1.dto.models.RowData;
import com.revealprecision.revealserver.api.v1.dto.response.FeatureSetResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.messaging.message.LocationBusinessStatusAggregate;
import com.revealprecision.revealserver.messaging.message.LocationPersonBusinessStateAggregate;
import com.revealprecision.revealserver.messaging.message.LocationPersonBusinessStateCountAggregate;
import com.revealprecision.revealserver.messaging.message.LocationStructureBusinessStatusAggregate;
import com.revealprecision.revealserver.messaging.message.OperationalAreaVisitedCount;
import com.revealprecision.revealserver.messaging.message.PersonBusinessStatusAggregate;
import com.revealprecision.revealserver.messaging.message.TreatedOperationalAreaAggregate;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Report;
import com.revealprecision.revealserver.persistence.projection.PlanLocationDetails;
import com.revealprecision.revealserver.persistence.repository.ReportRepository;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import com.revealprecision.revealserver.service.PlanLocationsService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class IRSLiteDashboardService {

  public static final String SPRAY_PROGRESS_SPRAYED_TARGETED = "Spray Progress(Sprayed/Targeted)";
  public static final String NUMBER_OF_SPRAY_DAYS = "Number of Spray days";
  public static final String TOTAL_SUPERVISOR_FORMS_SUBMITTED = "Total supervisor forms submitted";
  public static final String AVERAGE_STRUCTURES_PER_DAY = "Average Structures Per Day";
  public static final String AVERAGE_INSECTICIDE_USAGE_RATE = "Average Insecticide Usage Rate";
  public static final String DATE_VISITED_FOR_IRS = "Date visited for IRS";
  public static final String MOBILIZED = "Mobilized";
  public static final String DATE_MOBILIZED = "Date Mobilized";
  public static final String NO = "No";
  private final StreamsBuilderFactoryBean getKafkaStreams;
  private final KafkaProperties kafkaProperties;
  private final PlanLocationsService planLocationsService;
  private final LocationRelationshipService locationRelationshipService;

  private static final String TOTAL_SPRAY_AREAS = "Total spray areas";
  private static final String TARGET_SPRAY_AREAS = "Targeted spray areas";
  private static final String VISITED_AREAS = "Total spray areas visited";
  public static final String SPRAY_COVERAGE_OF_TARGETED = "Spray coverage of targeted (Progress)";
  public static final String SPRAY_COVERAGE_OF_FOUND = "Spray coverage of Found(Sprayed/Found)";
  private static final String STRUCTURES_ON_THE_GROUND = "Structures on the ground";
  private static final String TOTAL_STRUCTURES_TARGETED = "Total Structures Targeted";
  private static final String STRUCTURES_FOUND = "Structures Found";
  private static final String STRUCTURES_SPRAYED = "Structures Sprayed";
  private static final String SPRAY_AREA_VISITED = "Spray Area Visited";

  ReadOnlyKeyValueStore<String, Long> countOfAssignedStructures;
  ReadOnlyKeyValueStore<String, Long> structureCounts;
  ReadOnlyKeyValueStore<String, Long> countOfLocationsByBusinessStatus;
  ReadOnlyKeyValueStore<String, LocationStructureBusinessStatusAggregate> countOfLocationStructuresByBusinessStatus;
  ReadOnlyKeyValueStore<String, LocationStructureBusinessStatusAggregate> countOfLocationStructuresByBusinessStatusInSprayArea;
  ReadOnlyKeyValueStore<String, OperationalAreaVisitedCount> countOfOperationalArea;
  ReadOnlyKeyValueStore<String, PersonBusinessStatusAggregate> personBusinessStatus;
  ReadOnlyKeyValueStore<String, LocationBusinessStatusAggregate> locationBusinessState;
  ReadOnlyKeyValueStore<String, LocationPersonBusinessStateCountAggregate> structurePeopleCounts;
  ReadOnlyKeyValueStore<String, TreatedOperationalAreaAggregate> treatedOperationalCounts;
  ReadOnlyKeyValueStore<String, LocationPersonBusinessStateAggregate> structurePeople;

  private final ReportRepository planReportRepository;

  boolean datastoresInitialized = false;


  public List<RowData> getIRSFullData(Plan plan, Location childLocation) {
    Report report = planReportRepository.findByPlanAndLocation(plan, childLocation).orElse(null);
    Map<String, ColumnData> columns = new LinkedHashMap<>();
    columns.put(TOTAL_SPRAY_AREAS,
        getTotalAreas(plan, childLocation, getGeoNameDirectlyAboveStructure(plan)));
    columns.put(TARGET_SPRAY_AREAS, getTargetedAreas(plan, childLocation));
    columns.put(VISITED_AREAS, operationalAreaVisitedCounts(plan, childLocation));
    columns.put(STRUCTURES_ON_THE_GROUND, getTotalStructuresCounts(plan, childLocation));
    columns.put(TOTAL_STRUCTURES_TARGETED, getTotalStructuresTargetedCount(plan, childLocation));
    columns.put(STRUCTURES_SPRAYED, getTotalStructuresSprayed(plan,
        childLocation));
    columns.put(SPRAY_PROGRESS_SPRAYED_TARGETED,
        getSPrayedProgressTargeted(plan, childLocation));
    columns.put(STRUCTURES_FOUND, getTotalStructuresFoundCount(plan, childLocation));
    columns.put(SPRAY_COVERAGE_OF_FOUND, getSprayCoverageOfFound(plan, childLocation));
    columns.put(NUMBER_OF_SPRAY_DAYS,
        getNumberOfSprayDays(report));
    columns.put(TOTAL_SUPERVISOR_FORMS_SUBMITTED,
        getSupervisorFormSubmissions(report));
    columns.put(AVERAGE_STRUCTURES_PER_DAY,
        getAverageStructuresSprayedPerDay(plan, childLocation, report));
    columns.put(AVERAGE_INSECTICIDE_USAGE_RATE,
        getAverageInsecticideUsage(plan, childLocation, report));

    RowData rowData = new RowData();
    rowData.setLocationIdentifier(childLocation.getIdentifier());
    rowData.setColumnDataMap(columns);
    rowData.setLocationName(childLocation.getName());
    return List.of(rowData);
  }

  private ColumnData getAverageInsecticideUsage(Plan plan, Location childLocation, Report report) {
    ColumnData columnData = new ColumnData();
    Double sprayedStructures = (Double) getTotalStructuresSprayed(plan, childLocation).getValue();
    Double insecticidesUsed = (Double) getInsecticidesUsed(report).getValue();

    if (insecticidesUsed == 0) {
      columnData.setValue(0d);
    } else {
      columnData.setValue(sprayedStructures / insecticidesUsed);
    }
    return columnData;
  }

  private ColumnData getInsecticidesUsed(Report report) {
    ColumnData columnData = new ColumnData();
    if (report != null && report.getReportIndicators().getInsecticidesUsed() != null) {
      columnData.setValue(report.getReportIndicators().getInsecticidesUsed());
    } else {
      columnData.setValue(0d);
    }
    return columnData;
  }

  private ColumnData getAverageStructuresSprayedPerDay(Plan plan, Location childLocation,
      Report report) {
    ColumnData columnData = new ColumnData();
    Double numberOfSprayDays = (Double) getNumberOfSprayDays(report).getValue();
    Double sprayedStructures = (Double) getTotalStructuresSprayed(plan, childLocation).getValue();

    if (numberOfSprayDays == 0) {
      columnData.setValue(0);
    } else {
      columnData.setValue(sprayedStructures / numberOfSprayDays);
    }

    return columnData;
  }

  private ColumnData getSPrayedProgressTargeted(Plan plan, Location childLocation) {
    ColumnData columnData = new ColumnData();
    Double totalSprayedStructures = (Double) getTotalStructuresSprayed(plan,
        childLocation).getValue();
    Double totalTargetedStructures = (Double) getTotalStructuresTargetedCount(plan,
        childLocation).getValue();

    if (totalTargetedStructures != 0) {
      columnData.setValue(totalSprayedStructures / totalTargetedStructures);
    } else {
      columnData.setValue(0);
    }
    return columnData;
  }

  private ColumnData getNumberOfSprayDays(Report report) {
    ColumnData columnData = new ColumnData();

    if (report != null && report.getReportIndicators().getUniqueSupervisionDates() != null) {
      columnData.setValue(
          report.getReportIndicators().getUniqueSupervisionDates().stream().count());
    } else {
      columnData.setValue(0d);
    }
    return columnData;
  }

  private ColumnData getSupervisorFormSubmissions(Report report) {
    ColumnData columnData = new ColumnData();
    if (report != null && report.getReportIndicators().getSupervisorFormSubmissionCount() != null) {
      columnData.setValue(report.getReportIndicators().getSupervisorFormSubmissionCount());
    } else {
      columnData.setValue(0d);
    }
    return columnData;
  }

  public List<RowData> getIRSFullDataOperational(Plan plan, Location childLocation) {
    Map<String, ColumnData> columns = new LinkedHashMap<>();
    Report report = planReportRepository.findByPlanAndLocation(plan, childLocation).orElse(null);
    columns.put(TOTAL_STRUCTURES_TARGETED, getTotalStructuresTargetedCount(plan, childLocation));
    columns.put(STRUCTURES_FOUND,
        getTotalStructuresFoundCountInSprayArea(plan, childLocation));
    columns.put(STRUCTURES_SPRAYED, getTotalStructuresSprayedCountInSprayArea(plan, childLocation));
    columns.put(SPRAY_AREA_VISITED, getAreaVisitedInSprayArea(plan, childLocation));
    columns.put(DATE_VISITED_FOR_IRS, getSprayDate(report));
    columns.put(STRUCTURES_ON_THE_GROUND, getTotalStructuresCounts(plan, childLocation));
    columns.put(MOBILIZED, getMobilized(report));
    columns.put(DATE_MOBILIZED, getMobilizedDate(report));
    RowData rowData = new RowData();
    rowData.setLocationIdentifier(childLocation.getIdentifier());
    rowData.setColumnDataMap(columns);
    rowData.setLocationName(childLocation.getName());
    return List.of(rowData);
  }

  public void initDataStoresIfNecessary() throws InterruptedException {
    if (!datastoresInitialized) {
      KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
      countOfAssignedStructures = getQueryableStoreByWaiting(kafkaStreams,
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.assignedStructureCountPerParent),
              QueryableStoreTypes.keyValueStore()));

      structureCounts = getQueryableStoreByWaiting(kafkaStreams,
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.structureCountPerParent),
              QueryableStoreTypes.keyValueStore()));

      countOfLocationsByBusinessStatus = getQueryableStoreByWaiting(kafkaStreams,
          StoreQueryParameters.fromNameAndType(kafkaProperties.getStoreMap()
                  .get(KafkaConstants.locationBusinessStatusByPlanParentHierarchy),
              QueryableStoreTypes.keyValueStore()));

      countOfOperationalArea = getQueryableStoreByWaiting(kafkaStreams,
          StoreQueryParameters.fromNameAndType(kafkaProperties.getStoreMap()
                  .get(KafkaConstants.operationalAreaByPlanParentHierarchy),
              QueryableStoreTypes.keyValueStore()));

      personBusinessStatus = getQueryableStoreByWaiting(kafkaStreams,
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.personBusinessStatus),
              QueryableStoreTypes.keyValueStore()));

      locationBusinessState = getQueryableStoreByWaiting(kafkaStreams,
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.locationBusinessStatus),
              QueryableStoreTypes.keyValueStore()));

      structurePeopleCounts = getQueryableStoreByWaiting(kafkaStreams,
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.structurePeopleCounts),
              QueryableStoreTypes.keyValueStore()));

      treatedOperationalCounts = getQueryableStoreByWaiting(kafkaStreams,
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.operationalTreatedCounts),
              QueryableStoreTypes.keyValueStore()));

      structurePeople = getQueryableStoreByWaiting(kafkaStreams,
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.structurePeople),
              QueryableStoreTypes.keyValueStore()));

      countOfLocationStructuresByBusinessStatus = getQueryableStoreByWaiting(
          kafkaStreams,
          StoreQueryParameters.fromNameAndType(kafkaProperties.getStoreMap()
                  .get(KafkaConstants.locationStructureHierarchyBusinessStatus),
              QueryableStoreTypes.keyValueStore()));

      countOfLocationStructuresByBusinessStatusInSprayArea = getQueryableStoreByWaiting(
          kafkaStreams,
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.locationStructureBusinessStatus),
              QueryableStoreTypes.keyValueStore()));
      datastoresInitialized = true;
    }
  }

  private ColumnData getSprayDate(Report report) {
    ColumnData columnData = getStringValueColumnData();
    if (report != null && report.getReportIndicators().getDateSprayed() != null) {
      columnData.setValue(report.getReportIndicators().getDateSprayed());
    }
    return columnData;
  }

  private ColumnData getMobilizedDate(Report report) {
    ColumnData columnData = getStringValueColumnData();
    if (report != null && report.getReportIndicators().getMobilizationDate() != null) {
      columnData.setValue(report.getReportIndicators().getMobilizationDate());
    }
    return columnData;
  }


  private ColumnData getMobilized(Report report) {
    ColumnData columnData = getStringValueColumnData();
    if (report != null && report.getReportIndicators().getMobilized() != null) {
      columnData.setValue(report.getReportIndicators().getMobilized());
    } else {
      columnData.setValue(NO);
    }
    return columnData;
  }

  private ColumnData getTotalStructuresSprayed(Plan plan, Location childLocation) {

    String sprayedLocationsQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_" + "Sprayed";
    LocationStructureBusinessStatusAggregate sprayedLocationsCountObj = countOfLocationStructuresByBusinessStatus.get(
        sprayedLocationsQueryKey);
    double sprayedLocationsCount = 0;
    if (sprayedLocationsCountObj != null) {
      sprayedLocationsCount = sprayedLocationsCountObj.getStructureSum();
    }

    ColumnData totalStructuresFoundColumnData = new ColumnData();
    totalStructuresFoundColumnData.setValue(sprayedLocationsCount);
    totalStructuresFoundColumnData.setIsPercentage(false);
    return totalStructuresFoundColumnData;

  }

  private ColumnData operationalAreaVisitedCounts(Plan plan, Location childLocation) {

    String sprayedLocationsQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy().getIdentifier() + "_" + "Sprayed";
    Long sprayedLocationsObj = countOfLocationsByBusinessStatus.get(sprayedLocationsQueryKey);
    double sprayedLocationsCount = 0;
    if (sprayedLocationsObj != null) {
      sprayedLocationsCount = sprayedLocationsObj;
    }

    String notSprayedLocationsQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy().getIdentifier() + "_" + "Not Sprayed";
    Long notSprayedLocationsObj = countOfLocationsByBusinessStatus.get(notSprayedLocationsQueryKey);
    double notSprayedLocationsCount = 0;
    if (notSprayedLocationsObj != null) {
      notSprayedLocationsCount = notSprayedLocationsObj;
    }

    double visitedAreas = sprayedLocationsCount + notSprayedLocationsCount;

    ColumnData operationalAreaVisitedColumnData = new ColumnData();
    operationalAreaVisitedColumnData.setValue(visitedAreas);
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

  private ColumnData getTotalStructuresCounts(Plan plan, Location childLocation) {
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
    Long notEligibleStructuresCountObj = countOfLocationsByBusinessStatus.get(
        notEligibleStructuresQueryKey);
    double notEligibleStructuresCount = 0;
    if (notEligibleStructuresCountObj != null) {
      notEligibleStructuresCount = notEligibleStructuresCountObj;
    }

    double totalStructuresExcludingNotEligible = totalStructuresCount - notEligibleStructuresCount;

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
    Long notEligibleStructuresCountObj = countOfLocationsByBusinessStatus.get(
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

  private ColumnData getTotalStructuresFoundCount(Plan plan, Location childLocation) {

    String sprayedLocationsQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_" + "Sprayed";
    LocationStructureBusinessStatusAggregate sprayedLocationsCountObj = countOfLocationStructuresByBusinessStatus.get(
        sprayedLocationsQueryKey);
    double sprayedLocationsCount = 0;
    if (sprayedLocationsCountObj != null) {
      sprayedLocationsCount = sprayedLocationsCountObj.getStructureSum();
    }

    String notSprayedLocationsQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_" + "Not Sprayed";
    LocationStructureBusinessStatusAggregate notSprayedLocationsCountObj = countOfLocationStructuresByBusinessStatus.get(
        notSprayedLocationsQueryKey);
    double notSprayedLocationsCount = 0;
    if (notSprayedLocationsCountObj != null) {
      notSprayedLocationsCount = notSprayedLocationsCountObj.getStructureSum();
    }

    double totalStructuresFound = sprayedLocationsCount + notSprayedLocationsCount;

    ColumnData totalStructuresFoundColumnData = new ColumnData();
    totalStructuresFoundColumnData.setValue(totalStructuresFound);
    totalStructuresFoundColumnData.setIsPercentage(false);
    return totalStructuresFoundColumnData;
  }

  private ColumnData getTotalStructuresFoundCountInSprayArea(Plan plan, Location childLocation) {

    String locationStructureBusinessStatusAggregateQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier();
    LocationStructureBusinessStatusAggregate locationStructureBusinessStatusAggregate = countOfLocationStructuresByBusinessStatusInSprayArea.get(
        locationStructureBusinessStatusAggregateQueryKey);
    double totalStructuresFound = 0;
    if (locationStructureBusinessStatusAggregate != null) {
      if (locationStructureBusinessStatusAggregate.getBusinessStatus().equals("Sprayed")
          || locationStructureBusinessStatusAggregate.getBusinessStatus().equals("Not Sprayed")) {
        if (locationStructureBusinessStatusAggregate.getStructureCounts() != null) {
          totalStructuresFound = locationStructureBusinessStatusAggregate.getStructureCounts();
        }

      }
    }

    ColumnData totalStructuresFoundColumnData = new ColumnData();
    totalStructuresFoundColumnData.setValue(totalStructuresFound);
    totalStructuresFoundColumnData.setIsPercentage(false);
    return totalStructuresFoundColumnData;
  }

  private ColumnData getTotalStructuresSprayedCountInSprayArea(Plan plan, Location childLocation) {

    String locationStructureBusinessStatusAggregateQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier();
    LocationStructureBusinessStatusAggregate locationStructureBusinessStatusAggregate = countOfLocationStructuresByBusinessStatusInSprayArea.get(
        locationStructureBusinessStatusAggregateQueryKey);
    double totalStructuresSprayed = 0;
    if (locationStructureBusinessStatusAggregate != null) {
      if (locationStructureBusinessStatusAggregate.getBusinessStatus().equals("Sprayed")) {
        if (locationStructureBusinessStatusAggregate.getStructureCounts() != null) {
          totalStructuresSprayed = locationStructureBusinessStatusAggregate.getStructureCounts();
        }

      }
    }

    ColumnData totalStructuresSprayedColumnData = new ColumnData();
    totalStructuresSprayedColumnData.setValue(totalStructuresSprayed);
    totalStructuresSprayedColumnData.setIsPercentage(false);
    return totalStructuresSprayedColumnData;
  }

  private ColumnData getAreaVisitedInSprayArea(Plan plan, Location childLocation) {

    String locationStructureBusinessStatusAggregateQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier();
    LocationStructureBusinessStatusAggregate locationStructureBusinessStatusAggregate = countOfLocationStructuresByBusinessStatusInSprayArea.get(
        locationStructureBusinessStatusAggregateQueryKey);
    String areaVisited = "no";
    if (locationStructureBusinessStatusAggregate != null) {
      if (!locationStructureBusinessStatusAggregate.getBusinessStatus().equals("Not Visited")) {
        areaVisited = "yes";
      }
    }

    ColumnData areaVisitedColumnData = new ColumnData();
    areaVisitedColumnData.setValue(areaVisited);
    areaVisitedColumnData.setIsPercentage(false);
    areaVisitedColumnData.setDataType("string");
    return areaVisitedColumnData;
  }


  private ColumnData getSprayCoverageOfFound(Plan plan, Location childLocation) {

    String sprayedLocationsQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_" + "Sprayed";
    LocationStructureBusinessStatusAggregate sprayedLocationsCountObj = countOfLocationStructuresByBusinessStatus.get(
        sprayedLocationsQueryKey);
    double sprayedLocationsCount = 0;
    if (sprayedLocationsCountObj != null) {
      sprayedLocationsCount = sprayedLocationsCountObj.getStructureSum();
    }

    String notSprayedLocationsQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_" + "Not Sprayed";
    LocationStructureBusinessStatusAggregate notSprayedLocationsCountObj = countOfLocationStructuresByBusinessStatus.get(
        notSprayedLocationsQueryKey);
    double notSprayedLocationsCount = 0;
    if (notSprayedLocationsCountObj != null) {
      notSprayedLocationsCount = notSprayedLocationsCountObj.getStructureSum();
    }

    double totalStructuresFound = sprayedLocationsCount + notSprayedLocationsCount;

    double sprayCoverageOfFound = 0;

    if (totalStructuresFound > 0) {
      sprayCoverageOfFound = sprayedLocationsCount / totalStructuresFound * 100;
    }

    ColumnData sprayCoverageOfFoundColumnData = new ColumnData();
    sprayCoverageOfFoundColumnData.setValue(sprayCoverageOfFound);
    sprayCoverageOfFoundColumnData.setMeta(
        "Total Structures Sprayed: " + sprayedLocationsCount + " / " + "Total Structures Found: "
            + totalStructuresFound);
    sprayCoverageOfFoundColumnData.setIsPercentage(true);
    return sprayCoverageOfFoundColumnData;
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
      String businessStatus = (String) rowDataMap.get(loc.getIdentifier()).getColumnDataMap()
          .get(BUSINESS_STATUS).getValue();
      loc.getProperties().setBusinessStatus(
          businessStatus);
      loc.getProperties().setStatusColor(getBusinessStatusColor(businessStatus));
    }).collect(Collectors.toList());
  }

  public FeatureSetResponse getFeatureSetResponse(UUID parentIdentifier,
      List<PlanLocationDetails> locationDetails, Map<UUID, RowData> rowDataMap) {
    FeatureSetResponse response = new FeatureSetResponse();
    response.setType("FeatureCollection");
    List<LocationResponse> locationResponses = locationDetails.stream()
        .map(loc -> LocationResponseFactory.fromPlanLocationDetails(loc, parentIdentifier))
        .collect(Collectors.toList());

    locationResponses = setGeoJsonProperties(rowDataMap, locationResponses);
    response.setFeatures(locationResponses);
    response.setIdentifier(parentIdentifier);
    return response;
  }
}
