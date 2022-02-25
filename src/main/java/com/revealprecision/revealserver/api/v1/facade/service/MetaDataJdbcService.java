package com.revealprecision.revealserver.api.v1.facade.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class MetaDataJdbcService {

  @Autowired
  JdbcTemplate jdbcTemplate;

  public Map<String, Pair<Class<?>, Object>> getMetadataFor(String entityTable, UUID identifier) {

    String query = "SELECT t.key,t.value->>'value',t.value->>'entity_tag',et.value_type From (\n"
        + "SELECT  (jsonb_each(lm.entity_value)).*  from " + entityTable + " l\n"
        + "inner join " + entityTable + "_metadata lm on l.identifier = lm." + entityTable
        + "_identifier\n"
        + "where l.identifier = '" + identifier + "'\n"
        + "\t) as t left join entity_tags et on et.identifier = (t.value->>'entity_tag')::uuid";

    return jdbcTemplate.query(query, (rs, rowNum) ->
            getMapFromResultSet(rs)).stream().filter(Objects::nonNull).flatMap(
            mapOfKeyAndPair -> mapOfKeyAndPair.entrySet().stream())
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

  }


  private Map<String, Pair<Class<?>, Object>> getMapFromResultSet(ResultSet rs)
      throws SQLException {
    Map<String, Pair<Class<?>, Object>> stringObjectMap2 = new HashMap<>();

    String valueType = rs.getString(4);

    Pair<Class<?>, Object> pair = null;

    if (valueType == null) {
      return null;
    }

    switch (valueType) {
      case "integer":
        pair = Pair.of(Integer.class, rs.getInt(2));
        break;
      case "string":
        pair = Pair.of(String.class, rs.getString(2));
        break;
      case "boolean":
        pair = Pair.of(Boolean.class, rs.getBoolean(2));
        break;
    }

    stringObjectMap2.put(rs.getString(1), pair);

    return stringObjectMap2;
  }
}
