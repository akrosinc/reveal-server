package com.revealprecision.revealserver.service.dashboard;


import static com.revealprecision.revealserver.service.dashboard.DashboardService.CDD_LEVEL;
import static com.revealprecision.revealserver.service.dashboard.DashboardService.IS_ON_PLAN_TARGET;
import static com.revealprecision.revealserver.service.dashboard.DashboardService.SUPERVISOR_LEVEL;
import static java.util.Map.entry;

import com.revealprecision.revealserver.api.v1.dto.factory.LocationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.models.ColumnData;
import com.revealprecision.revealserver.api.v1.dto.models.RowData;
import com.revealprecision.revealserver.api.v1.dto.models.RowDataForSupervisor;
import com.revealprecision.revealserver.api.v1.dto.models.RowDataWithSupervisorOrCdd;
import com.revealprecision.revealserver.api.v1.dto.response.FeatureSetResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationPropertyResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.LocationFormDataSumAggregateEvent;
import com.revealprecision.revealserver.messaging.message.mdalite.MDALiteLocationSupervisorListAggregation;
import com.revealprecision.revealserver.messaging.message.mdalite.MDALiteSupervisorCddListAggregation;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.projection.PlanLocationDetails;
import com.revealprecision.revealserver.props.DashboardProperties;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import com.revealprecision.revealserver.service.PlanLocationsService;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
public class MDALiteDashboardService2 {

  private final StreamsBuilderFactoryBean getKafkaStreams;
  private final KafkaProperties kafkaProperties;
  private final PlanLocationsService planLocationsService;
  private final LocationRelationshipService locationRelationshipService;
  private final DashboardProperties dashboardProperties;

  public static final String MALES_1_4 = "Male 1-4 years";
  private static final String MALES_5_14 = "Male 5-14 years";
  private static final String MALES_15 = "Male 15+ years";
  private static final String MALES_TOTAL = "Total Males";
  private static final String FEMALES_1_4 = "Female 1-4 years";
  private static final String FEMALES_5_14 = "Female 5-14 years";
  private static final String FEMALES_15 = "Female 15+ years";
  private static final String FEMALES_TOTAL = "Total Females";
  private static final String TOTAL_TREATED = "Total Treated";
  private static final String SCH_CENSUS_POP_TARGET = "SCH Census Pop Target";
  private static final String STH_CENSUS_POP_TARGET = "STH Census Pop Target";
  public static final String SCH_TREATMENT_COVERAGE = "SCH Treatment Coverage";
  public static final String STH_TREATMENT_COVERAGE = "STH Treatment Coverage";
  private static final String ADMINISTERED = "Administered";
  public static final String ADVERSE = "Adverse";
  public static final String DAYS_WORKED = "Days Worked";
  public static final String AVERAGE = "Average per day";
  public static final String SUPERVISOR_DISTRIBUTED = "Supervisor Distributed";
  public static final String RECEIVED_BY_CDD = "Received by CDD";
  public static final String RETURNED_TO_SUPERVISOR = "Returned to Supervisor";
  public static final String REMAINING_WITH_CDD = "Remaining with CDD";

  public static final String treatedMale1_4 = "treated-male-1-to-4";
  public static final String treatedMale5_14 = "treated-male-5-to-14";
  public static final String treatedMale15 = "treated-male-above-15";
  public static final String totalMales = "total-males";
  public static final String treatedFemale1_4 = "treated-female-1-to-4";
  public static final String treatedFemale5_14 = "treated-female-5-to-14";
  public static final String treatedFemale15 = "treated-female-above-15";
  public static final String totalFemale = "total-female";
  public static final String totalPeople = "total-people";
  public static final String mdaLiteAdverse = "mda-lite-adverse";
  public static final String administered = "mda-lite-administered";
  public static final String supervisorDistributed = "supervisor-distributed";
  public static final String cddReceived = "cdd-received";
  public static final String supervisorReturned = "supervisor-returned";
  public static final String cddRemaining = "cdd-remaining";
  public static final String schTargetPop = "sch-target-pop";
  public static final String sthTargetPop = "sth-target-pop";

