package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.es.PersonElastic;
import com.revealprecision.revealserver.util.ElasticModelUtil;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("Elastic")
public class MetadataEsServiceImpl implements MetadataEsService {

  private final RestHighLevelClient client;

  @Value("${reveal.elastic.index-name}")
  String elasticIndex;

  public void updatePersonDetailsOnElasticSearch(Person person) throws IOException {
    PersonElastic personElastic = new PersonElastic(person);
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("person", ElasticModelUtil.toMapFromPersonElastic(personElastic));
    parameters.put("personId", personElastic.getIdentifier());
    UpdateByQueryRequest request = new UpdateByQueryRequest(elasticIndex);
    List<String> locationIds = person.getLocations().stream()
        .map(loc -> loc.getIdentifier().toString()).collect(Collectors.toList());

    request.setQuery(QueryBuilders.termsQuery("_id", locationIds));
    request.setScript(new Script(ScriptType.INLINE, "painless",
        "def foundPerson = ctx._source.person.find(attr-> attr.identifier == params.personId);"
            + " if(foundPerson == null) {ctx._source.person.add(params.person);}", parameters));
    client.updateByQuery(request, RequestOptions.DEFAULT);
  }
}
