package com.revealprecision.revealserver.persistence.repository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

@ConditionalOnProperty(
    name = "reveal.elastic.enabled",
    havingValue = "true"
)
public interface LocationElasticRepository extends ElasticsearchRepository<com.revealprecision.revealserver.persistence.es.LocationElastic, String> {

}