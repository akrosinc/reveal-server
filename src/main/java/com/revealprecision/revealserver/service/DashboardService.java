package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.LocationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.models.ColumnData;
import com.revealprecision.revealserver.api.v1.dto.models.RowData;
import com.revealprecision.revealserver.api.v1.dto.response.FeatureSetResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.enums.ApplicableReportsEnum;
import com.revealprecision.revealserver.enums.LookupUtil;
import com.revealprecision.revealserver.enums.ReportTypeEnum;
import com.revealprecision.revealserver.exceptions.WrongEnumException;
import com.revealprecision.revealserver.messaging.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.LocationBusinessStatusAggregate;
import com.revealprecision.revealserver.messaging.message.OperationalAreaVisitedCount;
import com.revealprecision.revealserver.messaging.message.PersonBusinessStatusAggregate;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.projection.PlanLocationDetails;
import com.revealprecision.revealserver.props.KafkaProperties;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
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
public class DashboardService {

  public static final String TREATMENT_COVERAGE = "Treatment coverage";
  public static final String HEALTH_FACILITY_REFERRALS = "Health Facility Referrals";
  public static final String OPERATIONAL_AREA_VISITED = "Operational Area Visited";
  public static final String TOTAL_STRUCTURES_RECEIVED_SPAQ = "Total Structures Received SPAQ";
  public static final String DISTRIBUTION_COVERAGE = "Distribution Coverage";
  public static final String FOUND_COVERAGE = "Found Coverage";
  public static final String TOTAL_STRUCTURES_FOUND = "Total Structures Found";
  public static final String TOTAL_STRUCTURES_TARGETED = "Total Structures Targeted";
  public static final String TOTAL_STRUCTURES = "Total Structures";
  public static final String VISITATION_COVERAGE_PERCENTAGE = "Visitation Coverage Percentage";
  public static final String DISTRIBUTION_COVERAGE_PERCENTAGE = "Distribution Coverage Percentage";
  public static final String STRUCTURE_DISTRIBUTION_EFFECTIVENESS_PERCENTAGE = "Structure Distribution Effectiveness Percentage";
  public static final String INDIVIDUAL_DISTRIBUTION_EFFECTIVENESS_PERCENTAGE = "Individual Distribution Effectiveness Percentage";
  public static final String STRUCTURE_BUSINESS_STATE = "Structure Business State";
  public static final String NO_OF_ELIGIBLE_CHILDREN = "Number of Eligible Children";
  public static final String NO_OF_TREATED_CHILDREN = "Number of Treated Children";


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
  ReadOnlyKeyValueStore<String, PersonBusinessStatusAggregate> personBusinessStatus;
  ReadOnlyKeyValueStore<String, LocationBusinessStatusAggregate> locationBusinessState;
  boolean datastoresInitialized = false;


  private RowData getIRSFullData(Location childLocation) {
    Map<String, ColumnData> columns = new HashMap<>();
    RowData rowData = new RowData();
    rowData.setLocationIdentifier(childLocation.getIdentifier());
    rowData.setColumnDataMap(columns);
    rowData.setLocationName(childLocation.getName());
    return rowData;
  }

  //TODO: dont really need the parent Identifier - using it for now to query the datastore, however ideally a datastore should be availble that can query on just plan and structure id
  private RowData getMDAFullCoverageStructureLevelData(Plan plan,
      Location childLocation, UUID parentLocationIdentifier) {
    Map<String, ColumnData> columns = new HashMap<>();

    Entry<String, ColumnData> businessStateColumnData = getLocationBusinessState(plan,
        childLocation, STRUCTURE_BUSINESS_STATE, parentLocationIdentifier);
    columns.put(businessStateColumnData.getKey(), businessStateColumnData.getValue());

    Entry<String, ColumnData> noOfEligibleChildrenByLocationColumnData = getNoOfEligibleChildrenByLocation(
        plan,
        childLocation, NO_OF_ELIGIBLE_CHILDREN, parentLocationIdentifier);
    columns.put(noOfEligibleChildrenByLocationColumnData.getKey(),
        noOfEligibleChildrenByLocationColumnData.getValue());

    Entry<String, ColumnData> noOfTreatedChildrenByLocationColumnData = getNoOfTreatedChildrenByLocation(
        plan,
        childLocation, NO_OF_TREATED_CHILDREN, parentLocationIdentifier);
    columns.put(noOfTreatedChildrenByLocationColumnData.getKey(),
        noOfTreatedChildrenByLocationColumnData.getValue());

    Entry<String, ColumnData> healthFacilityReferrals = getHealthFacilityReferrals(
        HEALTH_FACILITY_REFERRALS);
    columns.put(healthFacilityReferrals.getKey(), healthFacilityReferrals.getValue());

    RowData rowData = new RowData();
    rowData.setLocationIdentifier(childLocation.getIdentifier());
    rowData.setColumnDataMap(columns);
    rowData.setLocationName(childLocation.getName());
    return rowData;
  }


