package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.LocationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.models.ColumnData;
import com.revealprecision.revealserver.api.v1.dto.models.RowData;
import com.revealprecision.revealserver.api.v1.dto.models.TableRow;
import com.revealprecision.revealserver.api.v1.dto.response.FeatureSetResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.enums.LookupUtil;
import com.revealprecision.revealserver.enums.ReportTypeEnum;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.messaging.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.OperationalAreaVisitedCount;
import com.revealprecision.revealserver.messaging.message.PersonBusinessStatus;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.PlanLocations;
import com.revealprecision.revealserver.persistence.projection.PlanLocationDetails;
import com.revealprecision.revealserver.props.KafkaProperties;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
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
import org.springframework.context.annotation.DependsOn;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@DependsOn("OperationalAreaTable")
public class DashboardService {

  public static final String TREATMENT_COVERAGE = "Treatment coverage";
  public static final String HEALTH_FACILITY_REFERRAL = "Health Facility Referral";
  public static final String OPERATIONAL_AREA_VISITED = "Operational Area Visited";
  public static final String TOTAL_STRUCTURES_RECEIVED_SPAQ = "Total Structures Received SPAQ";
  public static final String DISTRIBUTION_COVERAGE = "Distribution Coverage";
  public static final String FOUND_COVERAGE = "Found Coverage";
  public static final String TOTAL_STRUCTURES_FOUND = "Total Structures Found";
  public static final String TOTAL_STRUCTURES_TARGETED = "Total Structures Targeted";
  public static final String TOTAL_STRUCTURES = "Total Structures";

  private final StreamsBuilderFactoryBean getKafkaStreams;
  private final KafkaProperties kafkaProperties;
  private final LocationService locationService;
  private final LocationRelationshipService locationRelationshipService;
  private final PlanService planService;
  private final PlanLocationsService planLocationsService;

  ReadOnlyKeyValueStore<String, Long> countOfAssignedStructures;
  ReadOnlyKeyValueStore<String, Long> structureCounts;
  ReadOnlyKeyValueStore<String, Long> countOfStructuresByBusinessStatus;
  ReadOnlyKeyValueStore<String, OperationalAreaVisitedCount> countOfOperationalArea;
  ReadOnlyKeyValueStore<String, PersonBusinessStatus> personBusinessStatus;
  boolean datastoresInitialized = false;


  public TableRow getRowData(UUID planIdentifier, UUID parentLocationIdentifier,
      Boolean getChildren, ReportTypeEnum reportTypeEnum) {

    initDataStoresIfNecessary();

    Plan plan = planService.getPlanByIdentifier(planIdentifier);

    parentLocationIdentifier = getParentLocation(planIdentifier, parentLocationIdentifier, plan);

    List<Location> childrenLocations = getChildrenLocations(parentLocationIdentifier, getChildren,
        plan);

    List<RowData> rowDatas = childrenLocations.stream().map(childLocation -> {

      switch (reportTypeEnum) {

        case MDA_FULL_COVERAGE:
          return getMDAFullCoverageData(planIdentifier, plan, childLocation);

        case MDA_FULL_COVERAGE_OPERATIONAL_AREA_LEVEL:
          return getMDAFullCoverageOperationalAreaLevelData(planIdentifier, plan, childLocation);

        case IRS_FULL_COVERAGE:
          return getIRSFullData(childLocation);

      }
      return null;
    }).collect(Collectors.toList());

    TableRow tableRow = new TableRow();
    tableRow.setRowData(rowDatas);
    tableRow.setPlanIdentifier(planIdentifier);
    tableRow.setReportTypeEnum(reportTypeEnum);
    tableRow.setParentLocationIdentifier(parentLocationIdentifier);

    Map<UUID, Long> childrenCount = locationRelationshipService.getLocationChildrenCount(plan.getLocationHierarchy().getIdentifier())
        .stream().filter(loc -> loc.getParentIdentifier() != null)
        .collect(Collectors.toMap(loc -> UUID.fromString(loc.getParentIdentifier()), loc -> loc.getChildrenCount()));

    tableRow.getRowData().forEach(row -> row.setChildrenNumber(childrenCount.containsKey(row.getLocationIdentifier()) ? childrenCount.get(row.getLocationIdentifier()) : 0));

    return tableRow;
  }

