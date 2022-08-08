package com.revealprecision.revealserver.service.dashboard;


import static com.revealprecision.revealserver.messaging.utils.DataStoreUtils.getQueryableStoreByWaiting;

import com.revealprecision.revealserver.api.v1.dto.factory.LocationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.models.ColumnData;
import com.revealprecision.revealserver.api.v1.dto.models.RowData;
import com.revealprecision.revealserver.api.v1.dto.response.FeatureSetResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.messaging.message.FormObservationsEvent;
import com.revealprecision.revealserver.messaging.message.LocationBusinessStatusAggregate;
import com.revealprecision.revealserver.messaging.message.LocationPersonBusinessStateAggregate;
import com.revealprecision.revealserver.messaging.message.LocationPersonBusinessStateCountAggregate;
import com.revealprecision.revealserver.messaging.message.OperationalAreaVisitedCount;
import com.revealprecision.revealserver.messaging.message.PersonBusinessStatusAggregate;
import com.revealprecision.revealserver.messaging.message.TreatedOperationalAreaAggregate;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.projection.PlanLocationDetails;
import com.revealprecision.revealserver.props.DashboardProperties;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import com.revealprecision.revealserver.service.PlanLocationsService;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
  private static final String SPRAY_COVERAGE = "Spray Coverage (Effectiveness)";
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
  ReadOnlyKeyValueStore<String, LocationBusinessStatusAggregate> locationBusinessState;
  ReadOnlyKeyValueStore<String, LocationPersonBusinessStateCountAggregate> structurePeopleCounts;
  ReadOnlyKeyValueStore<String, TreatedOperationalAreaAggregate> treatedOperationalCounts;
  ReadOnlyKeyValueStore<String, LocationPersonBusinessStateAggregate> structurePeople;
  ReadOnlyKeyValueStore<String, FormObservationsEvent> formObservations;
  ReadOnlyKeyValueStore<String, Long> discoveredStructuresPerPlan;
  boolean isDatastoresInitialized = false;


  public List<RowData> getIRSFullData(Plan plan, Location childLocation) {

    String geoNameDirectlyAboveStructure = null;
    if (plan.getLocationHierarchy().getNodeOrder().contains(LocationConstants.STRUCTURE)) {
      geoNameDirectlyAboveStructure = plan.getLocationHierarchy().getNodeOrder()
          .get(plan.getLocationHierarchy().getNodeOrder().indexOf(LocationConstants.STRUCTURE) - 1);
    }

    Map<String, ColumnData> columns = new LinkedHashMap<>();
    columns.put(TOTAL_SPRAY_AREAS,
        getTotalAreas(plan, childLocation, geoNameDirectlyAboveStructure));
    columns.put(TARGET_SPRAY_AREAS, getTargetedAreas(plan, childLocation));
    columns.put(VISITED_AREAS, getOperationalAreaVisitedCounts(plan, childLocation));
    columns.put(PERCENTAGE_VISITED_EFFECTIVELY, getSprayedEffectively(plan, childLocation));
    columns.put(TOTAL_STRUCTURES, getTotalStructuresCounts(plan, childLocation));
    columns.put(TOTAL_STRUCTURES_TARGETED, getTotalStructuresTargetedCount(plan, childLocation));
    columns.put(STRUCTURES_SPRAYED, getTotalStructuresSprayed(plan, childLocation));
    columns.put(SPRAY_COVERAGE_OF_TARGETED, getSprayCoverageOfTargeted(plan, childLocation));
    columns.put(TOTAL_STRUCTURES_FOUND, getTotalStructuresFoundCount(plan, childLocation));
    columns.put(FOUND_COVERAGE, getFoundCoverage(plan, childLocation));
    columns.put(SPRAY_COVERAGE_OF_FOUND_STRUCTURES,
        getSprayCoverageFoundStructures(plan, childLocation));
    RowData rowData = new RowData();
    rowData.setLocationIdentifier(childLocation.getIdentifier());
    rowData.setColumnDataMap(columns);
    rowData.setLocationName(childLocation.getName());
    return List.of(rowData);
  }

  public List<RowData> getIRSFullDataOperational(Plan plan, Location childLocation) {
    Map<String, ColumnData> columns = new LinkedHashMap<>();
    columns.put(TOTAL_STRUCTURES, getTotalStructuresCounts(plan, childLocation));
    columns.put(TOTAL_STRUCTURES_FOUND, getTotalStructuresFoundCount(plan, childLocation));
    columns.put(STRUCTURES_SPRAYED, getTotalStructuresSprayed(plan, childLocation));
    columns.put(SPRAY_COVERAGE_OF_TARGETED, getSprayCoverageOfTargeted(plan, childLocation));
    columns.put(FOUND_COVERAGE, getFoundCoverage(plan, childLocation));
    columns.put(SPRAY_COVERAGE_OF_FOUND_STRUCTURES,
        getSprayCoverageFoundStructures(plan, childLocation));
    columns.put(STRUCTURES_REMAINING_TO_SPRAY_TO_REACH_90,
        getStructuresRemainingToReach90(plan, childLocation));
    columns.put(REVIEWED_WITH_DECISION, getReviewedWithDecision(plan, childLocation));
    columns.put(MOBILIZED, getMobilized(plan, childLocation));
    RowData rowData = new RowData();
    rowData.setLocationIdentifier(childLocation.getIdentifier());
    rowData.setColumnDataMap(columns);
    rowData.setLocationName(childLocation.getName());
    return List.of(rowData);
  }

  private ColumnData getMobilized(Plan plan, Location childLocation) {
    return new ColumnData();
  }

  private ColumnData getReviewedWithDecision(Plan plan, Location childLocation) {
    return new ColumnData();
  }

  private ColumnData getStructuresRemainingToReach90(Plan plan, Location childLocation) {
    ColumnData columnData = new ColumnData();
    columnData.setIsPercentage(false);
    double sprayedStructures = (double) getTotalStructuresSprayed(plan, childLocation).getValue();
    double totalStructures = (double) getTotalStructuresCounts(plan, childLocation).getValue();
    double structuresRemaining = Math.round((totalStructures * 0.9) - sprayedStructures);
    if (structuresRemaining < 0) {
      columnData.setValue(0d);
    } else {
      columnData.setValue(structuresRemaining);
    }
    return columnData;
  }

  private ColumnData getSprayCoverageFoundStructures(Plan plan, Location childLocation) {
    ColumnData columnData = new ColumnData();
    columnData.setIsPercentage(true);
    double sprayedStructures = (double) getTotalStructuresSprayed(plan, childLocation).getValue();
    double foundStructures = (double) getTotalStructuresFoundCount(plan, childLocation).getValue();
    if (foundStructures == 0) {
      columnData.setValue(0d);
    } else {
      columnData.setValue((sprayedStructures / foundStructures) * 100);
    }
    return columnData;
  }

  private ColumnData getFoundCoverage(Plan plan, Location childLocation) {
    ColumnData columnData = new ColumnData();
    columnData.setIsPercentage(true);
    double foundStructures = (double) getTotalStructuresFoundCount(plan, childLocation).getValue();
    double targetedStructures = (double) getTotalStructuresTargetedCount(plan,
        childLocation).getValue();
    if (targetedStructures == 0) {
      columnData.setValue(0d);
    } else {
      columnData.setValue((foundStructures / targetedStructures) * 100);
    }
    return columnData;
  }

  public List<RowData> getIRSFullCoverageStructureLevelData(Plan plan, Location childLocation,
      UUID parentLocationIdentifier) {
    Map<String, ColumnData> columns = new HashMap<>();

    columns.put(STRUCTURE_STATUS,
        getLocationBusinessState(plan, childLocation, parentLocationIdentifier));
    ColumnData blankColumnData = new ColumnData();
    blankColumnData.setValue(0L);
    columns.put(NO_OF_MALES,
        getFormValue(plan, childLocation, "sprayed_males"));
    columns.put(NO_OF_FEMALES,
        getFormValue(plan, childLocation, "sprayed_females"));
    columns.put(NO_OF_ROOMS,
        getFormValue(plan, childLocation, "rooms_sprayed"));
    columns.put(NO_OF_PREGNANT_WOMEN,
        getFormValue(plan, childLocation, "sprayed_pregwomen"));
    RowData rowData = new RowData();
    rowData.setLocationIdentifier(childLocation.getIdentifier());
    rowData.setColumnDataMap(columns);
    rowData.setLocationName(childLocation.getName());
    return List.of(rowData);
  }

  private ColumnData getFormValue(Plan plan, Location childLocation, String fieldCode) {
    ColumnData columnData = new ColumnData();
    columnData.setIsPercentage(false);
    FormObservationsEvent event = formObservations.get(
        String.format("%s_%s_%s", plan.getIdentifier().toString(),
            childLocation.getIdentifier().toString(), fieldCode));
    if (event != null) {
      double value = Double.valueOf((String) event.getValues().get(0));
      columnData.setValue(value);
    } else {
      columnData.setValue(0d);
    }
    return columnData;
  }

  private ColumnData getLocationBusinessState(Plan plan, Location childLocation,
      UUID parentLocationIdentifier) {

    String businessStateDataStoreQueryKey =
        plan.getIdentifier() + "_" + parentLocationIdentifier + "_" + plan.getLocationHierarchy()
            .getIdentifier() + "_" + childLocation.getIdentifier();

    LocationBusinessStatusAggregate locationBusinessStatusAggregate = locationBusinessState.get(
        businessStateDataStoreQueryKey);

    String businessStatus = "Not Applicable";

    if (locationBusinessStatusAggregate != null) {
      businessStatus = locationBusinessStatusAggregate.getBusinessStatus();
    }

    ColumnData locationBusinessStateColumnData = new ColumnData();
    locationBusinessStateColumnData.setValue(businessStatus);
    locationBusinessStateColumnData.setMeta(null);
    locationBusinessStateColumnData.setDataType("string");
    locationBusinessStateColumnData.setIsPercentage(false);

    return locationBusinessStateColumnData;
  }

  private ColumnData getTotalStructuresSprayed(Plan plan, Location childLocation) {

    String completedStructuresQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy().getIdentifier() + "_" + "Complete";
    Long completedStructuresCountObj = countOfStructuresByBusinessStatus.get(
        completedStructuresQueryKey);
    double completedStructuresCount = 0;
    if (completedStructuresCountObj != null) {
      completedStructuresCount = completedStructuresCountObj;
    }

    ColumnData totalStructuresFoundColumnData = new ColumnData();
    totalStructuresFoundColumnData.setValue(completedStructuresCount);
    totalStructuresFoundColumnData.setIsPercentage(false);
    return totalStructuresFoundColumnData;

  }


  private Entry<String, ColumnData> getSprayedSuccess(Plan plan, Location childLocation,
      String columnName) {

    String completedStructuresQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy().getIdentifier() + "_" + "Complete";
    Long completedStructuresCountObj = countOfStructuresByBusinessStatus.get(
        completedStructuresQueryKey);
    double completedStructuresCount = 0;
    if (completedStructuresCountObj != null) {
      completedStructuresCount = completedStructuresCountObj;
    }

    String notSprayedStructuresQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy().getIdentifier() + "_" + "Not Sprayed";
    Long notSprayedStructuresCountObj = countOfStructuresByBusinessStatus.get(
        notSprayedStructuresQueryKey);
    double notSprayedStructuresCount = 0;
    if (notSprayedStructuresCountObj != null) {
      notSprayedStructuresCount = notSprayedStructuresCountObj;
    }

    double totalStructuresFound = notSprayedStructuresCount + completedStructuresCount;

    double spraySuccess = 0;
    if (totalStructuresFound > 0) {
      spraySuccess = completedStructuresCount / totalStructuresFound * 100;
    }
    ColumnData totalStructuresFoundColumnData = new ColumnData();
    totalStructuresFoundColumnData.setValue(spraySuccess);
    totalStructuresFoundColumnData.setIsPercentage(true);
    totalStructuresFoundColumnData.setMeta(
        "completedStructuresCount: " + completedStructuresCount + " / " + "totalStructuresFound: "
            + totalStructuresFound);
    return new SimpleEntry<>(columnName, totalStructuresFoundColumnData);

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
    Long notEligibleStructuresCountObj = countOfStructuresByBusinessStatus.get(
        notEligibleStructuresQueryKey);
    double notEligibleStructuresCount = 0;
    if (notEligibleStructuresCountObj != null) {
      notEligibleStructuresCount = notEligibleStructuresCountObj;
    }

    String discoveredStructuresPerParentKey = String.format("%s_%s",
        plan.getIdentifier().toString(), childLocation.getIdentifier().toString());
    double discoveredStructuresCount = discoveredStructuresPerPlan.get(
        discoveredStructuresPerParentKey) != null ? discoveredStructuresPerPlan.get(
        discoveredStructuresPerParentKey) : 0d;

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

  private ColumnData getTotalStructuresFoundCount(Plan plan, Location childLocation) {

    String completeStructuresQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy().getIdentifier() + "_" + "Complete";
    Long completeStructuresCountObj = countOfStructuresByBusinessStatus.get(
        completeStructuresQueryKey);
    double completeStructuresCount = 0;
    if (completeStructuresCountObj != null) {
      completeStructuresCount = completeStructuresCountObj;
    }

    String notSprayedStructuresQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy().getIdentifier() + "_" + "Not Sprayed";
    Long notSprayedStructuresCountObj = countOfStructuresByBusinessStatus.get(
        notSprayedStructuresQueryKey);
    double notSprayedStructuresCount = 0;
    if (notSprayedStructuresCountObj != null) {
      notSprayedStructuresCount = notSprayedStructuresCountObj;
    }

    double totalStructuresFound = notSprayedStructuresCount + completeStructuresCount;

    ColumnData totalStructuresFoundColumnData = new ColumnData();
    totalStructuresFoundColumnData.setValue(totalStructuresFound);
    totalStructuresFoundColumnData.setIsPercentage(false);
    return totalStructuresFoundColumnData;
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

  private ColumnData getSprayCoverage(Plan plan, Location childLocation) {

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

    String completedStructuresQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy().getIdentifier() + "_" + "Complete";
    Long completedStructuresCountObj = countOfStructuresByBusinessStatus.get(
        completedStructuresQueryKey);
    double completedStructuresCount = 0;
    if (completedStructuresCountObj != null) {
      completedStructuresCount = completedStructuresCountObj;
    }

    double totalStructuresExcludingNotEligible = totalStructuresCount - notEligibleStructuresCount;

    double percentageOfSprayedToTotal = 0;
    if (totalStructuresExcludingNotEligible > 0) {
      percentageOfSprayedToTotal =
          completedStructuresCount / totalStructuresExcludingNotEligible * 100;
    }

    ColumnData percentageOfSprayedToTotalColumnData = new ColumnData();
    percentageOfSprayedToTotalColumnData.setValue(percentageOfSprayedToTotal);
    percentageOfSprayedToTotalColumnData.setMeta(
        "Total Structures Sprayed: " + completedStructuresCount + " / " + "Total Structures: "
            + totalStructuresExcludingNotEligible);
    percentageOfSprayedToTotalColumnData.setIsPercentage(true);
    return percentageOfSprayedToTotalColumnData;
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

      locationBusinessState = getQueryableStoreByWaiting(getKafkaStreams.getKafkaStreams(),
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.locationBusinessStatus),
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

      discoveredStructuresPerPlan = getQueryableStoreByWaiting(getKafkaStreams.getKafkaStreams(),
          StoreQueryParameters.fromNameAndType(kafkaProperties.getStoreMap()
                  .get(KafkaConstants.discoveredStructuresCountPerPlan),
              QueryableStoreTypes.keyValueStore()));

      formObservations = getQueryableStoreByWaiting(getKafkaStreams.getKafkaStreams(),
          StoreQueryParameters.fromNameAndType(kafkaProperties.getStoreMap()
                  .get(KafkaConstants.formObservations),
              QueryableStoreTypes.keyValueStore()));
      isDatastoresInitialized = true;
    }
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
    }).collect(Collectors.toList());
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
}


