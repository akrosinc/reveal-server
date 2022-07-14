package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.messaging.message.MetaDataEvent;
import com.revealprecision.revealserver.messaging.message.PersonMetadataEvent;
import com.revealprecision.revealserver.persistence.es.EntityMetadataElastic;
import com.revealprecision.revealserver.util.ElasticModelUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
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
public class PersonMetadataUpdateListener extends Listener{

  private final RestHighLevelClient client;

  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('PERSON_METADATA_UPDATE')}", groupId = "reveal_server_group")
  public void updatePersonMetadata(PersonMetadataEvent event) throws IOException {
    log.info("Received Message in group foo: {}" , event.toString());
    init();
    Map<String, Object> parameters = new HashMap<>();
    List<Map<String, Object>> metadata = new ArrayList<>();
    for(MetaDataEvent metadataObj : event.getMetaDataEvents()) {
      metadata.add(ElasticModelUtil.toMapFromPersonMetadata(new EntityMetadataElastic(metadataObj)));
    }
    parameters.put("new_metadata", metadata);
    parameters.put("personId", event.getEntityId().toString());

    Script inline = new Script(ScriptType.INLINE, "painless",
        "def persons = ctx._source.person.findAll( "
            + "pers -> pers.identifier == params.personId); "
            + "for(per in persons) { "
            + " per.metadata = params.new_metadata "
            + "} ",parameters);
    List<String> locationIds = event.getLocationIdList().stream().map(UUID::toString).collect(
        Collectors.toList());
    UpdateByQueryRequest request = new UpdateByQueryRequest("location");
    request.setQuery(QueryBuilders.termsQuery("_id", locationIds));
    request.setScript(inline);
    client.updateByQuery(request, RequestOptions.DEFAULT);
  }
}