  private RowData getIRSFullData(Location childLocation) {
    Map<String, ColumnData> columns = new HashMap<>();
    RowData rowData = new RowData();
    rowData.setLocationIdentifier(childLocation.getIdentifier());
    rowData.setColumnDataMap(columns);
    rowData.setLocationName(childLocation.getName());
    return rowData;
  }

  private RowData getMDAFullCoverageOperationalAreaLevelData(UUID planIdentifier, Plan plan,
      Location childLocation) {
    Map<String, ColumnData> columns = new HashMap<>();
    Entry<String, ColumnData> totalStructuresCounts = getTotalStructuresCounts(

        plan, childLocation);
    columns.put(totalStructuresCounts.getKey(), totalStructuresCounts.getValue());

    Entry<String, ColumnData> totalStructuresTargetedCount = getTotalStructuresTargetedCount(
        planIdentifier, childLocation);
    columns.put(totalStructuresTargetedCount.getKey(), totalStructuresTargetedCount.getValue());

    Entry<String, ColumnData> totalStructuresFound = getTotalStructuresFound(planIdentifier, plan,
        childLocation);
    columns.put(totalStructuresFound.getKey(), totalStructuresFound.getValue());

    Entry<String, ColumnData> totalFoundCoverage = getTotalFoundCoverage(planIdentifier, plan,
        childLocation);
    columns.put(totalFoundCoverage.getKey(), totalFoundCoverage.getValue());

    Entry<String, ColumnData> operationalAreaVisited = operationalAreaVisitedCounts(planIdentifier,
        childLocation);
    columns.put(operationalAreaVisited.getKey(), operationalAreaVisited.getValue());

    Entry<String, ColumnData> noOfTreatedStructures = getNoOfTreatedStructures(planIdentifier, plan,
        childLocation);
    columns.put(noOfTreatedStructures.getKey(), noOfTreatedStructures.getValue());

    Entry<String, ColumnData> percentageOfTreatedStructuresToTotalStructures = getPercentageOfTreatedStructuresToTotalStructures(
        planIdentifier, plan, childLocation);
    columns.put(percentageOfTreatedStructuresToTotalStructures.getKey(),
        percentageOfTreatedStructuresToTotalStructures.getValue());

    Entry<String, ColumnData> percentageOfChildrenTreatedToPeopleEligible = getPercentageOfChildrenTreatedToPeopleEligible(
        planIdentifier, plan, childLocation);
    columns.put(percentageOfChildrenTreatedToPeopleEligible.getKey(),
        percentageOfChildrenTreatedToPeopleEligible.getValue());

    Entry<String, ColumnData> healthFacilityReferrals = getHealthFacilityReferrals();
    columns.put(healthFacilityReferrals.getKey(), healthFacilityReferrals.getValue());

    RowData rowData = new RowData();
    rowData.setLocationIdentifier(childLocation.getIdentifier());
    rowData.setColumnDataMap(columns);
    rowData.setLocationName(childLocation.getName());
    return rowData;
  }

