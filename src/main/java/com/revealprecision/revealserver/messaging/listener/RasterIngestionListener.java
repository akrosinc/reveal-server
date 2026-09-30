package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.messaging.message.RasterIngestionMessage;
import com.revealprecision.revealserver.service.RasterIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RasterIngestionListener {

  private final RasterIngestionService rasterIngestionService;

  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('RASTER_INGESTION')}",
      groupId = "reveal_server_group")
  public void rasterIngestion(RasterIngestionMessage message) {
    log.info("Received ingestion request for taskIdentifier: {}", message.getRasterId());
    rasterIngestionService.processIngestion(message);
    log.info("Successfully processed ingestion for taskIdentifier: {}", message.getRasterId());
  }
}

