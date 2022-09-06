package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.messaging.message.LocationMetadataEvent;
import com.revealprecision.revealserver.messaging.message.Message;
import com.revealprecision.revealserver.messaging.message.MetaDataEvent;
import com.revealprecision.revealserver.persistence.es.EntityMetadataElastic;
import com.revealprecision.revealserver.util.ElasticModelUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Profile("Simulation")
@Slf4j
public class LocationMetadataUpdateListener extends Listener{

  private final RestHighLevelClient client;


  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('LOCATION_METADATA_UPDATE')}", groupId = "reveal_server_group")
  public void updateLocationMetadata(Message message) throws IOException {
    log.info("Received Message in group foo: {}" , message.toString());
    init();

    LocationMetadataEvent locationMetadataEvent = (LocationMetadataEvent) message;
    Map<String, Object> parameters = new HashMap<>();
    List<Map<String, Object>> metadata = new ArrayList<>();
    for(MetaDataEvent metadataObj : locationMetadataEvent.getMetaDataEvents()) {
      metadata.add(ElasticModelUtil.toMapFromPersonMetadata(new EntityMetadataElastic(metadataObj)));
    }
    parameters.put("new_metadata", metadata);
    Script inline = new Script(ScriptType.INLINE, "painless",
        "ctx._source.metadata = params.new_metadata;",parameters);
    UpdateByQueryRequest request = new UpdateByQueryRequest("location");
    request.setQuery(QueryBuilders.termQuery("_id", locationMetadataEvent.getEntityId().toString()));
    request.setConflicts("proceed");
    request.setScript(inline);
    log.debug("requesting elastic update location id {} with request {}",locationMetadataEvent.getEntityId(),request);
    BulkByScrollResponse bulkByScrollResponse = client.updateByQuery(request,
        RequestOptions.DEFAULT);
    if(bulkByScrollResponse.getVersionConflicts() > 0) {
      log.error("elastic update failed");
      throw new ElasticsearchException("Version conflict exception");
    }
    log.debug("Updated location id {} with response {}",locationMetadataEvent.getEntityId(),bulkByScrollResponse.toString());
  }
}
