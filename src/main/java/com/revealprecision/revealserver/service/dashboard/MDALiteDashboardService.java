package com.revealprecision.revealserver.service.dashboard;


import static com.revealprecision.revealserver.service.dashboard.DashboardService.CDD_LEVEL;
import static com.revealprecision.revealserver.service.dashboard.DashboardService.IS_ON_PLAN_TARGET;
import static com.revealprecision.revealserver.service.dashboard.DashboardService.SUPERVISOR_LEVEL;
import static java.util.Map.entry;

import com.revealprecision.revealserver.api.v1.dto.factory.LocationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.models.ColumnData;
import com.revealprecision.revealserver.api.v1.dto.models.RowData;
import com.revealprecision.revealserver.api.v1.dto.models.RowDataForSupervisor;
import com.revealprecision.revealserver.api.v1.dto.response.FeatureSetResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationPropertyResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.messaging.message.RowDataWithSupervisorOrCdd;
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
public class MDALiteDashboardService {

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
  private static final String CENSUS_POP_TARGET = "Census Pop Target";
  private static final String ADMINISTERED = "Administered";
  private static final String REMAINING = "Remaining";
  public static final String ADVERSE = "Adverse";
  public static final String DAYS_WORKED = "Days Worked";
  public static final String AVERAGE = "Average per day";


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
  public static final String administered = "mda-lite-adminstered";

  public static final String MBZ = "MBZ";
  public static final String ALB = "ALB";
  public static final String PZQ = "PZQ";

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
      entry(ADVERSE,mdaLiteAdverse)
  );

  private final Map<String, String> supervisorColumnMap = Map.of(
      ADMINISTERED, administered,
      ADVERSE, mdaLiteAdverse
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
      entry(ADVERSE,mdaLiteAdverse)
  );