  private RowData getMDAFullCoverageData(UUID planIdentifier, Plan plan, Location childLocation) {
    Map<String, ColumnData> columns = new HashMap<>();
    Entry<String, ColumnData> totalStructuresCounts = getTotalStructuresCounts(plan, childLocation);
    columns.put(totalStructuresCounts.getKey(), totalStructuresCounts.getValue());

    Entry<String, ColumnData> totalStructuresTargetedCount = getTotalStructuresTargetedCount(
        planIdentifier, childLocation);
    columns.put(totalStructuresTargetedCount.getKey(), totalStructuresTargetedCount.getValue());

    Entry<String, ColumnData> totalStructuresFound = getTotalStructuresFound(planIdentifier, plan,
        childLocation);
    columns.put(totalStructuresFound.getKey(), totalStructuresFound.getValue());

    Entry<String, ColumnData> totalFoundCoverage = getTotalFoundCoverage(planIdentifier, plan,
        childLocation);
    columns.put(totalFoundCoverage.getKey(), totalFoundCoverage.getValue());

    Entry<String, ColumnData> operationalAreaVisited = operationalAreaVisitedCounts(planIdentifier,
        childLocation);
    columns.put(operationalAreaVisited.getKey(), operationalAreaVisited.getValue());

    Entry<String, ColumnData> noOfTreatedStructures = getNoOfTreatedStructures(planIdentifier, plan,
        childLocation);
    columns.put(noOfTreatedStructures.getKey(), noOfTreatedStructures.getValue());

    Entry<String, ColumnData> percentageOfTreatedStructuresToTotalStructures = getPercentageOfTreatedStructuresToTotalStructures(
        planIdentifier, plan, childLocation);
    columns.put(percentageOfTreatedStructuresToTotalStructures.getKey(),
        percentageOfTreatedStructuresToTotalStructures.getValue());

    Entry<String, ColumnData> percentageOfChildrenTreatedToPeopleEligible = getPercentageOfChildrenTreatedToPeopleEligible(
        planIdentifier, plan, childLocation);
    columns.put(percentageOfChildrenTreatedToPeopleEligible.getKey(),
        percentageOfChildrenTreatedToPeopleEligible.getValue());

    RowData rowData = new RowData();
    rowData.setLocationIdentifier(childLocation.getIdentifier());
    rowData.setColumnDataMap(columns);
    rowData.setLocationName(childLocation.getName());
    return rowData;
  }

  private List<Location> getChildrenLocations(UUID parentLocationIdentifier, Boolean getChildren,
      Plan plan) {
    List<Location> childrenLocations;
    if (getChildren) {
      childrenLocations = locationRelationshipService.getChildrenLocations(
          plan.getLocationHierarchy().getIdentifier(), parentLocationIdentifier);
    } else {
      childrenLocations = List.of(locationService.findByIdentifier(parentLocationIdentifier));
    }
    return childrenLocations;
  }

  private UUID getParentLocation(UUID planIdentifier, UUID parentLocationIdentifier, Plan plan) {
    if (parentLocationIdentifier == null) {
      List<PlanLocations> planLocations = planLocationsService.getPlanLocationsByPlanIdentifier(
          planIdentifier);
      LocationRelationship locationRelationshipfetched = planLocations.stream().map(
              planLocation -> locationRelationshipService.getLocationRelationshipsForLocation(
                  plan.getLocationHierarchy().getIdentifier(),
                  planLocation.getLocation().getIdentifier()))
          .filter(locationRelationship -> locationRelationship.getParentLocation() == null)
          .findFirst().orElseThrow(() -> new NotFoundException("no parent"));

      parentLocationIdentifier = locationRelationshipfetched.getLocation().getIdentifier();

    }
    return parentLocationIdentifier;
  }


  private Entry<String, ColumnData> getPercentageOfChildrenTreatedToPeopleEligible(
      UUID planIdentifier, Plan plan, Location childLocation) {

    String personLocationBusinessStatusKey =
        planIdentifier + "_" + childLocation.getIdentifier() + "_" + plan.getLocationHierarchy()
            .getIdentifier();
    PersonBusinessStatus personLocationBusinessStatusObj = personBusinessStatus.get(
        personLocationBusinessStatusKey);

    double noOfChildrenTreated = 0;
    double noOfPeopleEligible = 0;
    if (personLocationBusinessStatusObj != null) {
      noOfChildrenTreated = personLocationBusinessStatusObj.getPersonTreated().size();
      noOfPeopleEligible = personLocationBusinessStatusObj.getPersonEligible().size();
    }

    double percentageOfChildrenTreatedToPeopleEligible =
        noOfPeopleEligible > 0 ? noOfChildrenTreated / noOfPeopleEligible * 100 : 0;
    ColumnData percentageOfChildrenTreatedToPeopleEligibleColumnData = new ColumnData();
    percentageOfChildrenTreatedToPeopleEligibleColumnData.setValue(
        percentageOfChildrenTreatedToPeopleEligible);
    percentageOfChildrenTreatedToPeopleEligibleColumnData.setMeta(
        noOfChildrenTreated + " / " + noOfPeopleEligible);
    percentageOfChildrenTreatedToPeopleEligibleColumnData.setIsPercentage(true);
    return new SimpleEntry<>(TREATMENT_COVERAGE,
        percentageOfChildrenTreatedToPeopleEligibleColumnData);
  }

