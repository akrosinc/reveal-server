package com.revealprecision.revealserver.persistence.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface LocationElasticRepository extends ElasticsearchRepository<com.revealprecision.revealserver.persistence.es.LocationElastic, String> {

}