  private RowData getMDAFullCoverageOperationalAreaLevelData(Plan plan,
      Location childLocation) {
    Map<String, ColumnData> columns = new HashMap<>();

    Entry<String, ColumnData> totalFoundCoverage = getTotalFoundCoverage(plan,
        childLocation, VISITATION_COVERAGE_PERCENTAGE);
    columns.put(totalFoundCoverage.getKey(), totalFoundCoverage.getValue());

    Entry<String, ColumnData> percentageOfTreatedStructuresToTotalStructures = getPercentageOfTreatedStructuresToTotalStructures(
        plan, childLocation, DISTRIBUTION_COVERAGE_PERCENTAGE);
    columns.put(percentageOfTreatedStructuresToTotalStructures.getKey(),
        percentageOfTreatedStructuresToTotalStructures.getValue());

    Entry<String, ColumnData> percentageOfChildrenTreatedToPeopleEligible = getPercentageOfChildrenTreatedToPeopleEligible(
        plan, childLocation, INDIVIDUAL_DISTRIBUTION_EFFECTIVENESS_PERCENTAGE);
    columns.put(percentageOfChildrenTreatedToPeopleEligible.getKey(),
        percentageOfChildrenTreatedToPeopleEligible.getValue());

    Entry<String, ColumnData> healthFacilityReferrals = getHealthFacilityReferrals(
        HEALTH_FACILITY_REFERRALS);
    columns.put(healthFacilityReferrals.getKey(), healthFacilityReferrals.getValue());

    RowData rowData = new RowData();
    rowData.setLocationIdentifier(childLocation.getIdentifier());
    rowData.setColumnDataMap(columns);
    rowData.setLocationName(childLocation.getName());
    return rowData;
  }

  private RowData getMDAFullCoverageData(Plan plan, Location childLocation) {
    Map<String, ColumnData> columns = new HashMap<>();

    Entry<String, ColumnData> totalStructuresCounts = getTotalStructuresCounts(plan, childLocation,
        TOTAL_STRUCTURES);
    columns.put(totalStructuresCounts.getKey(), totalStructuresCounts.getValue());

    Entry<String, ColumnData> totalStructuresTargetedCount = getTotalStructuresTargetedCount(
        plan, childLocation, TOTAL_STRUCTURES_TARGETED);
    columns.put(totalStructuresTargetedCount.getKey(), totalStructuresTargetedCount.getValue());

    Entry<String, ColumnData> totalStructuresFound = getTotalStructuresFound(plan,
        childLocation, TOTAL_STRUCTURES_FOUND);
    columns.put(totalStructuresFound.getKey(), totalStructuresFound.getValue());

    Entry<String, ColumnData> totalFoundCoverage = getTotalFoundCoverage(plan,
        childLocation, FOUND_COVERAGE);
    columns.put(totalFoundCoverage.getKey(), totalFoundCoverage.getValue());

    Entry<String, ColumnData> operationalAreaVisited = operationalAreaVisitedCounts(plan, childLocation, OPERATIONAL_AREA_VISITED);
    columns.put(operationalAreaVisited.getKey(), operationalAreaVisited.getValue());

    Entry<String, ColumnData> noOfTreatedStructures = getNoOfTreatedStructures(plan,
        childLocation, TOTAL_STRUCTURES_RECEIVED_SPAQ);
    columns.put(noOfTreatedStructures.getKey(), noOfTreatedStructures.getValue());

    Entry<String, ColumnData> percentageOfTreatedStructuresToTotalStructures = getPercentageOfTreatedStructuresToTotalStructures(
        plan, childLocation, DISTRIBUTION_COVERAGE);
    columns.put(percentageOfTreatedStructuresToTotalStructures.getKey(),
        percentageOfTreatedStructuresToTotalStructures.getValue());

    Entry<String, ColumnData> percentageOfChildrenTreatedToPeopleEligible = getPercentageOfChildrenTreatedToPeopleEligible(
       plan, childLocation, TREATMENT_COVERAGE);
    columns.put(percentageOfChildrenTreatedToPeopleEligible.getKey(),
        percentageOfChildrenTreatedToPeopleEligible.getValue());

    RowData rowData = new RowData();
    rowData.setLocationIdentifier(childLocation.getIdentifier());
    rowData.setColumnDataMap(columns);
    rowData.setLocationName(childLocation.getName());
    return rowData;
  }


