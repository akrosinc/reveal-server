package com.revealprecision.revealserver.service.dashboard;


import com.revealprecision.revealserver.api.v1.dto.factory.LocationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.models.ColumnData;
import com.revealprecision.revealserver.api.v1.dto.models.RowData;
import com.revealprecision.revealserver.api.v1.dto.response.FeatureSetResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.messaging.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.LocationBusinessStatusAggregate;
import com.revealprecision.revealserver.messaging.message.LocationPersonBusinessStateAggregate;
import com.revealprecision.revealserver.messaging.message.LocationPersonBusinessStateCountAggregate;
import com.revealprecision.revealserver.messaging.message.LocationStructureBusinessStatusAggregate;
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
public class IRSLiteDashboardService {

  private final StreamsBuilderFactoryBean getKafkaStreams;
  private final KafkaProperties kafkaProperties;
  private final PlanLocationsService planLocationsService;
  private final LocationRelationshipService locationRelationshipService;
  private final DashboardProperties dashboardProperties;

  private static final String TOTAL_SPRAY_AREAS = "Total spray areas";
  private static final String TARGET_SPRAY_AREAS = "Targeted spray areas";
  private static final String VISITED_AREAS = "Total spray areas visited";
  private static final String SPRAYED_AREAS = "Total spray areas sprayed";
  public static final String SPRAY_COVERAGE_OF_TARGETED = "Spray coverage of targeted (Progress)";
  public static final String SPRAY_COVERAGE_OF_FOUND = "Spray coverage of Found Structures";
  public static final String FOUND_COVERAGE_OF_TARGETED = "Found coverage of targeted (Progress)";
  private static final String TOTAL_STRUCTURES = "Total structures";
  private static final String TOTAL_STRUCTURES_TARGETED = "Total Structures Targeted";
  private static final String TOTAL_STRUCTURES_FOUND = "Total Structures Found";
  private static final String STRUCTURES_SPRAYED = "Total Structures Sprayed";
  private static final String AREA_VISITED = "Area Visited";

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


  boolean datastoresInitialized = false;


  public List<RowData> getIRSFullData(Plan plan, Location childLocation) {

    String geoNameDirectlyAboveStructure = null;
    if (plan.getLocationHierarchy().getNodeOrder().contains(LocationConstants.STRUCTURE)) {
      geoNameDirectlyAboveStructure = plan.getLocationHierarchy().getNodeOrder().get(
          plan.getLocationHierarchy().getNodeOrder().indexOf(LocationConstants.STRUCTURE) - 1);
    }

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

    Entry<String, ColumnData> operationalAreaSprayed = operationalAreaSprayedCounts(plan,
        childLocation, SPRAYED_AREAS);
    columns.put(operationalAreaSprayed.getKey(), operationalAreaSprayed.getValue());

    Entry<String, ColumnData> targetAreas = getTargetedAreas(plan,
        childLocation, TARGET_SPRAY_AREAS);
    columns.put(targetAreas.getKey(), targetAreas.getValue());

    Entry<String, ColumnData> totalAreas = getTotalAreas(plan,
        childLocation, TOTAL_SPRAY_AREAS, geoNameDirectlyAboveStructure);
    columns.put(totalAreas.getKey(), totalAreas.getValue());

    Entry<String, ColumnData> totalStructuresSprayed = getTotalStructuresSprayed(plan,
        childLocation, STRUCTURES_SPRAYED);
    columns.put(totalStructuresSprayed.getKey(), totalStructuresSprayed.getValue());

    Entry<String, ColumnData> totalStructuresFound = getTotalStructuresFoundCount(plan,
        childLocation, TOTAL_STRUCTURES_FOUND);
    columns.put(totalStructuresFound.getKey(), totalStructuresFound.getValue());

    Entry<String, ColumnData> foundCoverageOfTargeted = getFoundCoverage(plan,
        childLocation, FOUND_COVERAGE_OF_TARGETED);
    columns.put(foundCoverageOfTargeted.getKey(), foundCoverageOfTargeted.getValue());

    Entry<String, ColumnData> sprayCoverageOfTargeted = getSprayCoverage(plan,
        childLocation, SPRAY_COVERAGE_OF_TARGETED);
    columns.put(sprayCoverageOfTargeted.getKey(), sprayCoverageOfTargeted.getValue());

    Entry<String, ColumnData> sprayCoverageOfFound = getSprayCoverageOfFound(plan,
        childLocation, SPRAY_COVERAGE_OF_FOUND);
    columns.put(sprayCoverageOfFound.getKey(), sprayCoverageOfFound.getValue());

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

    Entry<String, ColumnData> totalStructuresFoundCount = getTotalStructuresFoundCountInSprayArea(
        plan, childLocation, TOTAL_STRUCTURES_FOUND);
    columns.put(totalStructuresFoundCount.getKey(), totalStructuresFoundCount.getValue());

    Entry<String, ColumnData> totalStructuresSprayed = getTotalStructuresSprayedCountInSprayArea(plan,
        childLocation, STRUCTURES_SPRAYED);
    columns.put(totalStructuresSprayed.getKey(), totalStructuresSprayed.getValue());

    Entry<String, ColumnData> areaVisited = getAreaVisitedInSprayArea(plan,
        childLocation, AREA_VISITED);
    columns.put(areaVisited.getKey(), areaVisited.getValue());

    RowData rowData = new RowData();
    rowData.setLocationIdentifier(childLocation.getIdentifier());
    rowData.setColumnDataMap(columns);
    rowData.setLocationName(childLocation.getName());
    return List.of(rowData);
  }

