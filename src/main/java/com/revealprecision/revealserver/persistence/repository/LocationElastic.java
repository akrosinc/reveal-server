package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.es.Location;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.repository.query.Param;

public interface LocationElastic extends ElasticsearchRepository<Location, String> {


  @Query(value = "{\n"
      + "  \"query\": {\n"
      + "    \"bool\": {\n"
      + "      \"must\": {\n"
      + "        \"match\": {\n"
      + "            \"level\":\":level\"\n"
      + "        }\n"
      + "      },\n"
      + "      \"filter\": {\n"
      + "        \"geo_shape\": {\n"
      + "          \"geometry\": {\n"
      + "            \"shape\": {\n"
      + "              \"type\": \"point\",\n"
      + "              \"coordinates\": [  :x,  :y ]\n"
      + "            },\n"
      + "            \"relation\": \"contains\"\n"
      + "          }\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "}")
      Location getParentLocation(@Param("level") String level,@Param("x") double x, @Param("y")double y);
}
