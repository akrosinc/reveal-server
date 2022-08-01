package com.revealprecision.revealserver.messaging.streams;

import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.FieldAggregate;
import com.revealprecision.revealserver.messaging.message.UserAggregate;
import com.revealprecision.revealserver.messaging.message.UserDataParentChild;
import com.revealprecision.revealserver.messaging.message.UserLevel;
import com.revealprecision.revealserver.messaging.message.UserParentChildren;
import com.revealprecision.revealserver.messaging.message.UserPerOrgLevel;
import com.revealprecision.revealserver.messaging.message.UserPerformanceAggregate;
import com.revealprecision.revealserver.messaging.message.UserPerformanceData;
import com.revealprecision.revealserver.messaging.message.UserPerformancePerDate;
import com.revealprecision.revealserver.props.KafkaProperties;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KGroupedStream;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class UserPerformanceStream {

  private final KafkaProperties kafkaProperties;
  private final Logger userPerformanceFile = LoggerFactory.getLogger("user-performance-file");

  @Bean
  KStream<String, UserPerformanceData> performanceDataKStream(StreamsBuilder streamsBuilder) {

    KStream<String, UserPerformanceData> performanceDataKStream = streamsBuilder.stream(
        kafkaProperties.getTopicMap().get(KafkaConstants.USER_PERFORMANCE_DATA),
        Consumed.with(Serdes.String(), new JsonSerde<>(UserPerformanceData.class)));

    performanceDataKStream.peek(
        (k, v) -> userPerformanceFile.debug("performanceDataKStream k: {},v: {}", k, v));

    KStream<String, UserPerOrgLevel> stringUserPerOrgLevelKStream = performanceDataKStream.flatMapValues(
        (k, v) -> v.getOrgHierarchy().stream().map(org ->
            new UserPerOrgLevel(v.getPlanIdentifier(), v.getCaptureTime(), org, v.getFields())
        ).collect(Collectors.toList()));

    KGroupedStream<String, UserPerOrgLevel> stringUserAggregateKGroupedStream = stringUserPerOrgLevelKStream.groupBy(
        (k, v) -> v.getPlanIdentifier() + "_" + v.getUserLevel().getUserId());

    KTable<String, UserPerformanceAggregate> userSumAggregateKTable = stringUserAggregateKGroupedStream.aggregate(
        () -> null,
        (k, v, agg) -> {
          if (agg == null) {
            UserPerformancePerDate userPerformancePerDate = getUserPerformancePerDate(
                v);
            agg = new UserPerformanceAggregate(v.getPlanIdentifier(), v.getUserLevel(),
                Map.of(v.getCaptureDateTime().toLocalDate(), userPerformancePerDate));
          } else {
            if (agg.getDatedUserRecords().containsKey(v.getCaptureDateTime().toLocalDate())) {
              UserPerformancePerDate userPerformancePerDate = agg.getDatedUserRecords()
                  .get(v.getCaptureDateTime().toLocalDate());
              LocalTime minTime;
              if (getTimeOfDayFromLocalDateTime(v.getCaptureDateTime()).isBefore(
                  userPerformancePerDate.getMinStartTime())) {
                minTime = getTimeOfDayFromLocalDateTime(v.getCaptureDateTime());
              } else {
                minTime = userPerformancePerDate.getMinStartTime();
              }

              LocalTime maxTime;
              if (getTimeOfDayFromLocalDateTime(v.getCaptureDateTime()).isAfter(
                  userPerformancePerDate.getMaxEndTime())) {
                maxTime = getTimeOfDayFromLocalDateTime(v.getCaptureDateTime());
              } else {
                maxTime = userPerformancePerDate.getMaxEndTime();
              }

              //existing aggregate map
              Map<String, Map<String, FieldAggregate>> existingFieldAggregateMap = userPerformancePerDate.getFieldAggregate();
              Map<String, Map<String, FieldAggregate>> updatedAggregationMap = new HashMap<>(
                  existingFieldAggregateMap);
              //create new map with fields from input
              Map<String, Map<String, FieldAggregate>> newFieldAggregationMap = v.getFields()
                  .entrySet().stream().map(incomingFormValueEntry -> {
                    Long sum = 0L;
                    String valueKey = incomingFormValueEntry.getValue().toString();
                    if (incomingFormValueEntry.getValue() instanceof Integer) {
                      sum = ((Integer) incomingFormValueEntry.getValue()).longValue();
                      valueKey = "integer";
                    }

                    Map<String, FieldAggregate> fieldAggregate;
                    if (existingFieldAggregateMap.containsKey(incomingFormValueEntry.getKey())) {

                      Map<String, FieldAggregate> existingAggregationMapForThisKey = existingFieldAggregateMap.get(
                          incomingFormValueEntry.getKey());

                      if (existingAggregationMapForThisKey.containsKey(valueKey)) {
                        FieldAggregate fieldAggregate1 = existingAggregationMapForThisKey.get(
                            valueKey);

                        fieldAggregate1 = new FieldAggregate(fieldAggregate1.getCount() + 1,
                            fieldAggregate1.getSum() + sum);

                        existingAggregationMapForThisKey.put(valueKey, fieldAggregate1);
                      } else {
                        existingAggregationMapForThisKey.put(valueKey, new FieldAggregate(1L,
                            sum));
                      }
                      fieldAggregate = existingAggregationMapForThisKey;
                    } else {
                      fieldAggregate = Map.of(valueKey, new FieldAggregate(1L,
                          sum));
                    }
                    return new SimpleEntry<>(incomingFormValueEntry.getKey(), fieldAggregate);
                  }).collect(Collectors.toMap(Entry::getKey, Entry::getValue));

              updatedAggregationMap.putAll(newFieldAggregationMap);

              userPerformancePerDate = new UserPerformancePerDate(
                  getMinutesFromMidnightFromLocalTime(maxTime)
                      - getMinutesFromMidnightFromLocalTime(minTime), minTime, maxTime,
                  getMinutesFromMidnightFromLocalTime(minTime),
                  getMinutesFromMidnightFromLocalTime(maxTime), v.getFields(), updatedAggregationMap);

              agg.getDatedUserRecords().put(v.getCaptureDateTime().toLocalDate(), userPerformancePerDate);

            } else {
              UserPerformancePerDate userPerformancePerDate = getUserPerformancePerDate(
                  v);

              agg.getDatedUserRecords().put(v.getCaptureDateTime().toLocalDate(), userPerformancePerDate);
            }
          }
          return agg;
        }
        , Materialized.<String, UserPerformanceAggregate, KeyValueStore<Bytes, byte[]>>as(
                kafkaProperties.getStoreMap().get(KafkaConstants.userPerformanceSums))
            .withKeySerde(Serdes.String())
            .withValueSerde(new JsonSerde<>(UserPerformanceAggregate.class)));

    userSumAggregateKTable.toStream()
        .peek((k, v) -> userPerformanceFile.debug("userSumAggregateKTable k: {} v: {}", k, v));

    return performanceDataKStream;
  }

  private UserPerformancePerDate getUserPerformancePerDate(UserPerOrgLevel userPerOrgLevel) {

    Map<String, Map<String, FieldAggregate>> fieldAggregate = userPerOrgLevel.getFields().entrySet()
        .stream()
        .map(entry -> {
          Long sum = 0L;
          String key = entry.getValue().toString();
          if (entry.getValue() instanceof Integer) {
            sum = ((Integer) entry.getValue()).longValue();
            key = "integer";
          }
          return new SimpleEntry<>(entry.getKey(), Map.of(key, new FieldAggregate(1L, sum)));
        }).collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    return new UserPerformancePerDate(0L,
        getTimeOfDayFromLocalDateTime(userPerOrgLevel.getCaptureDateTime()),
        getTimeOfDayFromLocalDateTime(userPerOrgLevel.getCaptureDateTime()),
        getMinutesFromMidnightFromLocalDateTime(userPerOrgLevel.getCaptureDateTime()),
        getMinutesFromMidnightFromLocalDateTime(userPerOrgLevel.getCaptureDateTime()),
        userPerOrgLevel.getFields(), fieldAggregate);
  }

  private LocalTime getTimeOfDayFromLocalDateTime(LocalDateTime dateTime) {
    return LocalTime.MIDNIGHT.plusMinutes(getMinutesFromMidnightFromLocalDateTime(dateTime));
  }


  private long getMinutesFromMidnightFromLocalDateTime(LocalDateTime dateTime) {
    return dateTime.toLocalDate().atStartOfDay()
        .until(dateTime, ChronoUnit.MINUTES);
  }

  private long getMinutesFromMidnightFromLocalTime(LocalTime localTime) {
    return LocalTime.MIDNIGHT
        .until(localTime, ChronoUnit.MINUTES);
  }


  public static void main(String[] args) {
    Long mins = 1096L;

    LocalTime localTime = LocalTime.MIDNIGHT.plusMinutes(mins);
    System.out.println(localTime);
  }

  private UserAggregate getUserAggregate(UserPerOrgLevel v, UserAggregate agg) {
    if (agg == null) {
      agg = new UserAggregate(
          v.getPlanIdentifier(), v.getUserLevel(), v.getCaptureDateTime(), v.getCaptureDateTime(),
          1L, 0L,
          0L, 0L,
          v.getCaptureDateTime().toLocalDate().atStartOfDay()
              .until(v.getCaptureDateTime(), ChronoUnit.MINUTES)
          , v.getCaptureDateTime().toLocalDate().atStartOfDay()
          .until(v.getCaptureDateTime(), ChronoUnit.MINUTES));
    } else {
      UserAggregate oldAgg = agg;
      if (v.getCaptureDateTime().isBefore(agg.getMinDateTime())) {
        agg = new UserAggregate(agg.getPlanIdentifier(), agg.getUserLevel(), v.getCaptureDateTime(),
            agg.getMaxDateTime(),
            agg.getDaysWorked(), agg.getDaysWorkedRepeat(),
            agg.getMinutesWorked(), null, null, null);
      }

      if (v.getCaptureDateTime().isAfter(agg.getMaxDateTime())) {
        agg = new UserAggregate(agg.getPlanIdentifier(), v.getUserLevel(), agg.getMinDateTime(),
            v.getCaptureDateTime(),
            agg.getDaysWorked(), agg.getDaysWorkedRepeat(),
            agg.getMinutesWorked(), null, null, null);
      }

      //minutes worked
      if (oldAgg.getMinutesWorked() > 0) {
        // after second event

        agg = new UserAggregate(
            agg.getPlanIdentifier(), agg.getUserLevel(), agg.getMinDateTime(), agg.getMaxDateTime(),
            agg.getDaysWorked(), agg.getDaysWorkedRepeat(),
            agg.getMinutesWorked(), oldAgg.getMinutesWorked() * -1, null, null);
      } else {
        // second event
        agg = new UserAggregate(
            agg.getPlanIdentifier(), agg.getUserLevel(), agg.getMinDateTime(), agg.getMaxDateTime(),
            agg.getDaysWorked(), agg.getDaysWorkedRepeat(),
            agg.getMinDateTime().until(agg.getMaxDateTime(), ChronoUnit.MINUTES), 0L, null, null);
      }

      // start time long and end  time long
      agg = new UserAggregate(
          agg.getPlanIdentifier(), v.getUserLevel(), agg.getMinDateTime(), agg.getMaxDateTime(),
          1L, -1L,
          agg.getMinutesWorked(), agg.getMinutesWorkedRepeat(),
          agg.getMinDateTime().toLocalDate().atStartOfDay()
              .until(agg.getMinDateTime(), ChronoUnit.MINUTES),
          agg.getMaxDateTime().toLocalDate().atStartOfDay()
              .until(agg.getMaxDateTime(), ChronoUnit.MINUTES));

    }
    return agg;
  }


  @Bean
  KStream<String, UserDataParentChild> parentChildrenDataKStream(StreamsBuilder streamsBuilder) {
    KStream<String, UserDataParentChild> userParentChildStream = streamsBuilder.stream(
        kafkaProperties.getTopicMap().get(KafkaConstants.USER_PARENT_CHILD),
        Consumed.with(Serdes.String(), new JsonSerde<>(UserDataParentChild.class)));

    KGroupedStream<String, UserDataParentChild> stringUserDataParentChildKGroupedStream = userParentChildStream.groupBy(
        (k, v) -> v.getPlanIdentifier() + "_" + v.getParent().getUserId());

    stringUserDataParentChildKGroupedStream.aggregate(() -> null,
        (k, v, agg) -> {
          if (agg == null) {
            agg = new UserParentChildren(v.getPlanIdentifier(), v.getParent(), Set.of(v.getChild()));
          } else {
            UserParentChildren oldAgg = agg;
            Set<UserLevel> userLevels = agg.getChildren();
            userLevels.add(v.getChild());
            agg = new UserParentChildren(oldAgg.getPlanIdentifier(), oldAgg.getParent(), userLevels);
          }
          return agg;
        }, Materialized.<String, UserParentChildren, KeyValueStore<Bytes, byte[]>>as(
                kafkaProperties.getStoreMap().get(KafkaConstants.userParentChildren))
            .withValueSerde(new JsonSerde<>(UserParentChildren.class))
            .withKeySerde(Serdes.String()));

    return userParentChildStream;

  }

}
