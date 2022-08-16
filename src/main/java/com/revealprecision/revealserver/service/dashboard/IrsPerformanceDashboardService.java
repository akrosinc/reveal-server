package com.revealprecision.revealserver.service.dashboard;

import static com.revealprecision.revealserver.constants.FormConstants.IRS_FOUND;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_NOT_SPRAYED;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_SACHET_COUNT;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_SPRAYED;
import static com.revealprecision.revealserver.service.dashboard.PerformanceDashboardService.AVERAGE_END_TIME;
import static com.revealprecision.revealserver.service.dashboard.PerformanceDashboardService.AVERAGE_HOURS_WORKED;
import static com.revealprecision.revealserver.service.dashboard.PerformanceDashboardService.AVERAGE_START_TIME;
import static com.revealprecision.revealserver.service.dashboard.PerformanceDashboardService.DAYS_WORKED;
import static com.revealprecision.revealserver.service.dashboard.PerformanceDashboardService.END_TIME;
import static com.revealprecision.revealserver.service.dashboard.PerformanceDashboardService.HOURS_WORKED;
import static com.revealprecision.revealserver.service.dashboard.PerformanceDashboardService.START_TIME;

import com.revealprecision.revealserver.api.v1.dto.models.ColumnData;
import com.revealprecision.revealserver.api.v1.dto.models.RowData;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.enums.PlanInterventionTypeEnum;
import com.revealprecision.revealserver.messaging.message.UserLevel;
import com.revealprecision.revealserver.messaging.message.UserPerformanceAggregate;
import com.revealprecision.revealserver.messaging.message.UserPerformancePerDate;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.props.DashboardProperties;
import com.revealprecision.revealserver.props.KafkaProperties;
import java.time.LocalDate;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@Profile("Reveal-Streams")
public class IrsPerformanceDashboardService {

  private final DashboardProperties dashboardProperties;
  private final StreamsBuilderFactoryBean getKafkaStreams;
  private final KafkaProperties kafkaProperties;


  public static final String FOUND = "Found";
  public static final String SPRAYED = "Sprayed";
  public static final String NOT_SPRAYED = "Not Sprayed";
  public static final String BOTTLES_USED = "Bottles/Sachets Used";
  public static final String BOTTLES_USAGE_RATE = "Insecticide Usage Rate";

  boolean datastoresInitialized = false;

  private ReadOnlyKeyValueStore<String, UserPerformanceAggregate> userPerformanceSums;

