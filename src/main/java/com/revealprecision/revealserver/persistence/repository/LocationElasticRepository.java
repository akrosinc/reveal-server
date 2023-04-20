package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.es.LocationElastic;
import java.util.List;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface LocationElasticRepository extends ElasticsearchRepository<com.revealprecision.revealserver.persistence.es.LocationElastic, String> {

  List<LocationElastic> findAllByIdIn(List<String> ids);
}
