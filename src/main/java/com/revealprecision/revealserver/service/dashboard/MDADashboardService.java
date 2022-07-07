package com.revealprecision.revealserver.service.dashboard;


import static com.revealprecision.revealserver.service.dashboard.DashboardService.ALL_OTHER_LEVELS;
import static com.revealprecision.revealserver.service.dashboard.DashboardService.DIRECTLY_ABOVE_STRUCTURE_LEVEL;
import static com.revealprecision.revealserver.service.dashboard.DashboardService.STRUCTURE_LEVEL;
import static com.revealprecision.revealserver.service.dashboard.DashboardService.WITHIN_STRUCTURE_LEVEL;

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
import com.revealprecision.revealserver.messaging.message.OperationalAreaVisitedCount;
import com.revealprecision.revealserver.messaging.message.PersonBusinessStatusAggregate;
import com.revealprecision.revealserver.messaging.message.TreatedOperationalAreaAggregate;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.projection.PlanLocationDetails;
import com.revealprecision.revealserver.props.DashboardProperties;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.PersonService;
import com.revealprecision.revealserver.service.PlanLocationsService;
import com.revealprecision.revealserver.service.PlanService;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MDADashboardService {

  private final StreamsBuilderFactoryBean getKafkaStreams;
  private final KafkaProperties kafkaProperties;
  private final PersonService personService;
  private final DashboardProperties dashboardProperties;
  private final PlanLocationsService planLocationsService;

  //MDA
  private static final String TREATMENT_COVERAGE = "Treatment coverage";
  private static final String HEALTH_FACILITY_REFERRALS = "Health Facility Referrals";
  private static final String OPERATIONAL_AREA_VISITED = "Operational Area Visited";
  private static final String TOTAL_STRUCTURES_RECEIVED_SPAQ = "Total Structures Received SPAQ";
  public static final String DISTRIBUTION_COVERAGE = "Distribution Coverage";
  private static final String DISTRIBUTION_EFFECTIVENESS = "Distribution Effectiveness";
  private static final String FOUND_COVERAGE = "Found Coverage";
  private static final String TOTAL_STRUCTURES_FOUND = "Total Structures Found";
  private static final String TOTAL_STRUCTURES_TARGETED = "Total Structures Targeted";
  private static final String TOTAL_STRUCTURES_MDA = "Total Structures";
  private static final String VISITATION_COVERAGE_PERCENTAGE = "Visitation Coverage Percentage";
  private static final String DISTRIBUTION_COVERAGE_PERCENTAGE = "Distribution Coverage Percentage";
  private static final String STRUCTURE_DISTRIBUTION_EFFECTIVENESS_PERCENTAGE = "Structure Distribution Effectiveness Percentage";
  private static final String INDIVIDUAL_DISTRIBUTION_EFFECTIVENESS_PERCENTAGE = "Individual Distribution Effectiveness Percentage";
  private static final String STRUCTURE_STATUS = "Structure Status";
  private static final String NO_OF_ELIGIBLE_CHILDREN = "Number of Eligible Children";
  private static final String NO_OF_TREATED_CHILDREN = "Number of Treated Children";
  private static final String PERSON_FULLNAME = "Person full name";
  private static final String PERSON_AGE = "Person age";
  private static final String PERSON_STATE = "Person state";



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


  //TODO: dont really need the parent Identifier - using it for now to query the datastore, however ideally a datastore should be availble that can query on just plan and structure id
  public List<RowData> getMDAFullCoverageStructureLevelData(Plan plan,
      Location childLocation, UUID parentLocationIdentifier) {
    Map<String, ColumnData> columns = new HashMap<>();

    Entry<String, ColumnData> businessStateColumnData = getLocationBusinessState(plan,
        childLocation, STRUCTURE_STATUS, parentLocationIdentifier);
    columns.put(businessStateColumnData.getKey(), businessStateColumnData.getValue());

    Entry<String, ColumnData> noOfEligibleChildrenByLocationColumnData = getNoOfEligibleChildrenByLocation(
        plan,
        childLocation, NO_OF_ELIGIBLE_CHILDREN);
    columns.put(noOfEligibleChildrenByLocationColumnData.getKey(),
        noOfEligibleChildrenByLocationColumnData.getValue());

    Entry<String, ColumnData> noOfTreatedChildrenByLocationColumnData = getNoOfTreatedChildrenByLocation(
        plan,
        childLocation, NO_OF_TREATED_CHILDREN);
    columns.put(noOfTreatedChildrenByLocationColumnData.getKey(),
        noOfTreatedChildrenByLocationColumnData.getValue());

    Entry<String, ColumnData> healthFacilityReferrals = getHealthFacilityReferrals(
        HEALTH_FACILITY_REFERRALS);
    columns.put(healthFacilityReferrals.getKey(), healthFacilityReferrals.getValue());

    Entry<String, ColumnData> totalStructuresTargetedCount = getTotalStructuresTargetedCount(
        plan, childLocation, TOTAL_STRUCTURES_TARGETED);
    columns.put(totalStructuresTargetedCount.getKey(), totalStructuresTargetedCount.getValue());

    RowData rowData = new RowData();
    rowData.setLocationIdentifier(childLocation.getIdentifier());
    rowData.setColumnDataMap(columns);
    rowData.setLocationName(childLocation.getName());
    return List.of(rowData);
  }

  public List<RowData> getMDAFullWithinStructureLevelData(Plan plan,
      Location parentLocation) {
    Map<String, ColumnData> columns = new HashMap<>();

    List<PersonState> personData = getPersonData(plan, parentLocation.getIdentifier());

    List<RowData> rowDatas = new ArrayList<>();

    for (PersonState personState : personData) {

      ColumnData personFullName = new ColumnData();
      personFullName.setValue(
          personState.getPerson().getNameText() + " " + personState.getPerson().getNameFamily());
      personFullName.setMeta("personId: " + personState.getPerson().getIdentifier());
      personFullName.setDataType("string");
      personFullName.setIsPercentage(false);
      columns.put(PERSON_FULLNAME, personFullName);

      ColumnData personStateString = new ColumnData();
      personStateString.setValue(personState.getState());
      personStateString.setMeta(null);
      personStateString.setDataType("string");
      personStateString.setIsPercentage(false);
      columns.put(PERSON_STATE, personStateString);

      ColumnData personAge = new ColumnData();
      Period between = Period.between(
          Instant.ofEpochMilli(personState.getPerson().getBirthDate().getTime())
              .atZone(ZoneId.systemDefault())
              .toLocalDate(), LocalDate.now());
      personAge.setValue(
          between.getYears() + " years " + (between.getMonths() > 0 ? between.getMonths()
              + " months" : ""));
      personAge.setMeta(" DOB: " + personState.getPerson().getBirthDate());
      personAge.setDataType("string");
      personAge.setIsPercentage(false);

      columns.put(PERSON_AGE, personAge);

      RowData rowData = new RowData();
      rowData.setLocationIdentifier(parentLocation.getIdentifier());
      rowData.setColumnDataMap(columns);
      rowData.setLocationName(parentLocation.getName());
      rowDatas.add(rowData);
    }

    return rowDatas;
  }


  public List<RowData> getMDAFullCoverageOperationalAreaLevelData(Plan plan,
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
    return List.of(rowData);
  }

  public List<RowData> getMDAFullCoverageData(Plan plan, Location childLocation) {
    Map<String, ColumnData> columns = new LinkedHashMap<>();

    Entry<String, ColumnData> totalStructuresCounts = getTotalStructuresCounts(plan, childLocation,
        TOTAL_STRUCTURES_MDA);
    columns.put(totalStructuresCounts.getKey(), totalStructuresCounts.getValue());

    Entry<String, ColumnData> totalStructuresTargetedCount = getTotalStructuresTargetedCount(
        plan, childLocation, TOTAL_STRUCTURES_TARGETED);
    columns.put(totalStructuresTargetedCount.getKey(), totalStructuresTargetedCount.getValue());

    Entry<String, ColumnData> percentageOfChildrenTreatedToPeopleEligible = getPercentageOfChildrenTreatedToPeopleEligible(
        plan, childLocation, TREATMENT_COVERAGE);
    columns.put(percentageOfChildrenTreatedToPeopleEligible.getKey(),
        percentageOfChildrenTreatedToPeopleEligible.getValue());

    Entry<String, ColumnData> noOfTreatedStructures = getNoOfTreatedStructures(plan,
        childLocation, TOTAL_STRUCTURES_RECEIVED_SPAQ);
    columns.put(noOfTreatedStructures.getKey(), noOfTreatedStructures.getValue());

    Entry<String, ColumnData> totalStructuresFound = getTotalStructuresFound(plan,
        childLocation, TOTAL_STRUCTURES_FOUND);
    columns.put(totalStructuresFound.getKey(), totalStructuresFound.getValue());

    Entry<String, ColumnData> operationalAreaVisited = operationalAreaVisitedCounts(plan,
        childLocation, OPERATIONAL_AREA_VISITED);
    columns.put(operationalAreaVisited.getKey(), operationalAreaVisited.getValue());

    Entry<String, ColumnData> totalFoundCoverage = getTotalFoundCoverage(plan,
        childLocation, FOUND_COVERAGE);
    columns.put(totalFoundCoverage.getKey(), totalFoundCoverage.getValue());

    Entry<String, ColumnData> percentageOfTreatedStructuresToTotalStructures = getPercentageOfTreatedStructuresToTotalStructures(
        plan, childLocation, DISTRIBUTION_COVERAGE);
    columns.put(percentageOfTreatedStructuresToTotalStructures.getKey(),
        percentageOfTreatedStructuresToTotalStructures.getValue());

    Entry<String, ColumnData> percentageOfTreatedOperationalAreasToTotalOperationalAreas = operationalAreaTreatedPercentage(
        plan, childLocation, DISTRIBUTION_EFFECTIVENESS);
    columns.put(percentageOfTreatedOperationalAreasToTotalOperationalAreas.getKey(),
        percentageOfTreatedOperationalAreasToTotalOperationalAreas.getValue());

    RowData rowData = new RowData();
    rowData.setLocationIdentifier(childLocation.getIdentifier());
    rowData.setColumnDataMap(columns);
    rowData.setLocationName(childLocation.getName());
    return List.of(rowData);
  }


  private Entry<String, ColumnData> getPercentageOfChildrenTreatedToPeopleEligible(
      Plan plan, Location childLocation, String columnName) {

    String personLocationBusinessStatusKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy()
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
        "Number Of Children Treated: " + noOfChildrenTreated + " / "
            + "Number Of Children Eligible: " + noOfPeopleEligible);
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
    String operationalAreaVisitedQueryKey =
        childLocation.getIdentifier() + "_" + plan.getIdentifier();
    OperationalAreaVisitedCount operationalAreaVisitedObj = countOfOperationalArea.get(
        operationalAreaVisitedQueryKey);
    double operationalAreaVisitedCount = 0;
    if (operationalAreaVisitedObj != null) {
      operationalAreaVisitedCount = operationalAreaVisitedObj.getOperationalAreaVisitedCountMDA();
    }
    ColumnData operationalAreaVisitedColumnData = new ColumnData();
    operationalAreaVisitedColumnData.setValue(operationalAreaVisitedCount);
    operationalAreaVisitedColumnData.setIsPercentage(false);
    return new SimpleEntry<>(columnName, operationalAreaVisitedColumnData);
  }

  private Entry<String, ColumnData> operationalAreaTreatedPercentage(Plan plan,
      Location childLocation, String columnName) {
    String operationalAreaVisitedQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy()
            .getIdentifier();
    TreatedOperationalAreaAggregate treatedOperationalAreaAggregate = treatedOperationalCounts.get(
        operationalAreaVisitedQueryKey);
    double treatedOperationalAreaCount = 0;
    if (treatedOperationalAreaAggregate != null) {
      treatedOperationalAreaCount = (double) treatedOperationalAreaAggregate.getTreatLocationCount();
    }

    Long totalOperationAreaCounts = planLocationsService
        .getNumberOfAssignedChildrenByGeoLevelNameWithinLocationAndHierarchyAndPlan(
            plan.getIdentifier(),
            LocationConstants.OPERATIONAL,
            childLocation.getIdentifier(),
            plan.getLocationHierarchy().getIdentifier()
        );

    double distributionEffectiveness = 0;

    if (totalOperationAreaCounts > 0) {
      distributionEffectiveness =
          treatedOperationalAreaCount / (double) totalOperationAreaCounts * 100;
    }

    ColumnData treatedOperationalAreaColumnData = new ColumnData();
    treatedOperationalAreaColumnData.setValue(distributionEffectiveness);
    treatedOperationalAreaColumnData.setMeta(
        "treatedOperationalAreaCount: " + treatedOperationalAreaCount + " / "
            + "totalOperationAreaCounts: " + totalOperationAreaCounts);
    treatedOperationalAreaColumnData.setIsPercentage(true);
    return new SimpleEntry<>(columnName, treatedOperationalAreaColumnData);
  }

  private Entry<String, ColumnData> getNoOfTreatedStructures(Plan plan,
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
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Not Eligible";
    Long notEligibleStructuresCountObj = countOfStructuresByBusinessStatus.get(
        notEligibleStructuresQueryKey);
    double notEligibleStructuresCount = 0;
    if (notEligibleStructuresCountObj != null) {
      notEligibleStructuresCount = notEligibleStructuresCountObj;
    }

    double totalStructuresTargeted =
        totalStructuresInPlanLocationCount - notEligibleStructuresCount;

    double percentageOfTreatedStructuresToTotalStructures =
        totalStructuresInPlanLocationCount > 0 ? noOfTreatedStructures / totalStructuresTargeted
            * 100 : 0;

    ColumnData percentageOfTreatedStructuresToTotalStructureColumnData = new ColumnData();
    percentageOfTreatedStructuresToTotalStructureColumnData.setValue(
        percentageOfTreatedStructuresToTotalStructures);
    percentageOfTreatedStructuresToTotalStructureColumnData.setMeta(
        "No Of TreatedStructures: " + noOfTreatedStructures + " / " + "Total Structures Targeted: "
            + totalStructuresTargeted);
    percentageOfTreatedStructuresToTotalStructureColumnData.setIsPercentage(true);
    return new SimpleEntry<>(columnName,
        percentageOfTreatedStructuresToTotalStructureColumnData);
  }

  private List<PersonState> getPersonData(Plan plan,
      UUID parentLocationIdentifier) {

    String locationForPersonQueryKey =
        plan.getIdentifier() + "_" +
            parentLocationIdentifier;

    LocationPersonBusinessStateAggregate locationForPerson = structurePeople.get(
        locationForPersonQueryKey);

    if (locationForPerson != null) {
      return locationForPerson.getPersonBusinessStatusMap()
          .entrySet().stream().map(entry -> {
            PersonState personState = new PersonState();
            Person person = personService.getPersonByIdentifier(entry.getKey());
            personState.setPerson(person);
            personState.setState(entry.getValue().getStatus());
            return personState;
          })
          .collect(Collectors.toList());
    } else {
      return new ArrayList<>();
    }


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

  private Entry<String, ColumnData> getNoOfTreatedChildrenByLocation(Plan plan,
      Location childLocation, String columnName) {

//TODO: need to create a datastore for this metric
    String structurePeopleQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier();
    LocationPersonBusinessStateCountAggregate locationPersonBusinessStateCountAggregate = structurePeopleCounts.get(
        structurePeopleQueryKey);

    Long allTreatedPeopleInStructure = 0L;

    if (locationPersonBusinessStateCountAggregate != null) {
      Long allSMCCompletePeopleInStructure = locationPersonBusinessStateCountAggregate.getStructureBusinessStateCountMap()
          .entrySet()
          .stream()
          .filter(entry -> entry.getKey().equals("SMC Complete"))
          .map(entry -> entry.getValue() != null ? entry.getValue() : 0L)
          .reduce(0L, Long::sum);

      Long allSPAQCompletePeopleInStructure = locationPersonBusinessStateCountAggregate.getStructureBusinessStateCountMap()
          .entrySet()
          .stream().filter(entry -> entry.getKey().equals("SPAQ Complete"))
          .map(entry -> entry.getValue() != null ? entry.getValue() : 0L)
          .reduce(0L, Long::sum);

      allTreatedPeopleInStructure =
          allSMCCompletePeopleInStructure + allSPAQCompletePeopleInStructure;
    }
    ColumnData allTreatedPeopleInStructureColumnData = new ColumnData();
    allTreatedPeopleInStructureColumnData.setValue(allTreatedPeopleInStructure);
    allTreatedPeopleInStructureColumnData.setMeta(null);
    allTreatedPeopleInStructureColumnData.setIsPercentage(false);

    return new SimpleEntry<>(columnName, allTreatedPeopleInStructureColumnData);
  }

  private Entry<String, ColumnData> getNoOfEligibleChildrenByLocation(Plan plan,
      Location childLocation, String columnName) {

    String structurePeopleQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier();
    LocationPersonBusinessStateCountAggregate locationPersonBusinessStateCountAggregate = structurePeopleCounts.get(
        structurePeopleQueryKey);

    Long totalEligiblePeople = 0L;

    if (locationPersonBusinessStateCountAggregate != null) {
      Long allPeopleInStructure = locationPersonBusinessStateCountAggregate.getStructureBusinessStateCountMap()
          .entrySet().stream().map(entry -> entry.getValue() != null ? entry.getValue() : 0L)
          .reduce(0L, Long::sum);

      Long allIneligiblePeopleInStructure = locationPersonBusinessStateCountAggregate.getStructureBusinessStateCountMap()
          .entrySet()
          .stream().filter(entry -> entry.getKey().equals("Ineligible"))
          .map(entry -> entry.getValue() != null ? entry.getValue() : 0L)
          .reduce(0L, Long::sum);

      totalEligiblePeople = allPeopleInStructure - allIneligiblePeopleInStructure;
    }
    ColumnData noOfTreatedChildrenColumnData = new ColumnData();
    noOfTreatedChildrenColumnData.setValue(totalEligiblePeople);
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
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy()
            .getIdentifier() + "_" + "Not Eligible";
    Long notEligibleStructuresCountObj = countOfStructuresByBusinessStatus.get(
        notEligibleStructuresQueryKey);
    double notEligibleStructuresCount = 0;
    if (notEligibleStructuresCountObj != null) {
      notEligibleStructuresCount = notEligibleStructuresCountObj;
    }

    double totalStructuresTargeted =
        totalStructuresInPlanLocationCount - notEligibleStructuresCount;
    double totalStructuresFound = (totalStructuresTargeted - notVisitedStructuresCount);

    double totalFoundCoverage =
        totalStructuresTargeted > 0 ? totalStructuresFound / totalStructuresTargeted * 100
            : 0;
    ColumnData totalFoundCoverageColumnData = new ColumnData();
    totalFoundCoverageColumnData.setValue(totalFoundCoverage);
    totalFoundCoverageColumnData.setMeta(
        "Total Structures Found: " + totalStructuresFound + " / " + "Total Structures Targeted: "
            + totalStructuresTargeted);
    totalFoundCoverageColumnData.setIsPercentage(true);
    return new SimpleEntry<>(columnName, totalFoundCoverageColumnData);
  }

  private Entry<String, ColumnData> getTotalStructuresFound(Plan plan,
      Location childLocation, String columnName) {

    String totalStructuresTargetedQueryKey =
        plan.getIdentifier() + "_" + childLocation.getIdentifier();
    Long totalStructuresTargetedCountObj = countOfAssignedStructures.get(
        totalStructuresTargetedQueryKey);
    double totalStructuresTargetedCount = 0;
    if (totalStructuresTargetedCountObj != null) {
      totalStructuresTargetedCount = totalStructuresTargetedCountObj;
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
        plan.getIdentifier() + "_" + childLocation.getIdentifier() + "_"
            + plan.getLocationHierarchy()
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

  private List<LocationResponse> setGeoJsonProperties(Map<UUID, RowData> rowDataMap,
      List<LocationResponse> locationResponses) {
    return locationResponses.stream().peek(loc -> {
      loc.getProperties().setColumnDataMap(rowDataMap.get(loc.getIdentifier()).getColumnDataMap());
      loc.getProperties().setId(loc.getIdentifier());

      if (rowDataMap.get(loc.getIdentifier()).getColumnDataMap().get(DISTRIBUTION_COVERAGE)
          != null) {
        loc.getProperties().setDistCoveragePercent(
            rowDataMap.get(loc.getIdentifier()).getColumnDataMap().get(DISTRIBUTION_COVERAGE)
                .getValue());
      }
      if (rowDataMap.get(loc.getIdentifier()).getColumnDataMap()
          .get(DISTRIBUTION_COVERAGE_PERCENTAGE)
          != null) {
        loc.getProperties().setDistCoveragePercent(
            rowDataMap.get(loc.getIdentifier()).getColumnDataMap()
                .get(DISTRIBUTION_COVERAGE_PERCENTAGE)
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
    }).collect(Collectors.toList());
  }

  public FeatureSetResponse getFeatureSetResponse(UUID parentIdentifier,
      List<PlanLocationDetails> locationDetails,
      Map<UUID, RowData> rowDataMap, String reportLevel) {
    FeatureSetResponse response = new FeatureSetResponse();
    response.setType("FeatureCollection");
    List<LocationResponse> locationResponses = locationDetails.stream()
        .map(loc -> LocationResponseFactory.fromPlanLocationDetails(loc, parentIdentifier))
        .collect(Collectors.toList());

    if (!rowDataMap.isEmpty()) {
      locationResponses = setGeoJsonProperties(rowDataMap, locationResponses);
    }
    response.setDefaultDisplayColumn(dashboardProperties.getMdaDefaultDisplayColumns().get(reportLevel));
    response.setFeatures(locationResponses);
    response.setIdentifier(parentIdentifier);
    return response;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PersonState implements Serializable {

    private Person person;
    private String state;
  }
}