  public List<RowData> getDetailedPerformanceColumnData(
      Plan plan, String id) {

    String key = plan.getIdentifier() + "_" + id;

    UserPerformanceAggregate userSumAggregate = userPerformanceSums.get(key);
    if (userSumAggregate != null) {

      return userSumAggregate.getDatedUserRecords().entrySet().stream()
          .map(userPerformancePerDate -> {
            RowData rowData = new RowData();
            rowData.setUserName(userPerformancePerDate.getKey().toString());
            rowData.setUserId(userSumAggregate.getUser().getUserId());
            rowData.setUserType("date");
            rowData.setUserLabel("date");
            rowData.setUserParent(id);
            rowData.setColumnDataMap(
                Objects.requireNonNull(getDetailedPerformanceColumnData(
                        PlanInterventionTypeEnum.valueOf(
                            plan.getInterventionType().getCode()), userPerformancePerDate)).stream()
                    .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue)));
            return rowData;
          }).collect(Collectors.toList());
    } else {
      return null;
    }
  }

  public List<SimpleEntry<String, ColumnData>> getDetailedPerformanceColumnData(
      PlanInterventionTypeEnum planInterventionTypeEnum,
      Entry<LocalDate, UserPerformancePerDate> userPerformancePerDate) {
    return dashboardProperties.getDetailedPerformanceLevelColumns().get(planInterventionTypeEnum)
        .stream()
        .map(column -> {
              switch (column) {

                case SPRAYED:
                  ColumnData sprayedColumnData = new ColumnData();
                  sprayedColumnData.setValue(
                      userPerformancePerDate.getValue().getFieldAggregate().get(IRS_SPRAYED) == null
                          ? 0 :
                          userPerformancePerDate.getValue().getFieldAggregate().get(IRS_SPRAYED)
                              .get(Boolean.TRUE.toString()) == null ? 0 :
                              userPerformancePerDate.getValue().getFieldAggregate().get(IRS_SPRAYED)
                                  .get(Boolean.TRUE.toString())
                                  .getCount());

                  return new SimpleEntry<>(SPRAYED, sprayedColumnData);

                case NOT_SPRAYED:
                  ColumnData notSprayedColumnData = new ColumnData();
                  notSprayedColumnData.setValue(
                      userPerformancePerDate.getValue().getFieldAggregate().get(IRS_NOT_SPRAYED)
                          == null
                          ? 0
                          : userPerformancePerDate.getValue().getFieldAggregate().get(
                                  IRS_NOT_SPRAYED)
                              .get(Boolean.TRUE.toString()) == null ? 0 :
                              userPerformancePerDate.getValue().getFieldAggregate()
                                  .get(IRS_NOT_SPRAYED)
                                  .get(Boolean.TRUE.toString())
                                  .getCount());
                  return new SimpleEntry<>(NOT_SPRAYED, notSprayedColumnData);

                case FOUND:
                  ColumnData foundColumnData = new ColumnData();
                  foundColumnData.setValue(
                      userPerformancePerDate.getValue().getFieldAggregate().get(IRS_FOUND) == null ? 0
                          :
                              userPerformancePerDate.getValue().getFieldAggregate().get(IRS_FOUND)
                                  .get(Boolean.TRUE.toString()) == null ? 0 :
                                  userPerformancePerDate.getValue().getFieldAggregate().get(
                                          IRS_FOUND)
                                      .get(Boolean.TRUE.toString()).getCount());
                  return new SimpleEntry<>(FOUND, foundColumnData);

                case HOURS_WORKED:
                  ColumnData hoursWorkedColumnData = new ColumnData();
                  hoursWorkedColumnData.setValue(userPerformancePerDate.getValue().getMinutesWorked());
                  return new SimpleEntry<>(HOURS_WORKED, hoursWorkedColumnData);

                case BOTTLES_USED:
                  ColumnData bottlesUsedColumnData = new ColumnData();
                  bottlesUsedColumnData.setValue(
                      userPerformancePerDate.getValue().getFieldAggregate().get(IRS_SACHET_COUNT)
                          == null
                          ? 0 :
                          userPerformancePerDate.getValue().getFieldAggregate().get(
                                  IRS_SACHET_COUNT)
                              .get("integer")
                              .getSum());
                  return new SimpleEntry<>(BOTTLES_USED, bottlesUsedColumnData);

                case START_TIME:
                  ColumnData startTimeColumnData = new ColumnData();
                  startTimeColumnData.setValue(userPerformancePerDate.getValue().getMinStartTime());
                  return new SimpleEntry<>(START_TIME, startTimeColumnData);

                case END_TIME:
                  ColumnData endTimeColumnData = new ColumnData();
                  endTimeColumnData.setValue(userPerformancePerDate.getValue().getMaxEndTime());
                  return new SimpleEntry<>(END_TIME, endTimeColumnData);
              }
              return null;
            }
        ).collect(Collectors.toList());
  }

  private ColumnData getAverageHoursWorkedColumnData(
      UserPerformanceAggregate userPerformanceAggregate) {

    ColumnData averageHoursWorkedColumnData = new ColumnData();
    averageHoursWorkedColumnData.setDataType("string");
    averageHoursWorkedColumnData.setValue(userPerformanceAggregate.getAverageHoursWorked());
    return averageHoursWorkedColumnData;
  }

  private ColumnData getAverageStartTimeColumnData(
      UserPerformanceAggregate userPerformanceAggregate) {

    ColumnData averageHoursWorkedColumnData = new ColumnData();
    averageHoursWorkedColumnData.setDataType("string");
    averageHoursWorkedColumnData.setValue(userPerformanceAggregate.getAverageStartTime());
    return averageHoursWorkedColumnData;
  }

  private ColumnData getAverageEndTimeColumnData(
      UserPerformanceAggregate userPerformanceAggregate) {

    ColumnData averageHoursWorkedColumnData = new ColumnData();
    averageHoursWorkedColumnData.setDataType("string");
    averageHoursWorkedColumnData.setValue(userPerformanceAggregate.getAverageEndTime());
    return averageHoursWorkedColumnData;
  }

  private ColumnData getDaysWorkedColumnData(UserPerformanceAggregate userPerformanceAggregate) {

    ColumnData averageHoursWorkedColumnData = new ColumnData();
    averageHoursWorkedColumnData.setDataType("string");
    averageHoursWorkedColumnData.setValue(userPerformanceAggregate.getDaysWorked());
    return averageHoursWorkedColumnData;
  }

  private ColumnData getFoundStructure(UserPerformanceAggregate userPerformanceAggregate) {

    ColumnData averageHoursWorkedColumnData = new ColumnData();

    averageHoursWorkedColumnData.setValue(
        userPerformanceAggregate.getCountOfFieldForValue(IRS_FOUND, Boolean.TRUE));
    return averageHoursWorkedColumnData;
  }

  private ColumnData getSprayedStructure(UserPerformanceAggregate userPerformanceAggregate) {

    ColumnData averageHoursWorkedColumnData = new ColumnData();

    averageHoursWorkedColumnData.setValue(
        userPerformanceAggregate.getCountOfFieldForValue(IRS_SPRAYED, Boolean.TRUE));
    return averageHoursWorkedColumnData;
  }

  private ColumnData getNotSprayedStructure(UserPerformanceAggregate userPerformanceAggregate) {

    ColumnData averageHoursWorkedColumnData = new ColumnData();

    averageHoursWorkedColumnData.setValue(
        userPerformanceAggregate.getCountOfFieldForValue(IRS_NOT_SPRAYED, Boolean.TRUE));
    return averageHoursWorkedColumnData;
  }


  private ColumnData getNumberOfBottlesOpened(UserPerformanceAggregate userPerformanceAggregate) {

    ColumnData averageHoursWorkedColumnData = new ColumnData();

    averageHoursWorkedColumnData.setValue(
        userPerformanceAggregate.getSumOfField(IRS_SACHET_COUNT, "integer"));
    return averageHoursWorkedColumnData;
  }

  private ColumnData getNumberOfBottlesUsageRate(
      UserPerformanceAggregate userPerformanceAggregate) {

    ColumnData bottlesUsageRateColumnData = new ColumnData();

    Long sachetCount = userPerformanceAggregate.getSumOfField(IRS_SACHET_COUNT, "integer");

    Long sprayed = userPerformanceAggregate.getCountOfFieldForValue(IRS_SPRAYED, Boolean.TRUE);

    double usageRate = 0;
    if (sprayed > 0) {
      usageRate = (double) sachetCount / (double) sprayed;
    }

    bottlesUsageRateColumnData.setValue(usageRate);
    bottlesUsageRateColumnData.setMeta(
        "Sachet/Bottle Count: " + sachetCount + " / " + "Sprayed Structures: " + sprayed);
    bottlesUsageRateColumnData.setIsPercentage(true);
    return bottlesUsageRateColumnData;
  }

  public RowData getRowData(Plan plan, UserLevel userLevel, String parentUserLevelId) {

    RowData rowData = new RowData();

    Map<String, ColumnData> columnData = new HashMap<>();

    String key = plan.getIdentifier() + "_" + userLevel.getUserId();

    UserPerformanceAggregate userSumAggregate = userPerformanceSums.get(key);

    ColumnData averageHoursWorkedColumnData = getAverageHoursWorkedColumnData(
        userSumAggregate);
    columnData.put(AVERAGE_HOURS_WORKED, averageHoursWorkedColumnData);

    ColumnData averageStartTimeColumnData = getAverageStartTimeColumnData(
        userSumAggregate);
    columnData.put(AVERAGE_START_TIME, averageStartTimeColumnData);

    ColumnData averageEndTimeColumnData = getAverageEndTimeColumnData(
        userSumAggregate);
    columnData.put(AVERAGE_END_TIME, averageEndTimeColumnData);

    ColumnData daysWorkedColumnData = getDaysWorkedColumnData(
        userSumAggregate);
    columnData.put(DAYS_WORKED, daysWorkedColumnData);

    ColumnData foundStructure = getFoundStructure(
        userSumAggregate);
    columnData.put(FOUND, foundStructure);

    ColumnData sprayedStructure = getSprayedStructure(
        userSumAggregate);
    columnData.put(SPRAYED, sprayedStructure);

    ColumnData notSprayed = getNotSprayedStructure(
        userSumAggregate);
    columnData.put(NOT_SPRAYED, notSprayed);

    ColumnData bottlesOpened = getNumberOfBottlesOpened(
        userSumAggregate);
    columnData.put(BOTTLES_USED, bottlesOpened);

    ColumnData bottlesUsageRate = getNumberOfBottlesUsageRate(
        userSumAggregate);
    columnData.put(BOTTLES_USAGE_RATE, bottlesUsageRate);

    rowData.setColumnDataMap(columnData);
    rowData.setUserName(userLevel.getName());
    rowData.setUserId(userLevel.getUserId());
    rowData.setUserLabel(userLevel.getLabel());
    rowData.setUserType(userLevel.getType());
    rowData.setUserParent(parentUserLevelId);
    return rowData;
  }

  public void initialDataStores() {
    if (!datastoresInitialized) {

      this.userPerformanceSums = getKafkaStreams.getKafkaStreams().store(
          StoreQueryParameters.fromNameAndType(kafkaProperties.getStoreMap().get(
                  KafkaConstants.userPerformanceSums),
              QueryableStoreTypes.keyValueStore()));

      datastoresInitialized = true;
    }
  }

}
