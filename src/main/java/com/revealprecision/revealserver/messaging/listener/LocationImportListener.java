package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.messaging.message.LocationRelationshipMessage;
import com.revealprecision.revealserver.persistence.es.HierarchyDetailsElastic;
import com.revealprecision.revealserver.util.ElasticModelUtil;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Profile("Simulation & (Listening | location-import-listener))")
public class LocationImportListener extends Listener {

  private final RestHighLevelClient client;

  @Value("${reveal.elastic.index-name}")
  String elasticIndex;

  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('LOCATIONS_IMPORTED')}", groupId = "reveal_server_group")
  public void locationImport(LocationRelationshipMessage message) throws IOException {
    Map<String, Object> parameters = new HashMap<>();
    Map<String, List<String>> object = new HashMap<>();
    if (message.getAncestry() != null) {
      object.put(message.getLocationHierarchyIdentifier().toString(),
          message.getAncestry().stream()
              .map(UUID::toString)
              .collect(Collectors.toList()));
    }
    parameters.put("object", object);
    parameters.put("key", message.getLocationHierarchyIdentifier().toString());

    HierarchyDetailsElastic hierarchyDetailsElastic = new HierarchyDetailsElastic();
    if (message.getAncestry() != null) {
      hierarchyDetailsElastic.setAncestry(message.getAncestry().stream()
          .map(UUID::toString)
          .collect(Collectors.toList()));
    }
    if (message.getParentLocationIdentifier() != null) {
      hierarchyDetailsElastic.setParent(message.getParentLocationIdentifier().toString());
    }
    hierarchyDetailsElastic.setGeographicLevelNumber(message.getGeoNameLevelNumber());

    parameters.put("hierarchyDetailsElastic",
        Map.of(message.getLocationHierarchyIdentifier().toString(),
            ElasticModelUtil.toMapFromHierarchyDetailsElastic(hierarchyDetailsElastic)));

    Script inline = new Script(ScriptType.INLINE, "painless",
        "ctx._source.ancestry.add(params.object);  ctx._source.hierarchyDetailsElastic = params.hierarchyDetailsElastic;",
        parameters);

    UpdateRequest request = new UpdateRequest(
        elasticIndex,
        message.getLocationIdentifier().toString());
    request.script(inline);
    client.update(request, RequestOptions.DEFAULT);
  }
}