  private Entry<String, ColumnData> getTotalStructuresSprayed(Plan plan,
      Location childLocation, String columnName) {

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
    return new SimpleEntry<>(columnName, totalStructuresFoundColumnData);

  }

  private Entry<String, ColumnData> operationalAreaVisitedCounts(Plan plan,
      Location childLocation, String columnName) {

    String sprayedLocationsQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Sprayed";
    Long sprayedLocationsObj = countOfLocationsByBusinessStatus.get(
        sprayedLocationsQueryKey);
    double sprayedLocationsCount = 0;
    if (sprayedLocationsObj != null){
      sprayedLocationsCount = sprayedLocationsObj;
    }

    String notSprayedLocationsQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Not Sprayed";
    Long notSprayedLocationsObj = countOfLocationsByBusinessStatus.get(
        notSprayedLocationsQueryKey);
    double notSprayedLocationsCount = 0;
    if (notSprayedLocationsObj != null){
      notSprayedLocationsCount = notSprayedLocationsObj;
    }

    double visitedAreas = sprayedLocationsCount + notSprayedLocationsCount;


    ColumnData operationalAreaVisitedColumnData = new ColumnData();
    operationalAreaVisitedColumnData.setValue(visitedAreas);
    operationalAreaVisitedColumnData.setIsPercentage(false);
    return new SimpleEntry<>(columnName, operationalAreaVisitedColumnData);
  }

  private Entry<String, ColumnData> operationalAreaSprayedCounts(Plan plan,
      Location childLocation, String columnName) {

    String sprayedLocationsQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Sprayed";
    Long sprayedLocationsObj = countOfLocationsByBusinessStatus.get(
        sprayedLocationsQueryKey);
    double sprayedLocationsCount = 0;
    if (sprayedLocationsObj != null){
      sprayedLocationsCount = sprayedLocationsObj;
    }

    ColumnData operationalAreaSprayedColumnData = new ColumnData();
    operationalAreaSprayedColumnData.setValue(sprayedLocationsCount);
    operationalAreaSprayedColumnData.setIsPercentage(false);
    return new SimpleEntry<>(columnName, operationalAreaSprayedColumnData);
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
      Location childLocation, String columnName, String geoNameDirectlyAboveStructure) {

    Long totalOperationAreaCounts = locationRelationshipService.getNumberOfChildrenByGeoLevelNameWithinLocationAndHierarchy(
        geoNameDirectlyAboveStructure, childLocation.getIdentifier(), plan.getLocationHierarchy().getIdentifier());

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
    return new SimpleEntry<>(columnName, totalStructuresTargetedColumnData);
  }