  private Entry<String, ColumnData> getPercentageOfChildrenTreatedToPeopleEligible(
     Plan plan, Location childLocation, String columnName) {

    String personLocationBusinessStatusKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_" + plan.getLocationHierarchy()
            .getIdentifier();
    PersonBusinessStatusAggregate personLocationBusinessStatusObj = personBusinessStatus.get(
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
        "Number Of Children Treated: "+noOfChildrenTreated + " / " + "Number Of Children Eligible: "+noOfPeopleEligible);
    percentageOfChildrenTreatedToPeopleEligibleColumnData.setIsPercentage(true);
    return new SimpleEntry<>(columnName,
        percentageOfChildrenTreatedToPeopleEligibleColumnData);
  }

  private Entry<String, ColumnData> getHealthFacilityReferrals(String columnName) {

    ColumnData healthFacilityReferralsColumnData = new ColumnData();
    healthFacilityReferralsColumnData.setValue(0d);
    return new SimpleEntry<>(columnName, healthFacilityReferralsColumnData);

  }


  private Entry<String, ColumnData> operationalAreaVisitedCounts(Plan plan,
      Location childLocation, String columnName) {
    String operationalAreaVisitedQueryKey = childLocation.getIdentifier() + "_" + plan.getIdentifier();
    OperationalAreaVisitedCount operationalAreaVisitedObj = countOfOperationalArea.get(
        operationalAreaVisitedQueryKey);
    double operationalAreaVisitedCount = 0;
    if (operationalAreaVisitedObj != null) {
      operationalAreaVisitedCount = operationalAreaVisitedObj.getOperationalAreaVisitedCount();
    }
    ColumnData operationalAreaVisitedColumnData = new ColumnData();
    operationalAreaVisitedColumnData.setValue(operationalAreaVisitedCount);
    operationalAreaVisitedColumnData.setIsPercentage(false);
    return new SimpleEntry<>(columnName, operationalAreaVisitedColumnData);
  }

  private Entry<String, ColumnData> getNoOfTreatedStructures(Plan plan,
      Location childLocation, String columnName) {

    String personLocationBusinessStatusKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_" + plan.getLocationHierarchy()
            .getIdentifier();
    PersonBusinessStatusAggregate personLocationBusinessStatusObj = personBusinessStatus.get(
        personLocationBusinessStatusKey);

    double noOfTreatedStructures = 0;
    if (personLocationBusinessStatusObj != null) {
      noOfTreatedStructures = personLocationBusinessStatusObj.getLocationsTreated().size();
    }

    ColumnData noOfTreatedStructuresColumnData = new ColumnData();
    noOfTreatedStructuresColumnData.setValue(noOfTreatedStructures);
    noOfTreatedStructuresColumnData.setIsPercentage(false);
    return new SimpleEntry<>(columnName, noOfTreatedStructuresColumnData);
  }

