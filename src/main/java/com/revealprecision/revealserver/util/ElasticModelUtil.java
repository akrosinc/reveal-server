package com.revealprecision.revealserver.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.persistence.es.PersonElastic;
import com.revealprecision.revealserver.persistence.es.EntityMetadataElastic;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ElasticModelUtil {

  public static Map<String, Object> toMapFromPersonElastic(PersonElastic personElastic) {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.convertValue(personElastic, new TypeReference<Map<String, Object>>() {});
  }

  public static Map<String, Object> toMapFromPersonMetadata(
      EntityMetadataElastic personMetadataElastic) {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.convertValue(personMetadataElastic, new TypeReference<Map<String, Object>>() {});
  }

  public static Date toDateFromLocalDateTime(LocalDateTime localDate) {
    Instant instant = localDate.toInstant(ZoneOffset.UTC);
    return Date.from(instant);
  }

  public static String bytesToHex(byte[] hash) {
    StringBuilder hexString = new StringBuilder(2 * hash.length);
    for (int i = 0; i < hash.length; i++) {
      String hex = Integer.toHexString(0xff & hash[i]);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }
}
