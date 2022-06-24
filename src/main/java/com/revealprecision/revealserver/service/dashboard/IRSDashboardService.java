package com.revealprecision.revealserver.service.dashboard;


import com.revealprecision.revealserver.api.v1.dto.models.ColumnData;
import com.revealprecision.revealserver.api.v1.dto.models.RowData;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.messaging.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.LocationBusinessStatusAggregate;
import com.revealprecision.revealserver.messaging.message.LocationPersonBusinessStateAggregate;
import com.revealprecision.revealserver.messaging.message.LocationPersonBusinessStateCountAggregate;
import com.revealprecision.revealserver.messaging.message.OperationalAreaVisitedCount;
import com.revealprecision.revealserver.messaging.message.PersonBusinessStatusAggregate;
import com.revealprecision.revealserver.messaging.message.TreatedOperationalAreaAggregate;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import com.revealprecision.revealserver.service.PlanLocationsService;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
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

  public static final String TOTAL_SPRAY_AREAS = "Total spray areas";
  public static final String TARGET_SPRAY_AREAS = "Targeted spray areas";
  public static final String VISITED_AREAS = "Total spray areas visited";
  public static final String SPRAY_COVERAGE_OF_TARGETED = "Spray coverage of targeted (Progress)";
  public static final String TOTAL_STRUCTURES = "Total structures";
  public static final String TOTAL_STRUCTURES_TARGETED = "Total Structures Targeted";
  public static final String TOTAL_STRUCTURES_FOUND = "Total Structures Found";
  public static final String STRUCTURES_SPRAYED = "Total Structures Sprayed";
  public static final String SPRAY_COVERAGE = "Spray Coverage (Effectiveness)";
  public static final String SPRAY_SUCCESS = "Spray Success Rate (PMI SC)";
  public static final String PERCENTAGE_VISITED_EFFECTIVELY = "Spray Areas Effectively sprayed";
  public static final String STRUCTURE_STATUS = "Structure Status";
  public static final String NO_OF_ROOMS = "No of Rooms";
  public static final String NO_OF_MALES = "No of Males";
  public static final String NO_OF_FEMALES = "No of Females";
  public static final String NO_OF_PREGNANT_WOMEN = "No of Pregnant Women";


  ReadOnlyKeyValueStore<String, Long> countOfAssignedStructures;
  ReadOnlyKeyValueStore<String, Long> structureCounts;
  ReadOnlyKeyValueStore<String, Long> countOfStructuresByBusinessStatus;
  ReadOnlyKeyValueStore<String, OperationalAreaVisitedCount> countOfOperationalArea;
  ReadOnlyKeyValueStore<String, PersonBusinessStatusAggregate> personBusinessStatus;
  ReadOnlyKeyValueStore<String, LocationBusinessStatusAggregate> locationBusinessState;
  ReadOnlyKeyValueStore<String, LocationPersonBusinessStateCountAggregate> structurePeopleCounts;
  ReadOnlyKeyValueStore<String, TreatedOperationalAreaAggregate> treatedOperationalCounts;
  ReadOnlyKeyValueStore<String, LocationPersonBusinessStateAggregate> structurePeople;
  boolean datastoresInitialized = false;


  public List<RowData> getIRSFullData(Plan plan, Location childLocation) {
    Map<String, ColumnData> columns = new HashMap<>();

    Entry<String, ColumnData> totalStructuresCounts = getTotalStructuresCounts(plan, childLocation,
        TOTAL_STRUCTURES);
    columns.put(totalStructuresCounts.getKey(), totalStructuresCounts.getValue());

    Entry<String, ColumnData> totalStructuresTargetedCount = getTotalStructuresTargetedCount(
        plan, childLocation, TOTAL_STRUCTURES_TARGETED);
    columns.put(totalStructuresTargetedCount.getKey(), totalStructuresTargetedCount.getValue());

    Entry<String, ColumnData> totalStructuresFoundCount = getTotalStructuresFoundCount(
        plan, childLocation, TOTAL_STRUCTURES_FOUND);
    columns.put(totalStructuresFoundCount.getKey(), totalStructuresFoundCount.getValue());

    Entry<String, ColumnData> operationalAreaVisited = operationalAreaVisitedCounts(plan,
        childLocation, VISITED_AREAS);
    columns.put(operationalAreaVisited.getKey(), operationalAreaVisited.getValue());

    Entry<String, ColumnData> targetAreas = getTargetedAreas(plan,
        childLocation, TARGET_SPRAY_AREAS);
    columns.put(targetAreas.getKey(), targetAreas.getValue());

    Entry<String, ColumnData> totalAreas = getTotalAreas(plan,
        childLocation, TOTAL_SPRAY_AREAS);
    columns.put(totalAreas.getKey(), totalAreas.getValue());

    Entry<String, ColumnData> totalStructuresSprayed = getTotalStructuresSprayed(plan,
        childLocation, STRUCTURES_SPRAYED);
    columns.put(totalStructuresSprayed.getKey(), totalStructuresSprayed.getValue());

    Entry<String, ColumnData> sprayCoverageOfTargeted = getSprayCoverageOfTargeted(plan,
        childLocation, SPRAY_COVERAGE_OF_TARGETED);
    columns.put(sprayCoverageOfTargeted.getKey(), sprayCoverageOfTargeted.getValue());

    Entry<String, ColumnData> spraySuccess = getSprayedSuccess(plan,
        childLocation, SPRAY_SUCCESS);
    columns.put(spraySuccess.getKey(), spraySuccess.getValue());

    Entry<String, ColumnData> sprayCoverageEffectively = getSprayedEffectively(plan,
        childLocation, PERCENTAGE_VISITED_EFFECTIVELY);
    columns.put(sprayCoverageEffectively.getKey(), sprayCoverageEffectively.getValue());

    RowData rowData = new RowData();
    rowData.setLocationIdentifier(childLocation.getIdentifier());
    rowData.setColumnDataMap(columns);
    rowData.setLocationName(childLocation.getName());
    return List.of(rowData);
  }

  public List<RowData> getIRSFullDataOperational(Plan plan, Location childLocation) {
    Map<String, ColumnData> columns = new HashMap<>();

    Entry<String, ColumnData> totalStructuresCounts = getTotalStructuresCounts(plan, childLocation,
        TOTAL_STRUCTURES);
    columns.put(totalStructuresCounts.getKey(), totalStructuresCounts.getValue());

    Entry<String, ColumnData> totalStructuresTargetedCount = getTotalStructuresTargetedCount(
        plan, childLocation, TOTAL_STRUCTURES_TARGETED);
    columns.put(totalStructuresTargetedCount.getKey(), totalStructuresTargetedCount.getValue());

    Entry<String, ColumnData> totalStructuresFoundCount = getTotalStructuresFoundCount(
        plan, childLocation, TOTAL_STRUCTURES_FOUND);
    columns.put(totalStructuresFoundCount.getKey(), totalStructuresFoundCount.getValue());

    Entry<String, ColumnData> totalStructuresSprayed = getTotalStructuresSprayed(plan,
        childLocation, STRUCTURES_SPRAYED);
    columns.put(totalStructuresSprayed.getKey(), totalStructuresSprayed.getValue());

    Entry<String, ColumnData> sprayCoverageOfTargeted = getSprayCoverageOfTargeted(plan,
        childLocation, SPRAY_COVERAGE_OF_TARGETED);
    columns.put(sprayCoverageOfTargeted.getKey(), sprayCoverageOfTargeted.getValue());

    Entry<String, ColumnData> spraySuccess = getSprayedSuccess(plan,
        childLocation, SPRAY_SUCCESS);
    columns.put(spraySuccess.getKey(), spraySuccess.getValue());

    Entry<String, ColumnData> sprayCoverage = getSprayCoverage(plan,
        childLocation, SPRAY_COVERAGE);
    columns.put(sprayCoverage.getKey(), sprayCoverage.getValue());

    RowData rowData = new RowData();
    rowData.setLocationIdentifier(childLocation.getIdentifier());
    rowData.setColumnDataMap(columns);
    rowData.setLocationName(childLocation.getName());
    return List.of(rowData);
  }

  public List<RowData> getIRSFullCoverageStructureLevelData(Plan plan,
      Location childLocation, UUID parentLocationIdentifier) {
    Map<String, ColumnData> columns = new HashMap<>();

    Entry<String, ColumnData> businessStateColumnData = getLocationBusinessState(plan,
        childLocation, STRUCTURE_STATUS, parentLocationIdentifier);
    columns.put(businessStateColumnData.getKey(), businessStateColumnData.getValue());

    ColumnData blankColumnData = new ColumnData();
    blankColumnData.setValue(0L);

    Entry<String, ColumnData> noOfMalesColumnData = new SimpleEntry<>(NO_OF_MALES, blankColumnData);
    columns.put(noOfMalesColumnData.getKey(), noOfMalesColumnData.getValue());

    Entry<String, ColumnData> noOfFemalesColumnData = new SimpleEntry<>(NO_OF_FEMALES,
        blankColumnData);
    columns.put(noOfFemalesColumnData.getKey(), noOfFemalesColumnData.getValue());

    Entry<String, ColumnData> noOfRoomsColumnData = new SimpleEntry<>(NO_OF_ROOMS, blankColumnData);
    columns.put(noOfRoomsColumnData.getKey(), noOfRoomsColumnData.getValue());

    Entry<String, ColumnData> noOfPregnantWomenColumnData = new SimpleEntry<>(NO_OF_PREGNANT_WOMEN,
        blankColumnData);
    columns.put(noOfPregnantWomenColumnData.getKey(), noOfPregnantWomenColumnData.getValue());

    RowData rowData = new RowData();
    rowData.setLocationIdentifier(childLocation.getIdentifier());
    rowData.setColumnDataMap(columns);
    rowData.setLocationName(childLocation.getName());
    return List.of(rowData);
  }

  private Entry<String, ColumnData> getLocationBusinessState(Plan plan,
      Location childLocation, String columnName, UUID parentLocationIdentifier) {

    String businessStateDataStoreQueryKey =
        plan.getIdentifier() + "_" +
            parentLocationIdentifier + "_" +
            plan.getLocationHierarchy().getIdentifier() + "_" +
            childLocation.getIdentifier();

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

    return new SimpleEntry<>(columnName, locationBusinessStateColumnData);
  }

  private Entry<String, ColumnData> getTotalStructuresSprayed(Plan plan,
      Location childLocation, String columnName) {

    String completedStructuresQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Complete";
    Long completedStructuresCountObj = countOfStructuresByBusinessStatus.get(
        completedStructuresQueryKey);
    double completedStructuresCount = 0;
    if (completedStructuresCountObj != null) {
      completedStructuresCount = completedStructuresCountObj;
    }

    ColumnData totalStructuresFoundColumnData = new ColumnData();
    totalStructuresFoundColumnData.setValue(completedStructuresCount);
    totalStructuresFoundColumnData.setIsPercentage(false);
    return new SimpleEntry<>(columnName, totalStructuresFoundColumnData);

  }


  private Entry<String, ColumnData> getSprayedSuccess(Plan plan,
      Location childLocation, String columnName) {

    String completedStructuresQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Complete";
    Long completedStructuresCountObj = countOfStructuresByBusinessStatus.get(
        completedStructuresQueryKey);
    double completedStructuresCount = 0;
    if (completedStructuresCountObj != null) {
      completedStructuresCount = completedStructuresCountObj;
    }

    String notSprayedStructuresQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Not Sprayed";
    Long notSprayedStructuresCountObj = countOfStructuresByBusinessStatus.get(
        notSprayedStructuresQueryKey);
    double notSprayedStructuresCount = 0;
    if (notSprayedStructuresCountObj != null) {
      notSprayedStructuresCount = notSprayedStructuresCountObj;
    }

    double totalStructuresFound =
        notSprayedStructuresCount + completedStructuresCount;

    double spraySuccess = 0;
    if (totalStructuresFound > 0) {
      spraySuccess = completedStructuresCount / totalStructuresFound * 100;
    }
    ColumnData totalStructuresFoundColumnData = new ColumnData();
    totalStructuresFoundColumnData.setValue(spraySuccess);
    totalStructuresFoundColumnData.setIsPercentage(true);
    totalStructuresFoundColumnData.setMeta(
        "completedStructuresCount: " + completedStructuresCount + " / " + "totalStructuresFound: "
            + totalStructuresFound
    );
    return new SimpleEntry<>(columnName, totalStructuresFoundColumnData);

  }

  private Entry<String, ColumnData> getSprayedEffectively(Plan plan,
      Location childLocation, String columnName) {

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
            + "operationalAreaVisitedCount: "
            + operationalAreaVisitedCount
    );

    return new SimpleEntry<>(columnName, percentageSprayedEffectivelyColumnData);

  }

  private Entry<String, ColumnData> operationalAreaVisitedCounts(Plan plan,
      Location childLocation, String columnName) {
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
    return new SimpleEntry<>(columnName, operationalAreaVisitedColumnData);
  }

  private Entry<String, ColumnData> getTargetedAreas(Plan plan,
      Location childLocation, String columnName) {

    Long countOfOperationalAreas = planLocationsService.getNumberOfAssignedChildrenByGeoLevelNameWithinLocationAndHierarchyAndPlan(
        plan.getIdentifier(),
        LocationConstants.OPERATIONAL,
        childLocation.getIdentifier(),
        plan.getLocationHierarchy().getIdentifier()
    );

    Long countOfOperationalAreasValue = 0L;

    if (countOfOperationalAreas != null) {
      countOfOperationalAreasValue = countOfOperationalAreas;
    }

    ColumnData operationalAreaVisitedColumnData = new ColumnData();
    operationalAreaVisitedColumnData.setValue(countOfOperationalAreasValue);
    operationalAreaVisitedColumnData.setIsPercentage(false);
    return new SimpleEntry<>(columnName, operationalAreaVisitedColumnData);
  }

  private Entry<String, ColumnData> getTotalAreas(Plan plan,
      Location childLocation, String columnName) {

    Long totalOperationAreaCounts = locationRelationshipService.getNumberOfChildrenByGeoLevelNameWithinLocationAndHierarchy(
        "operational", childLocation.getIdentifier(), plan.getLocationHierarchy().getIdentifier());

    Long totalOperationAreaCountsValue = 0L;

    if (totalOperationAreaCounts != null) {
      totalOperationAreaCountsValue = totalOperationAreaCounts;
    }

    ColumnData operationalAreaVisitedColumnData = new ColumnData();
    operationalAreaVisitedColumnData.setValue(totalOperationAreaCountsValue);
    operationalAreaVisitedColumnData.setIsPercentage(false);
    return new SimpleEntry<>(columnName, operationalAreaVisitedColumnData);
  }

  private Entry<String, ColumnData> getTotalStructuresCounts(Plan plan, Location childLocation,
      String columnName) {
    String totalStructuresQueryKey =
        plan.getLocationHierarchy().getIdentifier() + "_" + childLocation.getIdentifier();
    Long totalStructuresCountObj = structureCounts.get(totalStructuresQueryKey);
    double totalStructuresCount = 0;
    if (totalStructuresCountObj != null) {
      totalStructuresCount = totalStructuresCountObj;
    }

    String notEligibleStructuresQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Not Eligible";
    Long notEligibleStructuresCountObj = countOfStructuresByBusinessStatus.get(
        notEligibleStructuresQueryKey);
    double notEligibleStructuresCount = 0;
    if (notEligibleStructuresCountObj != null) {
      notEligibleStructuresCount = notEligibleStructuresCountObj;
    }

    double totalStructuresExcludingNotEligible = totalStructuresCount - notEligibleStructuresCount;

    ColumnData totalStructuresColumnData = new ColumnData();
    totalStructuresColumnData.setValue(totalStructuresExcludingNotEligible);
    totalStructuresColumnData.setIsPercentage(false);
    return new SimpleEntry<>(columnName, totalStructuresColumnData);
  }

  private Entry<String, ColumnData> getTotalStructuresTargetedCount(Plan plan,
      Location childLocation, String columnName) {

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
            + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Not Eligible";
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
    return new SimpleEntry<>(columnName, totalStructuresTargetedColumnData);
  }

  private Entry<String, ColumnData> getTotalStructuresFoundCount(Plan plan,
      Location childLocation, String columnName) {

    String completeStructuresQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Complete";
    Long completeStructuresCountObj = countOfStructuresByBusinessStatus.get(
        completeStructuresQueryKey);
    double completeStructuresCount = 0;
    if (completeStructuresCountObj != null) {
      completeStructuresCount = completeStructuresCountObj;
    }

    String notSprayedStructuresQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Not Sprayed";
    Long notSprayedStructuresCountObj = countOfStructuresByBusinessStatus.get(
        notSprayedStructuresQueryKey);
    double notSprayedStructuresCount = 0;
    if (notSprayedStructuresCountObj != null) {
      notSprayedStructuresCount = notSprayedStructuresCountObj;
    }

    double totalStructuresFound =
        notSprayedStructuresCount + completeStructuresCount;

    ColumnData totalStructuresFoundColumnData = new ColumnData();
    totalStructuresFoundColumnData.setValue(totalStructuresFound);
    totalStructuresFoundColumnData.setIsPercentage(false);
    return new SimpleEntry<>(columnName, totalStructuresFoundColumnData);
  }

  private Entry<String, ColumnData> getSprayCoverageOfTargeted(Plan plan,
      Location childLocation, String columnName) {

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
            + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Not Eligible";
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
            + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Complete";
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
            + "Total Structures Targeted: "
            + totalStructuresInTargetedCount);
    totalStructuresTargetedColumnData.setIsPercentage(true);
    return new SimpleEntry<>(columnName, totalStructuresTargetedColumnData);
  }

  private Entry<String, ColumnData> getSprayCoverage(Plan plan,
      Location childLocation, String columnName) {

    String totalStructuresQueryKey =
        plan.getLocationHierarchy().getIdentifier() + "_" + childLocation.getIdentifier();
    Long totalStructuresCountObj = structureCounts.get(totalStructuresQueryKey);
    double totalStructuresCount = 0;
    if (totalStructuresCountObj != null) {
      totalStructuresCount = totalStructuresCountObj;
    }

    String notEligibleStructuresQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Not Eligible";
    Long notEligibleStructuresCountObj = countOfStructuresByBusinessStatus.get(
        notEligibleStructuresQueryKey);
    double notEligibleStructuresCount = 0;
    if (notEligibleStructuresCountObj != null) {
      notEligibleStructuresCount = notEligibleStructuresCountObj;
    }

    String completedStructuresQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Complete";
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
        "Total Structures Sprayed: " + completedStructuresCount + " / "
            + "Total Structures: "
            + totalStructuresExcludingNotEligible);
    percentageOfSprayedToTotalColumnData.setIsPercentage(true);
    return new SimpleEntry<>(columnName, percentageOfSprayedToTotalColumnData);
  }


  public void initDataStoresIfNecessary() {
    if (!datastoresInitialized) {
      countOfAssignedStructures = getKafkaStreams.getKafkaStreams().store(
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.assignedStructureCountPerParent),
              QueryableStoreTypes.keyValueStore()));

      structureCounts = getKafkaStreams.getKafkaStreams()
          .store(StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.structureCountPerParent),
              QueryableStoreTypes.keyValueStore()));

      countOfStructuresByBusinessStatus = getKafkaStreams.getKafkaStreams().store(
          StoreQueryParameters.fromNameAndType(kafkaProperties.getStoreMap()
                  .get(KafkaConstants.locationBusinessStatusByPlanParentHierarchy),
              QueryableStoreTypes.keyValueStore()));

      countOfOperationalArea = getKafkaStreams.getKafkaStreams().store(
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap()
                  .get(KafkaConstants.operationalAreaByPlanParentHierarchy),
              QueryableStoreTypes.keyValueStore()));

      personBusinessStatus = getKafkaStreams.getKafkaStreams().store(
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.personBusinessStatus),
              QueryableStoreTypes.keyValueStore()));

      locationBusinessState = getKafkaStreams.getKafkaStreams().store(
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.locationBusinessStatus),
              QueryableStoreTypes.keyValueStore()));

      structurePeopleCounts = getKafkaStreams.getKafkaStreams().store(
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.structurePeopleCounts),
              QueryableStoreTypes.keyValueStore()));

      treatedOperationalCounts = getKafkaStreams.getKafkaStreams().store(
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.operationalTreatedCounts),
              QueryableStoreTypes.keyValueStore()));

      structurePeople = getKafkaStreams.getKafkaStreams().store(
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.structurePeople),
              QueryableStoreTypes.keyValueStore()));

      datastoresInitialized = true;
    }
  }

  public List<LocationResponse> setGeoJsonProperties(Map<UUID, RowData> rowDataMap,
      List<LocationResponse> locationResponses) {
    return locationResponses.stream().peek(loc -> {
      loc.getProperties().setColumnDataMap(rowDataMap.get(loc.getIdentifier()).getColumnDataMap());
      loc.getProperties().setId(loc.getIdentifier());
      if (rowDataMap.get(loc.getIdentifier()).getColumnDataMap().get(SPRAY_COVERAGE_OF_TARGETED)
          != null) {
        loc.getProperties().setSprayCoverage(
            rowDataMap.get(loc.getIdentifier()).getColumnDataMap().get(SPRAY_COVERAGE_OF_TARGETED)
                .getValue());
      }
    }).collect(Collectors.toList());
  }
}