  private Entry<String, ColumnData> getHealthFacilityReferrals() {

    ColumnData healthFacilityReferralsColumnData = new ColumnData();
    healthFacilityReferralsColumnData.setValue(0d);
    return new SimpleEntry<>(HEALTH_FACILITY_REFERRAL, healthFacilityReferralsColumnData);

  }


  private Entry<String, ColumnData> operationalAreaVisitedCounts(UUID planIdentifier,
      Location childLocation) {
    String operationalAreaVisitedQueryKey = planIdentifier + "_" + childLocation.getIdentifier();
    OperationalAreaVisitedCount operationalAreaVisitedObj = countOfOperationalArea.get(
        operationalAreaVisitedQueryKey);
    double operationalAreaVisitedCount = 0;
    if (operationalAreaVisitedObj != null) {
      operationalAreaVisitedCount = operationalAreaVisitedObj.getCount();
    }
    ColumnData operationalAreaVisitedColumnData = new ColumnData();
    operationalAreaVisitedColumnData.setValue(operationalAreaVisitedCount);
    operationalAreaVisitedColumnData.setIsPercentage(false);
    return new SimpleEntry<>(OPERATIONAL_AREA_VISITED, operationalAreaVisitedColumnData);
  }

  private Entry<String, ColumnData> getNoOfTreatedStructures(UUID planIdentifier, Plan plan,
      Location childLocation) {

    String personLocationBusinessStatusKey =
        planIdentifier + "_" + childLocation.getIdentifier() + "_" + plan.getLocationHierarchy()
            .getIdentifier();
    PersonBusinessStatus personLocationBusinessStatusObj = personBusinessStatus.get(
        personLocationBusinessStatusKey);

    double noOfTreatedStructures = 0;
    if (personLocationBusinessStatusObj != null) {
      noOfTreatedStructures = personLocationBusinessStatusObj.getLocationsTreated().size();
    }

    ColumnData noOfTreatedStructuresColumnData = new ColumnData();
    noOfTreatedStructuresColumnData.setValue(noOfTreatedStructures);
    noOfTreatedStructuresColumnData.setIsPercentage(false);
    return new SimpleEntry<>(TOTAL_STRUCTURES_RECEIVED_SPAQ, noOfTreatedStructuresColumnData);
  }

  private Entry<String, ColumnData> getPercentageOfTreatedStructuresToTotalStructures(
      UUID planIdentifier, Plan plan, Location childLocation) {

    String personLocationBusinessStatusKey =
        planIdentifier + "_" + childLocation.getIdentifier() + "_" + plan.getLocationHierarchy()
            .getIdentifier();
    PersonBusinessStatus personLocationBusinessStatusObj = personBusinessStatus.get(
        personLocationBusinessStatusKey);

    double noOfTreatedStructures = 0;
    if (personLocationBusinessStatusObj != null) {
      noOfTreatedStructures = personLocationBusinessStatusObj.getLocationsTreated().size();
    }

    String totalStructuresTargetedQueryKey = planIdentifier + "_" + childLocation.getIdentifier();
    Long totalStructuresTargetedCountObj = countOfAssignedStructures.get(
        totalStructuresTargetedQueryKey);
    double totalStructuresTargetedCount = 0;
    if (totalStructuresTargetedCountObj != null) {
      totalStructuresTargetedCount = totalStructuresTargetedCountObj;
    }

    double percentageOfTreatedStructuresToTotalStructures =
        totalStructuresTargetedCount > 0 ? noOfTreatedStructures / totalStructuresTargetedCount
            * 100 : 0;
    ColumnData percentageOfTreatedStructuresToTotalStructureColumnData = new ColumnData();
    percentageOfTreatedStructuresToTotalStructureColumnData.setValue(
        percentageOfTreatedStructuresToTotalStructures);
    percentageOfTreatedStructuresToTotalStructureColumnData.setMeta(
        noOfTreatedStructures + " / " + totalStructuresTargetedCount);
    percentageOfTreatedStructuresToTotalStructureColumnData.setIsPercentage(true);
    return new SimpleEntry<>(DISTRIBUTION_COVERAGE,
        percentageOfTreatedStructuresToTotalStructureColumnData);
  }


