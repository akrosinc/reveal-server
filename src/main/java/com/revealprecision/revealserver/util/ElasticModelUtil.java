package com.revealprecision.revealserver.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.persistence.es.PersonElastic;
import com.revealprecision.revealserver.persistence.es.PersonMetadataElastic;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ElasticModelUtil {

  public static Map<String, Object> toMapFromPersonElastic(PersonElastic personElastic) {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.convertValue(personElastic, new TypeReference<Map<String, Object>>() {});
  }

  public static Map<String, Object> toMapFromPersonMetadata(PersonMetadataElastic personMetadataElastic) {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.convertValue(personMetadataElastic, new TypeReference<Map<String, Object>>() {});
  }
}