  private Entry<String, ColumnData> getPercentageOfTreatedStructuresToTotalStructures(Plan plan,
      Location childLocation, String columnName) {

    String personLocationBusinessStatusKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy()
            .getIdentifier();
    PersonBusinessStatusAggregate personLocationBusinessStatusObj = personBusinessStatus.get(
        personLocationBusinessStatusKey);

    double noOfTreatedStructures = 0;
    if (personLocationBusinessStatusObj != null) {
      noOfTreatedStructures = personLocationBusinessStatusObj.getLocationsTreated().size();
    }

    String totalStructuresTargetedQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier();
    Long totalStructuresInPlanLocationCountObj = countOfAssignedStructures.get(
        totalStructuresTargetedQueryKey);
    double totalStructuresInPlanLocationCount = 0;
    if (totalStructuresInPlanLocationCountObj != null) {
      totalStructuresInPlanLocationCount = totalStructuresInPlanLocationCountObj;
    }

    String notEligibleStructuresQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_" + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Not Eligible";
    Long notEligibleStructuresCountObj = countOfStructuresByBusinessStatus.get(
        notEligibleStructuresQueryKey);
    double notEligibleStructuresCount = 0;
    if (notEligibleStructuresCountObj != null) {
      notEligibleStructuresCount = notEligibleStructuresCountObj;
    }

    double totalStructuresTargeted = totalStructuresInPlanLocationCount - notEligibleStructuresCount;


    double percentageOfTreatedStructuresToTotalStructures =
        totalStructuresInPlanLocationCount > 0 ? noOfTreatedStructures / totalStructuresTargeted
            * 100 : 0;


    ColumnData percentageOfTreatedStructuresToTotalStructureColumnData = new ColumnData();
    percentageOfTreatedStructuresToTotalStructureColumnData.setValue(
        percentageOfTreatedStructuresToTotalStructures);
    percentageOfTreatedStructuresToTotalStructureColumnData.setMeta(
        "No Of TreatedStructures: "+noOfTreatedStructures + " / " + "Total Structures Targeted: "+totalStructuresTargeted);
    percentageOfTreatedStructuresToTotalStructureColumnData.setIsPercentage(true);
    return new SimpleEntry<>(columnName,
        percentageOfTreatedStructuresToTotalStructureColumnData);
  }