  private Entry<String, ColumnData> getTotalStructuresFoundCount(Plan plan,
      Location childLocation, String columnName) {

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
    return new SimpleEntry<>(columnName, totalStructuresFoundColumnData);
  }
  private Entry<String, ColumnData> getTotalStructuresFoundCountInSprayArea(Plan plan,
      Location childLocation, String columnName) {

    String locationStructureBusinessStatusAggregateQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier();
    LocationStructureBusinessStatusAggregate locationStructureBusinessStatusAggregate = countOfLocationStructuresByBusinessStatusInSprayArea.get(
        locationStructureBusinessStatusAggregateQueryKey);
    double totalStructuresFound = 0;
    if (locationStructureBusinessStatusAggregate != null) {
      if (locationStructureBusinessStatusAggregate.getBusinessStatus().equals("Sprayed") ||
      locationStructureBusinessStatusAggregate.getBusinessStatus().equals("Not Sprayed")){
        if (locationStructureBusinessStatusAggregate.getStructureCounts()!=null ){
                 totalStructuresFound = locationStructureBusinessStatusAggregate.getStructureCounts();
        }

      }
    }

    ColumnData totalStructuresFoundColumnData = new ColumnData();
    totalStructuresFoundColumnData.setValue(totalStructuresFound);
    totalStructuresFoundColumnData.setIsPercentage(false);
    return new SimpleEntry<>(columnName, totalStructuresFoundColumnData);
  }

  private Entry<String, ColumnData> getTotalStructuresSprayedCountInSprayArea(Plan plan,
      Location childLocation, String columnName) {

    String locationStructureBusinessStatusAggregateQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier();
    LocationStructureBusinessStatusAggregate locationStructureBusinessStatusAggregate = countOfLocationStructuresByBusinessStatusInSprayArea.get(
        locationStructureBusinessStatusAggregateQueryKey);
    double totalStructuresSprayed = 0;
    if (locationStructureBusinessStatusAggregate != null) {
      if (locationStructureBusinessStatusAggregate.getBusinessStatus().equals("Sprayed")){
        if (locationStructureBusinessStatusAggregate.getStructureCounts()!=null ){
          totalStructuresSprayed = locationStructureBusinessStatusAggregate.getStructureCounts();
        }

      }
    }

    ColumnData totalStructuresSprayedColumnData = new ColumnData();
    totalStructuresSprayedColumnData.setValue(totalStructuresSprayed);
    totalStructuresSprayedColumnData.setIsPercentage(false);
    return new SimpleEntry<>(columnName, totalStructuresSprayedColumnData);
  }
  private Entry<String, ColumnData> getAreaVisitedInSprayArea(Plan plan,
      Location childLocation, String columnName) {

    String locationStructureBusinessStatusAggregateQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier();
    LocationStructureBusinessStatusAggregate locationStructureBusinessStatusAggregate = countOfLocationStructuresByBusinessStatusInSprayArea.get(
        locationStructureBusinessStatusAggregateQueryKey);
    String areaVisited = "no";
    if (locationStructureBusinessStatusAggregate != null) {
      if (!locationStructureBusinessStatusAggregate.getBusinessStatus().equals("Not Visited")){
        areaVisited = "yes";
      }
    }

    ColumnData areaVisitedColumnData = new ColumnData();
    areaVisitedColumnData.setValue(areaVisited);
    areaVisitedColumnData.setIsPercentage(false);
    areaVisitedColumnData.setDataType("string");
    return new SimpleEntry<>(columnName, areaVisitedColumnData);
  }

  private Entry<String, ColumnData> getFoundCoverage(Plan plan,
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
    Long notEligibleStructuresCountObj = countOfLocationsByBusinessStatus.get(
        notEligibleStructuresQueryKey);
    double notEligibleStructuresCount = 0;
    if (notEligibleStructuresCountObj != null) {
      notEligibleStructuresCount = notEligibleStructuresCountObj;
    }

    double totalStructuresInTargetedCount =
        totalStructuresInPlanLocationCount - notEligibleStructuresCount;

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

    double foundCoverage = 0;

    if (totalStructuresInTargetedCount > 0){
      foundCoverage = totalStructuresFound / totalStructuresInTargetedCount * 100;
    }

    ColumnData foundCoverageColumnData = new ColumnData();
    foundCoverageColumnData.setValue(foundCoverage);
    foundCoverageColumnData.setMeta(
        "Total Structures Found: " + totalStructuresFound + " / "
            + "Total Structures Targeted: "
            + totalStructuresInTargetedCount);
    foundCoverageColumnData.setIsPercentage(true);
    return new SimpleEntry<>(columnName, foundCoverageColumnData);
  }


