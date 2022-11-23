package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.messaging.message.LocationRelationshipMessage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Profile("Simulation & (Listening | location-import-listener))")
public class LocationImportListener extends Listener{

  private final RestHighLevelClient client;

  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('LOCATIONS_IMPORTED')}", groupId = "reveal_server_group")
  public void locationImport(LocationRelationshipMessage message) throws IOException {
    Map<String, Object> parameters = new HashMap<>();
    Map<String, List<String>> object = new HashMap<>();
    object.put(message.getLocationHierarchyIdentifier().toString(),
        message.getAncestry().stream()
            .map(UUID::toString)
            .collect(Collectors.toList()));
    parameters.put("object", object);
    parameters.put("key", message.getLocationHierarchyIdentifier().toString());
    Script inline = new Script(ScriptType.INLINE, "painless",
        "ctx._source.ancestry.add(params.object);",parameters);
    UpdateRequest request = new UpdateRequest(
        "location",
        message.getLocationIdentifier().toString());
    request.script(inline);
    client.update(request, RequestOptions.DEFAULT);
  }
}