  private Entry<String, ColumnData> getLocationBusinessState(Plan plan,
      Location childLocation, String columnName, UUID parentLocationIdentifier) {

    String businessStateDataStoreQueryKey =
        plan.getIdentifier() + "_" +
            plan.getLocationHierarchy().getIdentifier() + "_" +
            parentLocationIdentifier + "_" +
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

  private Entry<String, ColumnData> getNoOfEligibleChildrenByLocation(Plan plan,
      Location childLocation, String columnName, UUID parentLocationIdentifier) {

//TODO: need to create a datastore for this metric

    ColumnData noOfEligibleChildrenColumnData = new ColumnData();
    noOfEligibleChildrenColumnData.setValue(0L);
    noOfEligibleChildrenColumnData.setMeta(null);
    noOfEligibleChildrenColumnData.setIsPercentage(false);

    return new SimpleEntry<>(columnName, noOfEligibleChildrenColumnData);
  }

  private Entry<String, ColumnData> getNoOfTreatedChildrenByLocation(Plan plan,
      Location childLocation, String columnName, UUID parentLocationIdentifier) {

//TODO: need to create a datastore for this metric

    ColumnData noOfTreatedChildrenColumnData = new ColumnData();
    noOfTreatedChildrenColumnData.setValue(0L);
    noOfTreatedChildrenColumnData.setMeta(null);
    noOfTreatedChildrenColumnData.setIsPercentage(false);

    return new SimpleEntry<>(columnName, noOfTreatedChildrenColumnData);
  }

  private Entry<String, ColumnData> getTotalFoundCoverage(Plan plan,
      Location childLocation, String columnName) {

    String totalStructuresTargetedQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier();
    Long totalStructuresInPlanLocationCountObj = countOfAssignedStructures.get(
        totalStructuresTargetedQueryKey);
    double totalStructuresInPlanLocationCount = 0;
    if (totalStructuresInPlanLocationCountObj != null) {
      totalStructuresInPlanLocationCount = totalStructuresInPlanLocationCountObj;
    }

    String notVisitedStructuresQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Not Visited";
    Long notVisitedStructuresCountObj = countOfStructuresByBusinessStatus.get(
        notVisitedStructuresQueryKey);
    double notVisitedStructuresCount = 0;
    if (notVisitedStructuresCountObj != null) {
      notVisitedStructuresCount = notVisitedStructuresCountObj;
    }

    String notEligibleStructuresQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_" + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Not Eligible";
    Long notEligibleStructuresCountObj = countOfStructuresByBusinessStatus.get(
        notEligibleStructuresQueryKey);
    double notEligibleStructuresCount = 0;
    if (notEligibleStructuresCountObj != null) {
      notEligibleStructuresCount = notEligibleStructuresCountObj;
    }

    double totalStructuresTargeted = totalStructuresInPlanLocationCount - notEligibleStructuresCount;
    double totalStructuresFound = (totalStructuresTargeted - notVisitedStructuresCount);

    double totalFoundCoverage =
        totalStructuresTargeted > 0 ? totalStructuresFound / totalStructuresTargeted * 100
            : 0;
    ColumnData totalFoundCoverageColumnData = new ColumnData();
    totalFoundCoverageColumnData.setValue(totalFoundCoverage);
    totalFoundCoverageColumnData.setMeta(
        "Total Structures Found: "+totalStructuresFound + " / " +  "Total Structures Targeted: "+totalStructuresTargeted);
    totalFoundCoverageColumnData.setIsPercentage(true);
    return new SimpleEntry<>(columnName, totalFoundCoverageColumnData);
  }

  private Entry<String, ColumnData> getTotalStructuresFound(Plan plan,
      Location childLocation, String columnName) {

    String totalStructuresTargetedQueryKey = plan.getIdentifier() + "_" + childLocation.getIdentifier();
    Long totalStructuresTargetedCountObj = countOfAssignedStructures.get(
        totalStructuresTargetedQueryKey);
    double totalStructuresTargetedCount = 0;
    if (totalStructuresTargetedCountObj != null) {
      totalStructuresTargetedCount = totalStructuresTargetedCountObj;
    }

    String notVisitedStructuresQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_" + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Not Visited";
    Long notVisitedStructuresCountObj = countOfStructuresByBusinessStatus.get(
        notVisitedStructuresQueryKey);
    double notVisitedStructuresCount = 0;
    if (notVisitedStructuresCountObj != null) {
      notVisitedStructuresCount = notVisitedStructuresCountObj;
    }

    String notEligibleStructuresQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_" + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Not Eligible";
    Long notEligibleStructuresCountObj = countOfStructuresByBusinessStatus.get(
        notEligibleStructuresQueryKey);
    double notEligibleStructuresCount = 0;
    if (notEligibleStructuresCountObj != null) {
      notEligibleStructuresCount = notEligibleStructuresCountObj;
    }

    double totalStructuresFound =
        ((totalStructuresTargetedCount - notEligibleStructuresCount) - notVisitedStructuresCount);

    ColumnData totalStructuresFoundColumnData = new ColumnData();
    totalStructuresFoundColumnData.setValue(totalStructuresFound);
    totalStructuresFoundColumnData.setIsPercentage(false);
    return new SimpleEntry<>(columnName, totalStructuresFoundColumnData);

  }

  private Entry<String, ColumnData> getTotalStructuresTargetedCount(Plan plan,
      Location childLocation, String columnName) {

    String totalStructuresTargetedQueryKey = plan.getIdentifier() + "_" + childLocation.getIdentifier();
    Long totalStructuresTargetedCountObj = countOfAssignedStructures.get(
        totalStructuresTargetedQueryKey);
    double totalStructuresInPlanLocationCount = 0;
    if (totalStructuresTargetedCountObj != null) {
      totalStructuresInPlanLocationCount = totalStructuresTargetedCountObj;
    }

    String notEligibleStructuresQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_" + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Not Eligible";
    Long notEligibleStructuresCountObj = countOfStructuresByBusinessStatus.get(
        notEligibleStructuresQueryKey);
    double notEligibleStructuresCount = 0;
    if (notEligibleStructuresCountObj != null) {
      notEligibleStructuresCount = notEligibleStructuresCountObj;
    }

    double totalStructuresInTargetedCount = totalStructuresInPlanLocationCount - notEligibleStructuresCount;

    ColumnData totalStructuresTargetedColumnData = new ColumnData();
    totalStructuresTargetedColumnData.setValue(totalStructuresInTargetedCount);
    totalStructuresTargetedColumnData.setIsPercentage(false);
    return new SimpleEntry<>(columnName, totalStructuresTargetedColumnData);
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
    ColumnData totalStructuresColumnData = new ColumnData();
    totalStructuresColumnData.setValue(totalStructuresCount);
    totalStructuresColumnData.setIsPercentage(false);
    return new SimpleEntry<>(columnName, totalStructuresColumnData);
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
      datastoresInitialized = true;
    }
  }
  public FeatureSetResponse getDataForReport(String reportType, UUID planIdentifier,
      UUID parentIdentifier) {


    ReportTypeEnum reportTypeEnum = LookupUtil.lookup(ReportTypeEnum.class, reportType);
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    List<String> applicableReportTypes = ApplicableReportsEnum.valueOf(
        plan.getInterventionType().getCode()).getReportName();
    if (!applicableReportTypes.contains(reportTypeEnum.name())) {
      throw new WrongEnumException(
          "Report type: '" + reportType + "' is not applicable to plan with identifier: '"
              + planIdentifier + "'");
    }
    List<PlanLocationDetails> locationDetails = new ArrayList<>();
    if (parentIdentifier == null) {
      locationDetails.add(locationService.getRootLocationByPlanIdentifier(planIdentifier));
    } else {
      Location location = locationService.findByIdentifier(parentIdentifier);
      int structureNodeIndex = plan.getLocationHierarchy().getNodeOrder().indexOf("structure");
      int locationNodeIndex = plan.getLocationHierarchy().getNodeOrder().indexOf(location.getGeographicLevel().getName());
      if(locationNodeIndex + 1 < structureNodeIndex) {
        locationDetails = locationService.getAssignedLocationsByParentIdentifierAndPlanIdentifier(
            parentIdentifier, planIdentifier, (locationNodeIndex + 2) == structureNodeIndex);
      }else {
        locationDetails = locationService.getLocationsByParentIdentifierAndPlanIdentifier(
            parentIdentifier, planIdentifier);
      }
    }

    initDataStoresIfNecessary();
    Map<UUID, RowData> rowDataMap = locationDetails.stream().map(loc -> {
          switch (reportTypeEnum) {

            case MDA_FULL_COVERAGE:

              switch (loc.getLocation().getGeographicLevel().getName()) {
                case "structure":
                  return getMDAFullCoverageStructureLevelData(plan, loc.getLocation(),
                      parentIdentifier);
                case "operational":
                  return getMDAFullCoverageOperationalAreaLevelData(plan,
                      loc.getLocation());
                default:
                  return getMDAFullCoverageData(plan, loc.getLocation());
              }

            case IRS_FULL_COVERAGE:
              return getIRSFullData(loc.getLocation());

          }
          return null;
        }).filter(Objects::nonNull)
        .collect(Collectors.toMap(RowData::getLocationIdentifier, row -> row));

    FeatureSetResponse response = new FeatureSetResponse();
    response.setType("FeatureCollection");
    List<LocationResponse> locationResponses = locationDetails.stream()
        .map(loc -> LocationResponseFactory.fromPlanLocationDetails(loc, parentIdentifier))
        .collect(Collectors.toList());

    locationResponses.forEach(loc -> {
      loc.getProperties().setColumnDataMap(rowDataMap.get(loc.getIdentifier()).getColumnDataMap());
      loc.getProperties().setId(loc.getIdentifier());


      if (rowDataMap.get(loc.getIdentifier()).getColumnDataMap().get(DISTRIBUTION_COVERAGE)
          != null) {
        loc.getProperties().setDistCoveragePercent(
            rowDataMap.get(loc.getIdentifier()).getColumnDataMap().get(DISTRIBUTION_COVERAGE)
                .getValue());
      }
      if (rowDataMap.get(loc.getIdentifier()).getColumnDataMap().get(DISTRIBUTION_COVERAGE_PERCENTAGE)
          != null) {
        loc.getProperties().setDistCoveragePercent(
            rowDataMap.get(loc.getIdentifier()).getColumnDataMap().get(DISTRIBUTION_COVERAGE_PERCENTAGE)
                .getValue());
      }
      if (rowDataMap.get(loc.getIdentifier()).getColumnDataMap().get(NO_OF_ELIGIBLE_CHILDREN)
          != null) {
        loc.getProperties().setNumberOfChildrenEligible(
            rowDataMap.get(loc.getIdentifier()).getColumnDataMap().get(NO_OF_ELIGIBLE_CHILDREN)
                .getValue());
      }
      if (rowDataMap.get(loc.getIdentifier()).getColumnDataMap().get(NO_OF_TREATED_CHILDREN)
          != null) {
        loc.getProperties().setNumberOfChildrenTreated(
            rowDataMap.get(loc.getIdentifier()).getColumnDataMap().get(NO_OF_TREATED_CHILDREN)
                .getValue());
      }
    });
    response.setFeatures(locationResponses);
    response.setIdentifier(parentIdentifier);
    return response;
  }
}
