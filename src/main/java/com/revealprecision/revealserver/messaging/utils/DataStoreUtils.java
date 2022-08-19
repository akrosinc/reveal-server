package com.revealprecision.revealserver.messaging.utils;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.errors.InvalidStateStoreException;

public class DataStoreUtils {

  public static <T> T getQueryableStoreByWaiting(final KafkaStreams streams,
      final StoreQueryParameters<T> storeQueryParameters) throws InterruptedException {
    while (true) {
      try {
        return streams.store(storeQueryParameters);
      } catch (InvalidStateStoreException ignored) {
        Thread.sleep(100);
      }
    }
  }
}