  private Entry<String, ColumnData> getTotalFoundCoverage(UUID planIdentifier, Plan plan,
      Location childLocation) {

    String totalStructuresTargetedQueryKey = planIdentifier + "_" + childLocation.getIdentifier();
    Long totalStructuresTargetedCountObj = countOfAssignedStructures.get(
        totalStructuresTargetedQueryKey);
    double totalStructuresTargetedCount = 0;
    if (totalStructuresTargetedCountObj != null) {
      totalStructuresTargetedCount = totalStructuresTargetedCountObj;
    }

    String notVisitedStructuresQueryKey =
        planIdentifier + "_" + childLocation.getIdentifier() + "_" + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Not Visited";
    Long notVisitedStructuresCountObj = countOfStructuresByBusinessStatus.get(
        notVisitedStructuresQueryKey);
    double notVisitedStructuresCount = 0;
    if (notVisitedStructuresCountObj != null) {
      notVisitedStructuresCount = notVisitedStructuresCountObj;
    }
    String notEligibleStructuresQueryKey =
        planIdentifier + "_" + childLocation.getIdentifier() + "_" + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Not Eligible";
    Long notEligibleStructuresObj = countOfStructuresByBusinessStatus.get(
        notEligibleStructuresQueryKey);
    double notEligibleStructures = 0;
    if (notEligibleStructuresObj != null) {
      notEligibleStructures = notEligibleStructuresObj;
    }
    double totalStructuresFound =
        totalStructuresTargetedCount - (notVisitedStructuresCount + notEligibleStructures);

    if (totalStructuresTargetedCount == totalStructuresFound) {
      if (notVisitedStructuresCount == 0 && notEligibleStructures == 0) {
        totalStructuresFound = 0; // this means there are no structures in the operational area
      }
    }

    double totalFoundCoverage =
        totalStructuresTargetedCount > 0 ? totalStructuresFound / totalStructuresTargetedCount * 100
            : 0;
    ColumnData totalFoundCoverageColumnData = new ColumnData();
    totalFoundCoverageColumnData.setValue(totalFoundCoverage);
    totalFoundCoverageColumnData.setMeta(
        totalStructuresFound + " / " + totalStructuresTargetedCount);
    totalFoundCoverageColumnData.setIsPercentage(true);
    return new SimpleEntry<>(FOUND_COVERAGE, totalFoundCoverageColumnData);
  }

  private Entry<String, ColumnData> getTotalStructuresFound(UUID planIdentifier, Plan plan,
      Location childLocation) {

    String totalStructuresTargetedQueryKey = planIdentifier + "_" + childLocation.getIdentifier();
    Long totalStructuresTargetedCountObj = countOfAssignedStructures.get(
        totalStructuresTargetedQueryKey);
    double totalStructuresTargetedCount = 0;
    if (totalStructuresTargetedCountObj != null) {
      totalStructuresTargetedCount = totalStructuresTargetedCountObj;
    }

    String notVisitedStructuresQueryKey =
        planIdentifier + "_" + childLocation.getIdentifier() + "_" + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Not Visited";
    Long notVisitedStructuresCountObj = countOfStructuresByBusinessStatus.get(
        notVisitedStructuresQueryKey);
    double notVisitedStructuresCount = 0;
    if (notVisitedStructuresCountObj != null) {
      notVisitedStructuresCount = notVisitedStructuresCountObj;
    }
    String notEligibleStructuresQueryKey =
        planIdentifier + "_" + childLocation.getIdentifier() + "_" + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Not Eligible";
    Long notEligibleStructuresObj = countOfStructuresByBusinessStatus.get(
        notEligibleStructuresQueryKey);
    double notEligibleStructures = 0;
    if (notEligibleStructuresObj != null) {
      notEligibleStructures = notEligibleStructuresObj;
    }
    double totalStructuresFound =
        totalStructuresTargetedCount - (notVisitedStructuresCount + notEligibleStructures);

    if (totalStructuresTargetedCount == totalStructuresFound) {
      if (notVisitedStructuresCount == 0 && notEligibleStructures == 0) {
        totalStructuresFound = 0; // this means there are no structures in the operational area
      }
    }
    ColumnData totalStructuresFoundColumnData = new ColumnData();
    totalStructuresFoundColumnData.setValue(totalStructuresFound);
    totalStructuresFoundColumnData.setIsPercentage(false);
    return new SimpleEntry<>(TOTAL_STRUCTURES_FOUND, totalStructuresFoundColumnData);

  }