  public static final String MEB = "MEB";
  public static final String ALB = "ALB";
  public static final String PZQ = "PZQ";

  public static final String STH = "STH";
  public static final String SCH = "STH";

  public static final String DRUG = "drug";

  private final Map<String, String> columnMap = Map.ofEntries(
      entry(MALES_1_4, treatedMale1_4),
      entry(MALES_5_14, treatedMale5_14),
      entry(MALES_15, treatedMale15),
      entry(MALES_TOTAL, totalMales),
      entry(FEMALES_1_4, treatedFemale1_4),
      entry(FEMALES_5_14, treatedFemale5_14),
      entry(FEMALES_15, treatedFemale15),
      entry(FEMALES_TOTAL, totalFemale),
      entry(TOTAL_TREATED, totalPeople),
      entry(ADMINISTERED, administered),
      entry(ADVERSE, mdaLiteAdverse),
      entry(SUPERVISOR_DISTRIBUTED, supervisorDistributed),
      entry(RECEIVED_BY_CDD, cddReceived),
      entry(RETURNED_TO_SUPERVISOR, supervisorReturned),
      entry(REMAINING_WITH_CDD, cddRemaining)
  );

  private final Map<String, String> supervisorColumnMap = Map.ofEntries(
      entry(ADMINISTERED, administered),
      entry(ADVERSE, mdaLiteAdverse),
      entry(SUPERVISOR_DISTRIBUTED, supervisorDistributed),
      entry(RECEIVED_BY_CDD, cddReceived),
      entry(RETURNED_TO_SUPERVISOR, supervisorReturned),
      entry(REMAINING_WITH_CDD, cddRemaining)
  );

  private final Map<String, String> cddColumnMap = Map.ofEntries(
      entry(MALES_1_4, treatedMale1_4),
      entry(MALES_5_14, treatedMale5_14),
      entry(MALES_15, treatedMale15),
      entry(MALES_TOTAL, totalMales),
      entry(FEMALES_1_4, treatedFemale1_4),
      entry(FEMALES_5_14, treatedFemale5_14),
      entry(FEMALES_15, treatedFemale15),
      entry(FEMALES_TOTAL, totalFemale),
      entry(TOTAL_TREATED, totalPeople),
      entry(ADMINISTERED, administered),
      entry(ADVERSE, mdaLiteAdverse),
      entry(SUPERVISOR_DISTRIBUTED, supervisorDistributed),
      entry(RECEIVED_BY_CDD, cddReceived),
      entry(RETURNED_TO_SUPERVISOR, supervisorReturned),
      entry(REMAINING_WITH_CDD, cddRemaining)
  );

  ReadOnlyKeyValueStore<String, LocationFormDataSumAggregateEvent> locationFormDataIntegerSumOrAverage;

  ReadOnlyKeyValueStore<String, MDALiteLocationSupervisorListAggregation> supervisors;

  ReadOnlyKeyValueStore<String, LocationFormDataSumAggregateEvent> supervisorLocationFormDataIntegerSumOrAverage;

  ReadOnlyKeyValueStore<String, LocationFormDataSumAggregateEvent> cddSupervisorLocationFormDataIntegerSumOrAverage;

  ReadOnlyKeyValueStore<String, MDALiteSupervisorCddListAggregation> cddNames;
  boolean datastoresInitialized = false;

  private String name(String constant, String drug) {

    return constant.concat(" (").concat(drug).concat(")");
  }

