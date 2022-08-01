package com.revealprecision.revealserver.messaging.message;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class UserPerformanceAggregate extends Message {

  UUID planIdentifier;
  UserLevel user;
  Map<LocalDate, UserPerformancePerDate> datedUserRecords;

  public String getAverageStartTime() {
    return LocalTime.MIN.plus(Duration.ofMinutes(Double.valueOf(
        ((double) (this.datedUserRecords.values().stream()
            .map(UserPerformancePerDate::getStartTimeLong).reduce(0L, Long::sum)))
            / ((double) this.datedUserRecords.size())).longValue())).toString();

  }

  public String getAverageEndTime() {
    return LocalTime.MIN.plus(Duration.ofMinutes(Double.valueOf(
            ((double) (this.datedUserRecords.values().stream().map(UserPerformancePerDate::getEndTimeLong)
                .reduce(0L, Long::sum))) / ((double) this.datedUserRecords.size())).longValue()))
        .toString();
  }

  public int getDaysWorked() {
    return this.datedUserRecords.size();
  }

  public String getAverageHoursWorked() {
    return LocalTime.MIN.plus(Duration.ofMinutes(Double.valueOf(
        ((double) (this.datedUserRecords.values().stream()
            .map(UserPerformancePerDate::getMinutesWorked).reduce(0L, Long::sum)))
            / ((double) this.datedUserRecords.size())).longValue())).toString();

  }

  public Long getCountOfFieldForValue(String fieldName, Object value) {
    return datedUserRecords.values().stream().filter(
            datedUserRecord -> datedUserRecord.getFieldAggregate().containsKey(fieldName)
                && datedUserRecord.getFieldAggregate().get(fieldName).containsKey(value.toString()))
        .map(datedUserRecord -> datedUserRecord.getFieldAggregate().get(fieldName))
        .map(fieldAggregateContainer -> fieldAggregateContainer.get(value.toString()))
        .collect(Collectors.summingLong(obj -> obj.getCount()));
  }

  public Long getSumOfField(String fieldName, String value) {
    return datedUserRecords.values().stream().filter(
            datedUserRecord -> datedUserRecord.getFieldAggregate().containsKey(fieldName)
                && datedUserRecord.getFieldAggregate().get(fieldName).containsKey(value.toString()))
        .map(datedUserRecord -> datedUserRecord.getFieldAggregate().get(fieldName))
        .map(fieldAggregateContainer -> fieldAggregateContainer.get(value.toString()))
        .collect(Collectors.summingLong(obj -> obj.getSum()));
  }

}
