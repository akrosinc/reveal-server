package com.revealprecision.revealserver.api.v1.controller.kafkareset;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/state-store")
@Slf4j
@RequiredArgsConstructor
@Profile("Reveal-Streams")
public class KafkaStateStoreResetController {

  private final StreamsBuilderFactoryBean getKafkaStreams;

  @GetMapping("/reset")
  public String resetStateStores() {
    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    kafkaStreams.close();
    kafkaStreams.cleanUp();

    return "done";
  }
}