  public List<RowData> getMDALiteSupervisorCoverageData(Plan plan, Location childLocation,
      List<String> filters) {

    List<RowDataWithSupervisorOrCdd> rowDataWithSupervisorOrCdds = new ArrayList<>();

    String supervisorKey =
        plan.getIdentifier() + "_" + plan.getLocationHierarchy().getIdentifier() + "_"
            + childLocation.getIdentifier();

    MDALiteLocationSupervisorListAggregation locationFormDataSumAggregateEvent = supervisors.get(
        supervisorKey);

    Map<String, String> supervisorNames = locationFormDataSumAggregateEvent.getSupervisorNames();
    for (Entry<String, String> supervisor : supervisorNames.entrySet()) {
      Map<String, ColumnData> columns = new HashMap<>();
      if (filters == null || (filters.contains(ALB) || filters.isEmpty())) {
        columns.putAll(
            getSupervisorColumnDataMap(plan, childLocation, ALB, supervisor));
      }

      if (filters == null || (filters.contains(PZQ) || filters.isEmpty())) {
        columns.putAll(
            getSupervisorColumnDataMap(plan, childLocation, PZQ, supervisor));
      }

      if (filters == null || (filters.contains(MEB) || filters.isEmpty())) {
        columns.putAll(
            getSupervisorColumnDataMap(plan, childLocation, MEB, supervisor));
      }

      RowDataWithSupervisorOrCdd rowData = new RowDataWithSupervisorOrCdd();
      rowData.setSupervisor(supervisor.getKey());
      rowData.setSupervisorKey("CDD" + "_" + supervisor.getKey().concat("_")
          .concat(childLocation.getIdentifier().toString()));
      rowData.setMaps(columns);
      rowDataWithSupervisorOrCdds.add(rowData);
    }

    RowDataForSupervisor rowDataForSupervisor = new RowDataForSupervisor();
    rowDataForSupervisor.setRowDataWithSupervisorOrCdds(rowDataWithSupervisorOrCdds);
    rowDataForSupervisor.setLocationIdentifier(childLocation.getIdentifier());
    rowDataForSupervisor.setLocationName(childLocation.getName());
    return List.of(rowDataForSupervisor);
  }