//  ReadOnlyKeyValueStore<String, LocationFormDataSumAggregateEvent> locationFormDataIntegerSumOrAverage;
//
//  ReadOnlyKeyValueStore<String, MDALiteLocationSupervisorListAggregation> supervisors;
//
//  ReadOnlyKeyValueStore<String, LocationFormDataSumAggregateEvent> supervisorLocationFormDataIntegerSumOrAverage;
//
//  ReadOnlyKeyValueStore<String, LocationFormDataSumAggregateEvent> cddSupervisorLocationFormDataIntegerSumOrAverage;
//
//  ReadOnlyKeyValueStore<String, MDALiteSupervisorCddListAggregation> cddNames;
  boolean datastoresInitialized = false;

  private String name(String constant, String drug) {

    return constant.concat(" (").concat(drug).concat(")");
  }

  public List<RowData> getMDALiteSupervisorCoverageData(Plan plan, Location childLocation,
      List<String> filters) {
    List<RowData> rowDatas = new ArrayList<>();

    List<RowDataWithSupervisorOrCdd> rowDataWithSupervisorOrCdds = new ArrayList<>();


    Map<String, String> supervisorNames = Map.of(UUID.randomUUID().toString(),"Supervisor1");
    for (Entry<String, String> supervisor : supervisorNames.entrySet()) {
      Map<String, ColumnData> columns = new HashMap<>();
      if (filters == null || (filters.contains(ALB) || filters.isEmpty())) {
        List<Entry<String, ColumnData>> ALBEntries = supervisorColumnMap.keySet().stream().map(
            s -> getSupervisorFormData(plan, childLocation,
                ALB, supervisorColumnMap.get(s), name(s, ALB), supervisor)

        ).collect(Collectors.toList());
        Map<String, ColumnData> ALBdata = ALBEntries.stream()
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        columns.putAll(ALBdata);
      }

      if (filters == null || (filters.contains(PZQ) || filters.isEmpty())) {
        List<Entry<String, ColumnData>> PZQEntries = supervisorColumnMap.keySet().stream().map(
            s -> getSupervisorFormData(plan, childLocation,
                PZQ, supervisorColumnMap.get(s), name(s, PZQ), supervisor)

        ).collect(Collectors.toList());
        Map<String, ColumnData> PZQdata = PZQEntries.stream()
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        columns.putAll(PZQdata);
      }

      if (filters == null || (filters.contains(MBZ) || filters.isEmpty())) {
        List<Entry<String, ColumnData>> MBZEntries = supervisorColumnMap.keySet().stream().map(
            s -> getSupervisorFormData(plan, childLocation,
                MBZ, supervisorColumnMap.get(s), name(s, MBZ), supervisor)
        ).collect(Collectors.toList());
        Map<String, ColumnData> MBZData = MBZEntries.stream()
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        columns.putAll(MBZData);
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

  public List<RowData> getMDALiteCDDCoverageData(Plan plan, Location childLocation,
      List<String> filters, String parentIdentifierString) {
    List<RowData> rowDatas = new ArrayList<>();

    List<RowDataWithSupervisorOrCdd> rowDataWithSupervisorOrCdds = new ArrayList<>();

    String supervisor = parentIdentifierString.split("_")[1];

    String supervisorKey =
        plan.getIdentifier() + "_" + plan.getLocationHierarchy().getIdentifier() + "_"
            + childLocation.getIdentifier()
            + "_" + supervisor;

//    MDALiteSupervisorCddListAggregation locationFormDataSumAggregateEvent = cddNames.get(
//        supervisorKey);
//
//    if (locationFormDataSumAggregateEvent != null) {

      Set<String> cddNames = Set.of("Cdd1");
      for (String cddName : cddNames) {
        Map<String, ColumnData> columns = new HashMap<>();
        SimpleEntry<String, String> stringStringSimpleEntry = new SimpleEntry<>(supervisor,
            cddName);
        if (filters == null || (filters.contains(ALB) || filters.isEmpty())) {

          List<Entry<String, ColumnData>> ALBEntries = cddColumnMap.keySet().stream().map(
              s -> getCddFormData(plan, childLocation,
                  ALB, cddColumnMap.get(s), name(s, ALB), stringStringSimpleEntry)

          ).collect(Collectors.toList());
          Map<String, ColumnData> ALBdata = ALBEntries.stream()
              .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
          columns.putAll(ALBdata);

          Entry<String, ColumnData> daysWorked = getDaysWorked(plan, childLocation,
              ALB, DAYS_WORKED, name(DAYS_WORKED, ALB), stringStringSimpleEntry);

          columns.put(daysWorked.getKey(),daysWorked.getValue());

          Entry<String, ColumnData> daysWorkedAverage = getDaysWorkedAverage(plan, childLocation,
              ALB, AVERAGE, name(AVERAGE, ALB), stringStringSimpleEntry);

          columns.put(daysWorkedAverage.getKey(),daysWorkedAverage.getValue());

        }

        if (filters == null || (filters.contains(PZQ) || filters.isEmpty())) {
          List<Entry<String, ColumnData>> PZQEntries = cddColumnMap.keySet().stream().map(
              s -> getCddFormData(plan, childLocation,
                  PZQ, cddColumnMap.get(s), name(s, PZQ), stringStringSimpleEntry)

          ).collect(Collectors.toList());
          Map<String, ColumnData> PZQdata = PZQEntries.stream()
              .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
          columns.putAll(PZQdata);
        }

        if (filters == null || (filters.contains(MBZ) || filters.isEmpty())) {
          List<Entry<String, ColumnData>> MBZEntries = cddColumnMap.keySet().stream().map(
              s -> getCddFormData(plan, childLocation,
                  MBZ, cddColumnMap.get(s), name(s, MBZ), stringStringSimpleEntry)
          ).collect(Collectors.toList());
          Map<String, ColumnData> MBZData = MBZEntries.stream()
              .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
          columns.putAll(MBZData);
        }



        RowDataWithSupervisorOrCdd rowData = new RowDataWithSupervisorOrCdd();
        rowData.setCdd(cddName);
        rowData.setCddKey(supervisor + "_" + cddName.concat("_")
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


  public List<RowData> getMDALiteCoverageData(Plan plan, Location childLocation,
      List<String> filters) {
    Map<String, ColumnData> columns = new HashMap<>();

    if (filters == null || (filters.contains(ALB) || filters.isEmpty())) {
      List<Entry<String, ColumnData>> ALBEntries = columnMap.keySet().stream().map(
          s -> getFormData(plan, childLocation,
              ALB, columnMap.get(s), name(s, ALB))

      ).collect(Collectors.toList());
      Map<String, ColumnData> ALBdata = ALBEntries.stream()
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      columns.putAll(ALBdata);
    }

    if (filters == null || (filters.contains(PZQ) || filters.isEmpty())) {
      List<Entry<String, ColumnData>> PZQEntries = columnMap.keySet().stream().map(
          s -> getFormData(plan, childLocation,
              PZQ, columnMap.get(s), name(s, PZQ))

      ).collect(Collectors.toList());
      Map<String, ColumnData> PZQdata = PZQEntries.stream()
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      columns.putAll(PZQdata);
    }

    if (filters == null || (filters.contains(MBZ) || filters.isEmpty())) {
      List<Entry<String, ColumnData>> MBZEntries = columnMap.keySet().stream().map(
          s -> getFormData(plan, childLocation,
              MBZ, columnMap.get(s), name(s, MBZ))
      ).collect(Collectors.toList());
      Map<String, ColumnData> MBZData = MBZEntries.stream()
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      columns.putAll(MBZData);
    }

    RowData rowData = new RowData();
    rowData.setLocationIdentifier(childLocation.getIdentifier());
    rowData.setColumnDataMap(columns);
    rowData.setLocationName(childLocation.getName());

    return List.of(rowData);
  }

  public List<RowData> getMDALiteCoverageDataOnPlanTarget(Plan plan, Location childLocation,
      List<String> filters) {
    Map<String, ColumnData> columns = new HashMap<>();

    if (filters == null || (filters.contains(ALB) || filters.isEmpty())) {
      List<Entry<String, ColumnData>> ALBEntries = columnMap.keySet().stream().map(
          s -> getFormData(plan, childLocation,
              ALB, columnMap.get(s), name(s, ALB))

      ).collect(Collectors.toList());
      Map<String, ColumnData> ALBdata = ALBEntries.stream()
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      columns.putAll(ALBdata);
    }

    if (filters == null || (filters.contains(PZQ) || filters.isEmpty())) {
      List<Entry<String, ColumnData>> PZQEntries = columnMap.keySet().stream().map(
          s -> getFormData(plan, childLocation,
              PZQ, columnMap.get(s), name(s, PZQ))

      ).collect(Collectors.toList());
      Map<String, ColumnData> PZQdata = PZQEntries.stream()
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      columns.putAll(PZQdata);
    }

    if (filters == null || (filters.contains(MBZ) || filters.isEmpty())) {
      List<Entry<String, ColumnData>> MBZEntries = columnMap.keySet().stream().map(
          s -> getFormData(plan, childLocation,
              MBZ, columnMap.get(s), name(s, MBZ))
      ).collect(Collectors.toList());
      Map<String, ColumnData> MBZData = MBZEntries.stream()
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      columns.putAll(MBZData);
    }

    RowData rowData = new RowData();
    rowData.setLocationIdentifier(childLocation.getIdentifier());
    rowData.setColumnDataMap(columns);
    rowData.setLocationName(childLocation.getName());

    return List.of(rowData);
  }


  private Entry<String, ColumnData> getSupervisorFormData(Plan plan, Location childLocation,
      String drug, String searchKey, String columnName, Entry<String, String> supervisor) {


    Long treatedMaleOneToFour = 0L;


    ColumnData columnData = new ColumnData();
    columnData.setDataType("integer");
    columnData.setValue(treatedMaleOneToFour);
    columnData.setIsPercentage(false);

    return new SimpleEntry<>(columnName,
        columnData);
  }

  private Entry<String, ColumnData> getCddFormData(Plan plan, Location childLocation, String drug,
      String searchKey, String columnName, Entry<String, String> cdd) {


    Long treatedMaleOneToFour = 0L;


    ColumnData columnData = new ColumnData();
    columnData.setDataType("integer");
    columnData.setValue(treatedMaleOneToFour);
    columnData.setIsPercentage(false);

    return new SimpleEntry<>(columnName,
        columnData);
  }

  private Entry<String, ColumnData> getDaysWorked(Plan plan, Location childLocation, String drug,
      String searchKey, String columnName, Entry<String, String> cdd) {


    Long daysWorked = 0L;


    ColumnData columnData = new ColumnData();
    columnData.setDataType("integer");
    columnData.setValue(daysWorked);
    columnData.setIsPercentage(false);

    return new SimpleEntry<>(columnName,
        columnData);
  }

  private Entry<String, ColumnData> getDaysWorkedAverage(Plan plan, Location childLocation, String drug,
      String searchKey, String columnName, Entry<String, String> cdd) {



    Long treated = 0L;


    Long daysWorked = 0L;


    double average = 0L;
    if (daysWorked > 0){
      average = (double) treated / daysWorked ;
    }

    ColumnData columnData = new ColumnData();
    columnData.setDataType("integer");
    columnData.setValue(average);
    columnData.setIsPercentage(false);
    columnData.setMeta("Total Treated: "+treated+" / daysWorked: "+daysWorked);

    return new SimpleEntry<>(columnName,
        columnData);
  }


  private Entry<String, ColumnData> getFormData(Plan plan, Location childLocation, String drug,
      String searchKey, String columnName) {

    Long treatedMaleOneToFour = 0L;


    ColumnData columnData = new ColumnData();
    columnData.setDataType("integer");
    columnData.setValue(treatedMaleOneToFour);
    columnData.setIsPercentage(false);

    return new SimpleEntry<>(columnName,
        columnData);
  }

  private List<LocationResponse> setGeoJsonProperties(Map<UUID, RowData> rowDataMap,
      List<LocationResponse> locationResponses, String reportLevel) {
    return locationResponses.stream().peek(loc -> {

      loc.getProperties().setColumnDataMap(rowDataMap.get(loc.getIdentifier()).getColumnDataMap());
      loc.getProperties().setId(loc.getIdentifier().toString());
    }).collect(Collectors.toList());
  }


  private List<LocationResponse> setGeoJsonPropertiesWithSupervisor(Map<UUID, RowData> rowDataMap,
      List<LocationResponse> locationResponses, String reportLevel) {

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


  private List<LocationResponse> setGeoJsonPropertiesWithCDD(Map<UUID, RowData> rowDataMap,
      List<LocationResponse> locationResponses, String reportLevel) {

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

              response.getProperties().setName(rowDataWithSupervisorOrCdd.getCdd());
              response.getProperties().setId(rowDataWithSupervisorOrCdd.getCddKey());

              response.getProperties().setColumnDataMap(rowDataWithSupervisorOrCdd.getMaps());
              return response;
            })).collect(Collectors.toList());
  }

  private List<LocationResponse> setGeoJsonPropertiesOnPlanTarget(Map<UUID, RowData> rowDataMap,
      List<LocationResponse> locationResponses, String reportLevel) {

    return locationResponses.stream().peek(loc -> {
      loc.getProperties().setColumnDataMap(rowDataMap.get(loc.getIdentifier()).getColumnDataMap());
      loc.getProperties().setId("SUPERVISOR_" + "placeholder_" + loc.getIdentifier().toString());

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

    locationResponses = setGeoJsonProperties(rowDataMap, locationResponses, reportLevel);
    response.setDefaultDisplayColumn(
        name(dashboardProperties.getMdaLiteDefaultDisplayColumns().getOrDefault(reportLevel, null),
            "ALB"));
    response.setFeatures(locationResponses);
    response.setIdentifier(parentIdentifier);
    return response;
  }

  public FeatureSetResponse getFeatureSetResponseWithSupervisor(UUID parentIdentifier,
      List<PlanLocationDetails> locationDetails,
      Map<UUID, RowData> rowDataMap, String reportLevel) {
    FeatureSetResponse response = new FeatureSetResponse();
    response.setType("FeatureCollection");

    if (reportLevel.equals(SUPERVISOR_LEVEL)) {
      locationDetails = locationDetails.stream().map(locationDetail -> {
        PlanLocationDetails planLocationDetails = new PlanLocationDetails();
        planLocationDetails.setLocation(locationDetail.getLocation());
        planLocationDetails.setChildrenNumber((long) rowDataMap.size());
        planLocationDetails.setHasChildren(true);
        planLocationDetails.setAssignedLocations(0L);
        planLocationDetails.setAssignedTeams(0L);
        return planLocationDetails;
      }).collect(Collectors.toList());
    }

    List<LocationResponse> locationResponses = locationDetails.stream()
        .map(loc -> LocationResponseFactory.fromPlanLocationDetails(loc, parentIdentifier))
        .collect(Collectors.toList());

    locationResponses = setGeoJsonPropertiesWithSupervisor(rowDataMap, locationResponses,
        reportLevel);
    response.setDefaultDisplayColumn(
        name(dashboardProperties.getMdaLiteDefaultDisplayColumns().getOrDefault(reportLevel, null),
            "ALB"));
    response.setFeatures(locationResponses);
    response.setIdentifier(parentIdentifier);
    return response;
  }

  public FeatureSetResponse getFeatureSetResponseWithCDD(UUID parentIdentifier,
      List<PlanLocationDetails> locationDetails,
      Map<UUID, RowData> rowDataMap, String reportLevel) {
    FeatureSetResponse response = new FeatureSetResponse();
    response.setType("FeatureCollection");

    if (reportLevel.equals(CDD_LEVEL)) {
      locationDetails = locationDetails.stream().map(locationDetail -> {
        PlanLocationDetails planLocationDetails = new PlanLocationDetails();
        planLocationDetails.setLocation(locationDetail.getLocation());
        planLocationDetails.setChildrenNumber((long) rowDataMap.size());
        planLocationDetails.setHasChildren(true);
        planLocationDetails.setAssignedLocations(0L);
        planLocationDetails.setAssignedTeams(0L);
        return planLocationDetails;
      }).collect(Collectors.toList());
    }

    List<LocationResponse> locationResponses = locationDetails.stream()
        .map(loc -> LocationResponseFactory.fromPlanLocationDetails(loc, parentIdentifier))
        .collect(Collectors.toList());

    locationResponses = setGeoJsonPropertiesWithCDD(rowDataMap, locationResponses, reportLevel);
    response.setDefaultDisplayColumn(
        name(dashboardProperties.getMdaLiteDefaultDisplayColumns().getOrDefault(reportLevel, null),
            "ALB"));
    response.setFeatures(locationResponses);
    response.setIdentifier(parentIdentifier);
    return response;
  }

  public FeatureSetResponse getFeatureSetResponseOnPlanTarget(UUID parentIdentifier,
      List<PlanLocationDetails> locationDetails,
      Map<UUID, RowData> rowDataMap, String reportLevel) {
    FeatureSetResponse response = new FeatureSetResponse();
    response.setType("FeatureCollection");

    if (reportLevel.equals(IS_ON_PLAN_TARGET)) {
      locationDetails = locationDetails.stream().map(locationDetail -> {
        PlanLocationDetails planLocationDetails = new PlanLocationDetails();
        planLocationDetails.setLocation(locationDetail.getLocation());
        planLocationDetails.setChildrenNumber((long) rowDataMap.size());
        planLocationDetails.setHasChildren(true);
        planLocationDetails.setAssignedLocations(0L);
        planLocationDetails.setAssignedTeams(0L);
        return planLocationDetails;
      }).collect(Collectors.toList());
    }

    List<LocationResponse> locationResponses = locationDetails.stream()
        .map(loc -> LocationResponseFactory.fromPlanLocationDetails(loc, parentIdentifier))
        .collect(Collectors.toList());

    locationResponses = setGeoJsonPropertiesOnPlanTarget(rowDataMap, locationResponses,
        reportLevel);
    response.setDefaultDisplayColumn(
        name(dashboardProperties.getMdaLiteDefaultDisplayColumns().getOrDefault(reportLevel, null),
            "ALB"));
    response.setFeatures(locationResponses);
    response.setIdentifier(parentIdentifier);
    return response;
  }


  public void initDataStoresIfNecessary() {
    if (!datastoresInitialized) {
//      locationFormDataIntegerSumOrAverage = getKafkaStreams.getKafkaStreams().store(
//          StoreQueryParameters.fromNameAndType(
//              kafkaProperties.getStoreMap().get(KafkaConstants.locationFormDataIntegerSumOrAverage),
//              QueryableStoreTypes.keyValueStore()));
//
//      supervisorLocationFormDataIntegerSumOrAverage = getKafkaStreams.getKafkaStreams().store(
//          StoreQueryParameters.fromNameAndType(
//              kafkaProperties.getStoreMap()
//                  .get(KafkaConstants.supervisorLocationFormDataIntegerSumOrAverage),
//              QueryableStoreTypes.keyValueStore()));
//
//      cddSupervisorLocationFormDataIntegerSumOrAverage = getKafkaStreams.getKafkaStreams().store(
//          StoreQueryParameters.fromNameAndType(
//              kafkaProperties.getStoreMap()
//                  .get(KafkaConstants.cddSupervisorLocationFormDataIntegerSumOrAverage),
//              QueryableStoreTypes.keyValueStore()));
//
//      supervisors = getKafkaStreams.getKafkaStreams().store(
//          StoreQueryParameters.fromNameAndType(
//              kafkaProperties.getStoreMap().get(KafkaConstants.mdaLiteSupervisors),
//              QueryableStoreTypes.keyValueStore()));
//
//      cddNames = getKafkaStreams.getKafkaStreams().store(
//          StoreQueryParameters.fromNameAndType(
//              kafkaProperties.getStoreMap().get(KafkaConstants.cddNames),
//              QueryableStoreTypes.keyValueStore()));

      datastoresInitialized = true;
    }
  }

}