  private Entry<String, ColumnData> getSprayCoverageOfFound(Plan plan,
      Location childLocation, String columnName) {


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

    if (totalStructuresFound > 0){
      sprayCoverageOfFound = sprayedLocationsCount / totalStructuresFound * 100;
    }

    ColumnData sprayCoverageOfFoundColumnData = new ColumnData();
    sprayCoverageOfFoundColumnData.setValue(sprayCoverageOfFound);
    sprayCoverageOfFoundColumnData.setMeta(
        "Total Structures Sprayed: " + sprayedLocationsCount + " / "
            + "Total Structures Found: "
            + totalStructuresFound);
    sprayCoverageOfFoundColumnData.setIsPercentage(true);
    return new SimpleEntry<>(columnName, sprayCoverageOfFoundColumnData);
  }

  private Entry<String, ColumnData> getSprayCoverage(Plan plan,
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
    Long notEligibleStructuresCountObj = countOfLocationsByBusinessStatus.get(
        notEligibleStructuresQueryKey);
    double notEligibleStructuresCount = 0;
    if (notEligibleStructuresCountObj != null) {
      notEligibleStructuresCount = notEligibleStructuresCountObj;
    }

    double totalStructuresInTargetedCount =
        totalStructuresInPlanLocationCount - notEligibleStructuresCount;

    String sprayedLocationsQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_" + "Sprayed";
    LocationStructureBusinessStatusAggregate sprayedLocationsCountObj = countOfLocationStructuresByBusinessStatus.get(
        sprayedLocationsQueryKey);
    double sprayedLocationsCount = 0;
    if (sprayedLocationsCountObj != null) {
      sprayedLocationsCount = sprayedLocationsCountObj.getStructureSum();
    }

    double sprayCoverage = 0;

    if (totalStructuresInTargetedCount > 0){
      sprayCoverage = sprayedLocationsCount / totalStructuresInTargetedCount * 100;
    }

    ColumnData sprayCoverageColumnData = new ColumnData();
    sprayCoverageColumnData.setValue(sprayCoverage);
    sprayCoverageColumnData.setMeta(
        "Total Structures Sprayed: " + sprayedLocationsCount + " / "
            + "Total Structures Targeted: "
            + totalStructuresInTargetedCount);
    sprayCoverageColumnData.setIsPercentage(true);
    return new SimpleEntry<>(columnName, sprayCoverageColumnData);
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

      countOfLocationsByBusinessStatus = getKafkaStreams.getKafkaStreams().store(
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

      countOfLocationStructuresByBusinessStatus = getKafkaStreams.getKafkaStreams().store(
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.locationStructureHierarchyBusinessStatus),
              QueryableStoreTypes.keyValueStore())
      );

      countOfLocationStructuresByBusinessStatusInSprayArea = getKafkaStreams.getKafkaStreams().store(
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.locationStructureBusinessStatus),
              QueryableStoreTypes.keyValueStore())
      );

      datastoresInitialized = true;
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
  public FeatureSetResponse getFeatureSetResponse(UUID parentIdentifier, List<PlanLocationDetails> locationDetails,
      Map<UUID, RowData> rowDataMap, String reportLevel) {
    FeatureSetResponse response = new FeatureSetResponse();
    response.setType("FeatureCollection");
    List<LocationResponse> locationResponses = locationDetails.stream()
        .map(loc -> LocationResponseFactory.fromPlanLocationDetails(loc, parentIdentifier))
        .collect(Collectors.toList());

    locationResponses = setGeoJsonProperties(rowDataMap, locationResponses);
    response.setDefaultDisplayColumn(dashboardProperties.getIrsDefaultDisplayColumns().getOrDefault(reportLevel, null));
    response.setFeatures(locationResponses);
    response.setIdentifier(parentIdentifier);
    return response;
  }
}