  private Map<String, ColumnData> getSupervisorColumnDataMap(Plan plan, Location childLocation,
      String drug,
      Entry<String, String> supervisor) {
    return supervisorColumnMap.keySet().stream().map(
            s -> getSupervisorFormData(plan, childLocation,
                drug, columnMap.get(s), name(s, drug), supervisor)
        ).collect(Collectors.toList()).stream()
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  public List<RowData> getMDALiteCDDCoverageData(Plan plan, Location childLocation,
      List<String> filters, String parentIdentifierString) {
    List<RowDataWithSupervisorOrCdd> rowDataWithSupervisorOrCdds = new ArrayList<>();

    String supervisor = parentIdentifierString.split("_")[1];

    String supervisorKey =
        plan.getIdentifier() + "_" + plan.getLocationHierarchy().getIdentifier() + "_"
            + childLocation.getIdentifier()
            + "_" + supervisor;

    MDALiteSupervisorCddListAggregation locationFormDataSumAggregateEvent = cddNames.get(
        supervisorKey);

    if (locationFormDataSumAggregateEvent != null) {

      Set<String> cddNames = locationFormDataSumAggregateEvent.getCddNames();
      for (String cddName : cddNames) {
        Map<String, ColumnData> columnsToAdd = new HashMap<>();

        SimpleEntry<String, String> supervisorCddEntry = new SimpleEntry<>(supervisor,
            cddName);

        if (filters == null || (filters.contains(ALB) || filters.isEmpty())) {
          columnsToAdd.putAll(getCddColumnDashboardData(
              plan, childLocation, supervisorCddEntry, ALB));
        }

        if (filters == null || (filters.contains(PZQ) || filters.isEmpty())) {
          columnsToAdd.putAll(getCddColumnDashboardData(
              plan, childLocation, supervisorCddEntry, PZQ));
        }

        if (filters == null || (filters.contains(MEB) || filters.isEmpty())) {

          columnsToAdd.putAll(getCddColumnDashboardData(
              plan, childLocation, supervisorCddEntry, MEB));
        }

        RowDataWithSupervisorOrCdd rowData = new RowDataWithSupervisorOrCdd();
        rowData.setCdd(cddName);
        rowData.setCddKey(supervisor + "_" + cddName.concat("_")
            .concat(childLocation.getIdentifier().toString()));
        rowData.setMaps(columnsToAdd);
        rowDataWithSupervisorOrCdds.add(rowData);
      }

      RowDataForSupervisor rowDataForSupervisor = new RowDataForSupervisor();
      rowDataForSupervisor.setRowDataWithSupervisorOrCdds(rowDataWithSupervisorOrCdds);
      rowDataForSupervisor.setLocationIdentifier(childLocation.getIdentifier());
      rowDataForSupervisor.setLocationName(childLocation.getName());
      return List.of(rowDataForSupervisor);
    }
    return null;
  }

  private Map<String, ColumnData> getCddColumnDashboardData(Plan plan, Location childLocation,
      SimpleEntry<String, String> supervisorCddEntry, String drug) {
    Map<String, ColumnData> columns = new HashMap<>(
        getCddColumnData(plan, childLocation, supervisorCddEntry, drug));

    Entry<String, ColumnData> daysWorked = getDaysWorked(plan, childLocation,
        drug, name(DAYS_WORKED, drug), supervisorCddEntry);
    columns.put(daysWorked.getKey(), daysWorked.getValue());

    Entry<String, ColumnData> daysWorkedAverage = getDaysWorkedAverage(plan, childLocation,
        drug, name(AVERAGE, drug), supervisorCddEntry);

    columns.put(daysWorkedAverage.getKey(), daysWorkedAverage.getValue());
    return columns;
  }

  private Map<String, ColumnData> getCddColumnData(Plan plan, Location childLocation,
      SimpleEntry<String, String> supervisorCddEntry, String drug) {
    return cddColumnMap.keySet().stream().map(
            columnName -> getCddFormData(plan, childLocation,
                drug, cddColumnMap.get(columnName), name(columnName, drug), supervisorCddEntry)
        ).collect(Collectors.toList())
        .stream()
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  public List<RowData> getMDALiteCoverageData(Plan plan, Location childLocation,
      List<String> filters) {
    Map<String, ColumnData> columns = new HashMap<>();

    if (filters == null || (filters.contains(ALB) || filters.isEmpty())) {
      columns.putAll(getDashboardData(plan, childLocation, ALB));
    }

    if (filters == null || (filters.contains(PZQ) || filters.isEmpty())) {
      columns.putAll(getDashboardData(plan, childLocation, PZQ));
    }

    if (filters == null || (filters.contains(MEB) || filters.isEmpty())) {
      columns.putAll(getDashboardData(plan, childLocation, MEB));
    }

    if (filters == null || filters.contains(MEB) || filters.contains(ALB)) {
      Entry<String, ColumnData> sthCensusPopulationTarget = getCensusPopulationTargetColumnMap(plan,
          childLocation,
          sthTargetPop, STH_CENSUS_POP_TARGET);
      columns.put(sthCensusPopulationTarget.getKey(), sthCensusPopulationTarget.getValue());

      Entry<String, ColumnData> treatmentCoverage = getTreatmentCoverageTarget(plan,
          childLocation,
          STH, STH_TREATMENT_COVERAGE);
      columns.put(treatmentCoverage.getKey(), treatmentCoverage.getValue());
    }
    if (filters == null || filters.contains(PZQ)) {
      Entry<String, ColumnData> schCensusPopulationTarget = getCensusPopulationTargetColumnMap(plan,
          childLocation,
          schTargetPop, SCH_CENSUS_POP_TARGET);
      columns.put(schCensusPopulationTarget.getKey(), schCensusPopulationTarget.getValue());

      Entry<String, ColumnData> treatmentCoverage = getTreatmentCoverageTarget(plan,
          childLocation,
          SCH, SCH_TREATMENT_COVERAGE);
      columns.put(treatmentCoverage.getKey(), treatmentCoverage.getValue());

    }

    RowData rowData = new RowData();
    rowData.setLocationIdentifier(childLocation.getIdentifier());
    rowData.setColumnDataMap(columns);
    rowData.setLocationName(childLocation.getName());

    return List.of(rowData);
  }

  private Map<String, ColumnData> getDashboardData(Plan plan, Location childLocation, String drug) {
    return columnMap.keySet().stream().map(
            s -> getFormData(plan, childLocation,
                drug, columnMap.get(s), name(s, drug))

        ).collect(Collectors.toList()).stream()
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  private Entry<String, ColumnData> getSupervisorFormData(Plan plan, Location childLocation,
      String drug, String searchKey, String columnName, Entry<String, String> supervisor) {

    String key = plan.getIdentifier() + "_" + plan.getLocationHierarchy().getIdentifier() + "_"
        + childLocation.getIdentifier()
        + "_" + searchKey + "-" + drug + "_" + supervisor.getKey();

    LocationFormDataSumAggregateEvent locationFormDataSumAggregateEvent = supervisorLocationFormDataIntegerSumOrAverage.get(
        key);

    Long treatedMaleOneToFour = 0L;
    if (locationFormDataSumAggregateEvent != null) {
      Long sum = locationFormDataSumAggregateEvent.getSum();
      if (sum != null) {
        treatedMaleOneToFour = sum;
      }
    }

    ColumnData columnData = new ColumnData();
    columnData.setDataType("integer");
    columnData.setValue(treatedMaleOneToFour);
    columnData.setIsPercentage(false);

    return new SimpleEntry<>(columnName,
        columnData);
  }

  private Entry<String, ColumnData> getCddFormData(Plan plan, Location childLocation, String drug,
      String searchKey, String columnName, Entry<String, String> cdd) {

    String key = plan.getIdentifier() + "_" + plan.getLocationHierarchy().getIdentifier() + "_"
        + childLocation.getIdentifier()
        + "_" + searchKey + "-" + drug + "_" + cdd.getKey() + "_" + cdd.getValue();

    LocationFormDataSumAggregateEvent locationFormDataSumAggregateEvent = cddSupervisorLocationFormDataIntegerSumOrAverage.get(
        key);

    Long treatedMaleOneToFour = 0L;
    if (locationFormDataSumAggregateEvent != null) {
      Long sum = locationFormDataSumAggregateEvent.getSum();
      if (sum != null) {
        treatedMaleOneToFour = sum;
      }
    }

    ColumnData columnData = new ColumnData();
    columnData.setDataType("integer");
    columnData.setValue(treatedMaleOneToFour);
    columnData.setIsPercentage(false);

    return new SimpleEntry<>(columnName,
        columnData);
  }

  private Entry<String, ColumnData> getDaysWorked(Plan plan, Location childLocation, String drug,
      String columnName, Entry<String, String> cdd) {

    String key = plan.getIdentifier() + "_" + plan.getLocationHierarchy().getIdentifier() + "_"
        + childLocation.getIdentifier()
        + "_" + totalPeople + "-" + drug + "_" + cdd.getKey() + "_" + cdd.getValue();

    LocationFormDataSumAggregateEvent locationFormDataSumAggregateEvent = cddSupervisorLocationFormDataIntegerSumOrAverage.get(
        key);

    Long daysWorked = 0L;
    if (locationFormDataSumAggregateEvent != null) {
      Long counter = locationFormDataSumAggregateEvent.getCounter();
      if (counter != null) {
        daysWorked = counter;
      }
    }

    ColumnData columnData = new ColumnData();
    columnData.setDataType("integer");
    columnData.setValue(daysWorked);
    columnData.setIsPercentage(false);

    return new SimpleEntry<>(columnName,
        columnData);
  }


  private Entry<String, ColumnData> getCensusPopulationTargetColumnMap(Plan plan, Location childLocation,
      String searchKey, String columnName) {

    Long censusTargetPopulation = getCensusTargetPopulation(plan, childLocation, searchKey);

    ColumnData columnData = new ColumnData();
    columnData.setDataType("integer");
    columnData.setValue(censusTargetPopulation);
    columnData.setIsPercentage(false);

    return new SimpleEntry<>(columnName,
        columnData);
  }

  private Entry<String, ColumnData> getTreatmentCoverageTarget(Plan plan, Location childLocation,
      String infection, String columnName) {

    double treatmentCoverage = 0;
    String meta = "";
    if (infection.equals(STH)) {

      Long sthCensusTargetPopulation = getCensusTargetPopulation( plan,  childLocation, sthTargetPop);

      Long totalPeopleALB = getTotalPeople(plan,childLocation, ALB);

      Long totalPeopleMEB = getTotalPeople(plan,childLocation, MEB);

      Long totalSTHPeople = totalPeopleALB + totalPeopleMEB;

      if (sthCensusTargetPopulation > 0) {
        treatmentCoverage = (double) totalSTHPeople / (double) sthCensusTargetPopulation * 100;
      }
      meta =
          "Total People Treated for STH: " + totalSTHPeople +
              " / STH Census Target Population: " + sthCensusTargetPopulation;
    }
    if (infection.equals(SCH)) {

      Long schCensusTargetPopulation = getCensusTargetPopulation( plan,  childLocation, schTargetPop);

      Long totalPeoplePZQ = getTotalPeople(plan,childLocation, PZQ);

      if (schCensusTargetPopulation > 0) {
        treatmentCoverage = (double) totalPeoplePZQ / (double) schCensusTargetPopulation * 100;
      }
      meta =
          "Total People Treated for SCH: " + schCensusTargetPopulation + " / SCH Census Target Population: "
              + schCensusTargetPopulation;

    }

    ColumnData columnData = new ColumnData();
    columnData.setDataType("integer");
    columnData.setValue(treatmentCoverage);
    columnData.setIsPercentage(true);
    columnData.setMeta(meta);

    return new SimpleEntry<>(columnName,
        columnData);
  }

  private Long getCensusTargetPopulation(Plan plan, Location childLocation,
      String searchKey) {

    String key = plan.getIdentifier() + "_" + plan.getLocationHierarchy().getIdentifier() + "_"
        + childLocation.getIdentifier() + "_" + searchKey;

    LocationFormDataSumAggregateEvent censusTargetPopulationEvent = locationFormDataIntegerSumOrAverage.get(
        key);

    Long censusTargetPopulation = 0L;
    if (censusTargetPopulationEvent != null) {
      censusTargetPopulation = censusTargetPopulationEvent.getSum();
    }
    return censusTargetPopulation;
  }

  private Long getTotalPeople(Plan plan, Location childLocation, String drug) {

    String totalPeopleKey =
        plan.getIdentifier() + "_" + plan.getLocationHierarchy().getIdentifier() + "_"
            + childLocation.getIdentifier() + "_" + totalPeople + "-" + drug;

    LocationFormDataSumAggregateEvent totalPeopleTreatedEvent = locationFormDataIntegerSumOrAverage.get(
        totalPeopleKey);
    Long totalPeopleALB = 0L;
    if (totalPeopleTreatedEvent != null) {
      totalPeopleALB = totalPeopleTreatedEvent.getSum();
    }
    return totalPeopleALB;
  }

  private Entry<String, ColumnData> getDaysWorkedAverage(Plan plan, Location childLocation,
      String drug, String columnName, Entry<String, String> cdd) {

    String key = plan.getIdentifier() + "_" + plan.getLocationHierarchy().getIdentifier() + "_"
        + childLocation.getIdentifier()
        + "_" + totalPeople + "-" + drug + "_" + cdd.getKey() + "_" + cdd.getValue();

    LocationFormDataSumAggregateEvent locationFormDataSumAggregateEvent = cddSupervisorLocationFormDataIntegerSumOrAverage.get(
        key);

    Long treated = 0L;
    if (locationFormDataSumAggregateEvent != null) {
      Long sum = locationFormDataSumAggregateEvent.getSum();
      if (sum != null) {
        treated = sum;
      }
    }

    Long daysWorked = 0L;
    if (locationFormDataSumAggregateEvent != null) {
      Long counter = locationFormDataSumAggregateEvent.getCounter();
      if (counter != null) {
        daysWorked = counter;
      }
    }

    double average = 0L;
    if (daysWorked > 0) {
      average = (double) treated / daysWorked;
    }

    ColumnData columnData = new ColumnData();
    columnData.setDataType("integer");
    columnData.setValue(average);
    columnData.setIsPercentage(false);
    columnData.setMeta("Total Treated: " + treated + " / daysWorked: " + daysWorked);

    return new SimpleEntry<>(columnName,
        columnData);
  }


  private Entry<String, ColumnData> getFormData(Plan plan, Location childLocation, String drug,
      String searchKey, String columnName) {
    String key = plan.getIdentifier() + "_" + plan.getLocationHierarchy().getIdentifier() + "_"
        + childLocation.getIdentifier()
        + "_" + searchKey + "-" + drug;

    LocationFormDataSumAggregateEvent locationFormDataSumAggregateEvent = locationFormDataIntegerSumOrAverage.get(
        key);

    Long treatedMaleOneToFour = 0L;
    if (locationFormDataSumAggregateEvent != null) {
      Long sum = locationFormDataSumAggregateEvent.getSum();
      if (sum != null) {
        treatedMaleOneToFour = sum;
      }
    }

    ColumnData columnData = new ColumnData();
    columnData.setDataType("integer");
    columnData.setValue(treatedMaleOneToFour);
    columnData.setIsPercentage(false);

    return new SimpleEntry<>(columnName,
        columnData);
  }

  private List<LocationResponse> setDefaultGeoJsonProperties(Map<UUID, RowData> rowDataMap,
      List<LocationResponse> locationResponses) {
    return locationResponses.stream().peek(loc -> {

      loc.getProperties().setColumnDataMap(rowDataMap.get(loc.getIdentifier()).getColumnDataMap());
      loc.getProperties().setId(loc.getIdentifier().toString());
    }).collect(Collectors.toList());
  }


  private List<LocationResponse> setGeoJsonPropertiesWithSupervisorOrCdd(
      Map<UUID, RowData> rowDataMap,
      List<LocationResponse> locationResponses) {

    return locationResponses.stream().flatMap(loc ->
        ((RowDataForSupervisor) rowDataMap.get(
            loc.getIdentifier())).getRowDataWithSupervisorOrCdds().stream().map(
            rowDataWithSupervisorOrCdd -> {
              LocationResponse response = new LocationResponse();
              response.setGeometry(loc.getGeometry());
              response.setIdentifier(loc.getIdentifier());
              response.setType(loc.getType());
              response.setIsActive(loc.getIsActive());
              if (response.getProperties() == null) {
                response.setProperties(new LocationPropertyResponse());
              }

              response.getProperties().setName(rowDataWithSupervisorOrCdd.getSupervisor());
              response.getProperties().setId(rowDataWithSupervisorOrCdd.getSupervisorKey());
              response.getProperties().setColumnDataMap(rowDataWithSupervisorOrCdd.getMaps());
              return response;
            })).collect(Collectors.toList());
  }


  private List<LocationResponse> setGeoJsonPropertiesOnPlanTarget(Map<UUID, RowData> rowDataMap,
      List<LocationResponse> locationResponses) {

    return locationResponses.stream().peek(loc -> {
      loc.getProperties().setColumnDataMap(rowDataMap.get(loc.getIdentifier()).getColumnDataMap());
      loc.getProperties().setId("SUPERVISOR_" + "placeholder_" + loc.getIdentifier().toString());
    }).collect(Collectors.toList());
  }


  public FeatureSetResponse getFeatureSetResponse(UUID parentIdentifier,
      List<PlanLocationDetails> locationDetails,
      Map<UUID, RowData> rowDataMap, String reportLevel, List<String> filters) {
    FeatureSetResponse response = new FeatureSetResponse();
    response.setType("FeatureCollection");
    List<LocationResponse> locationResponses;
    if (reportLevel.equals(IS_ON_PLAN_TARGET) ||
        reportLevel.equals(CDD_LEVEL) ||
        reportLevel.equals(SUPERVISOR_LEVEL)) {

      locationDetails = locationDetails.stream()
          .map(locationDetail -> getDummyPlanLocationDetails(rowDataMap, locationDetail))
          .collect(Collectors.toList());
      locationResponses = locationDetails.stream()
          .map(loc -> LocationResponseFactory.fromPlanLocationDetails(loc, parentIdentifier))
          .collect(Collectors.toList());

    } else {
      locationResponses = locationDetails.stream()
          .map(loc -> LocationResponseFactory.fromPlanLocationDetails(loc, parentIdentifier))
          .collect(Collectors.toList());
    }
    locationResponses = setGeojsonResponseProperties(rowDataMap, reportLevel, locationResponses);
    response.setDefaultDisplayColumn(
        getDefaultColumn(filters, reportLevel)
    );
    response.setFeatures(locationResponses);
    response.setIdentifier(parentIdentifier);
    return response;
  }
  private PlanLocationDetails getDummyPlanLocationDetails(Map<UUID, RowData> rowDataMap,
      PlanLocationDetails locationDetail) {
    PlanLocationDetails planLocationDetails = new PlanLocationDetails();
    planLocationDetails.setLocation(locationDetail.getLocation());
    planLocationDetails.setChildrenNumber((long) rowDataMap.size());
    planLocationDetails.setHasChildren(true);
    planLocationDetails.setAssignedLocations(0L);
    planLocationDetails.setAssignedTeams(0L);
    return planLocationDetails;
  }


  private List<LocationResponse> setGeojsonResponseProperties(Map<UUID, RowData> rowDataMap,
      String reportLevel, List<LocationResponse> locationResponses) {
    switch (reportLevel) {
      case IS_ON_PLAN_TARGET:
        locationResponses = setGeoJsonPropertiesOnPlanTarget(rowDataMap, locationResponses);
        break;
      case CDD_LEVEL:
      case SUPERVISOR_LEVEL:
        locationResponses = setGeoJsonPropertiesWithSupervisorOrCdd(rowDataMap, locationResponses);
        break;
      default:
        locationResponses = setDefaultGeoJsonProperties(rowDataMap, locationResponses);
    }
    return locationResponses;
  }

  private String getDefaultColumn(List<String> filters, String reportLevel) {
    String defaultFilter = (filters == null ? ALB
        : (filters.contains(ALB) ? ALB : filters.contains(MEB) ? MEB : PZQ));
    String defaultColumn = dashboardProperties.getMdaLiteDefaultDisplayColumns().getOrDefault(
        defaultFilter +
            reportLevel, null);
    String name = defaultColumn;
    if (!defaultColumn.equals(STH_TREATMENT_COVERAGE) && !defaultColumn.equals(
        SCH_TREATMENT_COVERAGE)) {
      name = name(defaultColumn, defaultFilter);
    }
    return name;
  }

  public void initDataStoresIfNecessary() {
    if (!datastoresInitialized) {
      locationFormDataIntegerSumOrAverage = getKafkaStreams.getKafkaStreams().store(
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.locationFormDataIntegerSumOrAverage),
              QueryableStoreTypes.keyValueStore()));

      supervisorLocationFormDataIntegerSumOrAverage = getKafkaStreams.getKafkaStreams().store(
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap()
                  .get(KafkaConstants.supervisorLocationFormDataIntegerSumOrAverage),
              QueryableStoreTypes.keyValueStore()));

      cddSupervisorLocationFormDataIntegerSumOrAverage = getKafkaStreams.getKafkaStreams().store(
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap()
                  .get(KafkaConstants.cddSupervisorLocationFormDataIntegerSumOrAverage),
              QueryableStoreTypes.keyValueStore()));

      supervisors = getKafkaStreams.getKafkaStreams().store(
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.mdaLiteSupervisors),
              QueryableStoreTypes.keyValueStore()));

      cddNames = getKafkaStreams.getKafkaStreams().store(
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.cddNames),
              QueryableStoreTypes.keyValueStore()));

      datastoresInitialized = true;
    }
  }

}
