package com.revealprecision.revealserver.messaging.streams;

import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.mdalite.MDALiteLocationSupervisorCddEvent;
import com.revealprecision.revealserver.messaging.message.mdalite.MDALiteLocationSupervisorListAggregation;
import com.revealprecision.revealserver.messaging.message.mdalite.MDALiteSupervisorCddListAggregation;
import com.revealprecision.revealserver.props.KafkaProperties;
import java.util.UUID;
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
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.support.serializer.JsonSerde;

@Configuration
@Slf4j
@RequiredArgsConstructor
@Profile("Reveal-Streams")
public class MDALiteLocationSupervisorCddStream {

  private final KafkaProperties kafkaProperties;
  private final Logger formDataLog = LoggerFactory.getLogger("form-data-file");

  @Bean
  KStream<UUID, MDALiteLocationSupervisorCddEvent> supervisorCddEventKStreamProcessor(
      StreamsBuilder streamsBuilder) {

    KStream<UUID, MDALiteLocationSupervisorCddEvent> mdaLiteLocationSupervisorCddStream = streamsBuilder.stream(
        kafkaProperties.getTopicMap().get(KafkaConstants.LOCATION_SUPERVISOR_CDD),
        Consumed.with(Serdes.UUID(), new JsonSerde<>(MDALiteLocationSupervisorCddEvent.class)));

    mdaLiteLocationSupervisorCddStream.peek(
        (k, v) -> formDataLog.debug("mdaLiteLocationSupervisorCddStream k:{} ,v:{}", k, v));

    KGroupedStream<String, MDALiteLocationSupervisorCddEvent> groupedMdaLiteLocationSupervisorStream = mdaLiteLocationSupervisorCddStream.groupBy(
        (k, v) -> v.getPlanIdentifier() + "_" + v.getLocationHierarchyIdentifier() + "_"
            + v.getLocationIdentifier());

    KTable<String, MDALiteLocationSupervisorListAggregation> supervisorListKTable = groupedMdaLiteLocationSupervisorStream.aggregate(
        MDALiteLocationSupervisorListAggregation::new, (k, v, agg) -> {
          agg.getSupervisorNames().put(v.getSupervisorName(), UUID.randomUUID().toString());
          return agg;
        },
        Materialized.<String, MDALiteLocationSupervisorListAggregation, KeyValueStore<Bytes, byte[]>>as(
                kafkaProperties.getStoreMap().get(KafkaConstants.mdaLiteSupervisors))
            .withKeySerde(Serdes.String())
            .withValueSerde(new JsonSerde<>(MDALiteLocationSupervisorListAggregation.class)));

    supervisorListKTable.toStream()
        .peek((k, v) -> formDataLog.debug("supervisorListKTable k:{} ,v:{}", k, v));

    KGroupedStream<String, MDALiteLocationSupervisorCddEvent> stringMDALiteSupervisorCddEventGroupedStream = mdaLiteLocationSupervisorCddStream.groupBy(
        (k, v) -> v.getPlanIdentifier() + "_" + v.getLocationHierarchyIdentifier() + "_"
            + v.getLocationIdentifier() + "_" + v.getSupervisorName());

    KTable<String, MDALiteSupervisorCddListAggregation> cddListKTable = stringMDALiteSupervisorCddEventGroupedStream.aggregate(
        MDALiteSupervisorCddListAggregation::new, (k, v, agg) -> {
          agg.getCddNames().add(v.getCddName());
          return agg;
        },
        Materialized.<String, MDALiteSupervisorCddListAggregation, KeyValueStore<Bytes, byte[]>>as(
                kafkaProperties.getStoreMap().get(KafkaConstants.cddNames))
            .withKeySerde(Serdes.String())
            .withValueSerde(new JsonSerde<>(MDALiteSupervisorCddListAggregation.class)));

    cddListKTable.toStream().peek((k, v) -> formDataLog.debug("cddListKTable k:{} ,v:{}", k, v));

    return mdaLiteLocationSupervisorCddStream;
  }


}
