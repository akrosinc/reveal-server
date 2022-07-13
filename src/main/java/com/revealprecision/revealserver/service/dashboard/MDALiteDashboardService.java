package com.revealprecision.revealserver.service.dashboard;


import com.revealprecision.revealserver.api.v1.dto.factory.LocationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.models.ColumnData;
import com.revealprecision.revealserver.api.v1.dto.models.RowData;
import com.revealprecision.revealserver.api.v1.dto.response.FeatureSetResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.messaging.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.LocationFormDataSumAggregateEvent;
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

  public static final String treatedMale1_4 = "treated-male-1-to-4";
  public static final String treatedMale5_14 = "treated-male-5-to-14";
  public static final String treatedMale15 = "treated-male-above-15";
  public static final String totalMales = "total-males";
  public static final String treatedFemale1_4 = "treated-female-1-to-4";
  public static final String treatedFemale5_14 = "treated-female-5-to-14";
  public static final String treatedFemale15 = "treated-female-above-15";
  public static final String totalFemale = "total-female";
  public static final String totalPeople = "total-people";


  private Map<String, String> columnMap = Map.of(
      MALES_1_4,treatedMale1_4,
      MALES_5_14,treatedMale5_14,
      MALES_15,treatedMale15,
      MALES_TOTAL,totalMales,
      FEMALES_1_4,treatedFemale1_4,
      FEMALES_5_14,treatedFemale5_14,
      FEMALES_15,treatedFemale15,
      FEMALES_TOTAL,totalFemale,
      TOTAL_TREATED,totalPeople
  );

  ReadOnlyKeyValueStore<String, LocationFormDataSumAggregateEvent> locationFormDataIntegerSumOrAverage;
  boolean datastoresInitialized = false;

  private String name(String constant,String drug){

    return constant.concat(" (").concat(drug).concat(")");
  }

  public List<RowData> getMDALiteCoverageData(Plan plan, Location childLocation) {
    Map<String, ColumnData> columns = new HashMap<>();

    List<Entry<String, ColumnData>> ALBEntries = columnMap.keySet().stream().map(
        s -> getFormData(plan, childLocation,
            "ALB", columnMap.get(s), name(s, "ALB"))

    ).collect(Collectors.toList());
    Map<String, ColumnData> ALBdata = ALBEntries.stream()
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    columns.putAll(ALBdata);

    List<Entry<String, ColumnData>> PZQEntries = columnMap.keySet().stream().map(
        s -> getFormData(plan, childLocation,
            "PZQ", columnMap.get(s), name(s, "PZQ"))

    ).collect(Collectors.toList());
    Map<String, ColumnData> PZQdata = PZQEntries.stream()
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    columns.putAll(PZQdata);

    List<Entry<String, ColumnData>> MBZEntries = columnMap.keySet().stream().map(
        s -> getFormData(plan, childLocation,
            "MBZ", columnMap.get(s), name(s, "MBZ"))

    ).collect(Collectors.toList());
    Map<String, ColumnData> MBZData = MBZEntries.stream()
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    columns.putAll(MBZData);

    RowData rowData = new RowData();
    rowData.setLocationIdentifier(childLocation.getIdentifier());
    rowData.setColumnDataMap(columns);
    rowData.setLocationName(childLocation.getName());

    return List.of(rowData);
  }


  private Entry<String, ColumnData>  getFormData(Plan plan, Location childLocation, String drug, String searchKey,String columnName) {
    String key = plan.getIdentifier() + "_" + plan.getLocationHierarchy().getIdentifier() + "_" + childLocation.getIdentifier()
        + "_" + searchKey+ "-" + drug;

    LocationFormDataSumAggregateEvent locationFormDataSumAggregateEvent = locationFormDataIntegerSumOrAverage.get(
        key);

    Long treatedMaleOneToFour = 0L;
    if (locationFormDataSumAggregateEvent != null) {
      Long sum = locationFormDataSumAggregateEvent.getSum();
      if (sum !=null){
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

  private List<LocationResponse> setGeoJsonProperties(Map<UUID, RowData> rowDataMap,
      List<LocationResponse> locationResponses) {
    return locationResponses.stream().peek(loc -> {
      loc.getProperties().setColumnDataMap(rowDataMap.get(loc.getIdentifier()).getColumnDataMap());
      loc.getProperties().setId(loc.getIdentifier());
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
    response.setDefaultDisplayColumn(name(dashboardProperties.getMdaLiteDefaultDisplayColumns().getOrDefault(reportLevel, null),"ALB"));
    response.setFeatures(locationResponses);
    response.setIdentifier(parentIdentifier);
    return response;
  }

  public void initDataStoresIfNecessary() {
    if (!datastoresInitialized) {
      locationFormDataIntegerSumOrAverage = getKafkaStreams.getKafkaStreams().store(
          StoreQueryParameters.fromNameAndType(
              kafkaProperties.getStoreMap().get(KafkaConstants.locationFormDataIntegerSumOrAverage),
              QueryableStoreTypes.keyValueStore()));

      datastoresInitialized = true;
    }
  }

}