  private Entry<String, ColumnData> getTotalStructuresTargetedCount(UUID planIdentifier,
      Location childLocation) {

    String totalStructuresTargetedQueryKey = planIdentifier + "_" + childLocation.getIdentifier();
    Long totalStructuresTargetedCountObj = countOfAssignedStructures.get(
        totalStructuresTargetedQueryKey);
    double totalStructuresTargetedCount = 0;
    if (totalStructuresTargetedCountObj != null) {
      totalStructuresTargetedCount = totalStructuresTargetedCountObj;
    }
    ColumnData totalStructuresTargetedColumnData = new ColumnData();
    totalStructuresTargetedColumnData.setValue(totalStructuresTargetedCount);
    totalStructuresTargetedColumnData.setIsPercentage(false);
    return new SimpleEntry<>(TOTAL_STRUCTURES_TARGETED, totalStructuresTargetedColumnData);
  }

  private Entry<String, ColumnData> getTotalStructuresCounts(Plan plan, Location childLocation) {
    String totalStructuresQueryKey =
        plan.getLocationHierarchy().getIdentifier() + "_" + childLocation.getIdentifier();
    Long totalStructuresCountObj = structureCounts.get(totalStructuresQueryKey);
    double totalStructuresCount = 0;
    if (totalStructuresCountObj != null) {
      totalStructuresCount = totalStructuresCountObj;
    }
    ColumnData totalStructuresColumnData = new ColumnData();
    totalStructuresColumnData.setValue(totalStructuresCount);
    totalStructuresColumnData.setIsPercentage(false);
    return new SimpleEntry<>(TOTAL_STRUCTURES, totalStructuresColumnData);
  }

  private void initDataStoresIfNecessary() {
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
              kafkaProperties.getStoreMap().get(KafkaConstants.tableOfOperationalAreaHierarchies),
              QueryableStoreTypes.keyValueStore()));

      personBusinessStatus = getKafkaStreams.getKafkaStreams().store(
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.personBusinessStatus),
              QueryableStoreTypes.keyValueStore()));
      datastoresInitialized = true;
    }
  }

  public FeatureSetResponse getDataForReport(String reportType, UUID planIdentifier, UUID parentIdentifier) {
    ReportTypeEnum reportTypeEnum = LookupUtil.lookup(ReportTypeEnum.class, reportType);
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    List<PlanLocationDetails> locationDetails = new ArrayList<>();
    if(parentIdentifier == null) {
      locationDetails.add(locationService.getRootLocationByPlanIdentifier(planIdentifier));
    }else {
      locationDetails = locationService.getLocationsByParentIdentifierAndPlanIdentifier(parentIdentifier, planIdentifier);
    }

    initDataStoresIfNecessary();
    Map<UUID, RowData> rowDataMap = locationDetails.stream().map(loc -> {
      switch (reportTypeEnum) {

        case MDA_FULL_COVERAGE:
          return getMDAFullCoverageData(planIdentifier, plan, loc.getLocation());

        case MDA_FULL_COVERAGE_OPERATIONAL_AREA_LEVEL:
          return getMDAFullCoverageOperationalAreaLevelData(planIdentifier, plan, loc.getLocation());

        case IRS_FULL_COVERAGE:
          return getIRSFullData(loc.getLocation());

      }
      return null;
    }).collect(Collectors.toMap(RowData::getLocationIdentifier, row -> row));

    FeatureSetResponse response = new FeatureSetResponse();
    response.setType("FeatureCollection");
    List<LocationResponse> locationResponses = locationDetails.stream()
        .map(loc -> LocationResponseFactory.fromPlanLocationDetails(loc, parentIdentifier))
        .collect(Collectors.toList());

    locationResponses.forEach(loc -> loc.getProperties().setColumnDataMap(rowDataMap.get(loc.getIdentifier()).getColumnDataMap()));
    response.setFeatures(locationResponses);
    response.setIdentifier(parentIdentifier);
    return response;
  }
}
