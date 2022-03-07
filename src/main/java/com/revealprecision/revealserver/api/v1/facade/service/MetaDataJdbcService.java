package com.revealprecision.revealserver.api.v1.facade.service;

import static com.revealprecision.revealserver.api.v1.facade.service.TaskFacadeService.GENERAL;

import com.revealprecision.revealserver.api.v1.facade.properties.TaskFacadeProperties;
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

  @Autowired
  TaskFacadeProperties taskFacadeProperties;


  public Map<String, Pair<Class<?>, Object>> getMetadataFor(String entityTable, UUID identifier) {

    String query = "SELECT t.key,t.value->>'value',t.value->>'entity_tag',et.value_type From (\n"
        + "SELECT  (jsonb_each(lm.entity_value)).*  from " + entityTable + " l\n" + "inner join "
        + entityTable + "_metadata lm on l.identifier = lm." + entityTable + "_identifier\n"
        + "where l.identifier = '" + identifier + "'\n"
        + "\t) as t left join entity_tags et on et.identifier = (t.value->>'entity_tag')::uuid";

    return jdbcTemplate.query(query, (rs, rowNum) -> getMapFromResultSet(rs)).stream()
        .filter(Objects::nonNull).flatMap(mapOfKeyAndPair -> mapOfKeyAndPair.entrySet().stream())
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

  }

  public int updateMetadata(UUID entityIdentifier, String entity, Object value, UUID planIdentifier,
      Class<?> entityDataType) {

    String businessStatusField = taskFacadeProperties.getBusinessStatusMapping().get(GENERAL);

    if (planIdentifier != null) {
      String businessStatusFieldProperty = taskFacadeProperties.getBusinessStatusMapping()
          .get(planIdentifier.toString());
      if (businessStatusFieldProperty != null) {
        businessStatusField = businessStatusFieldProperty;
      }
    }

    String quotes = (entityDataType.equals(String.class) ? "'" : "");

    String query = "update " + entity + "_metadata\n" + " set entity_value = jsonb_set(\n"
        + "  entity_value,\n" + "'{" + businessStatusField + "}',\n" + "(jsonb_build_object(\n"
        + "                         'entity_tag',\n"
        + "                         (SELECT identifier From entity_tags WHERE tag='"
        + businessStatusField + "'),\n" + "                         'value'," + quotes + value
        + quotes + "\n" + "                     )),true)\n" + " WHERE " + entity + "_identifier = '"
        + entityIdentifier + "'";

    return jdbcTemplate.update(query);

  }


  private Map<String, Pair<Class<?>, Object>> getMapFromResultSet(ResultSet rs)
      throws SQLException {
    Map<String, Pair<Class<?>, Object>> stringObjectMap2 = new HashMap<>();

    String valueType = rs.getString(4);

    Pair<Class<?>, Object> pair = null;

    if (valueType == null) {
      pair = Pair.of(String.class, rs.getString(2));
    } else {
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
    }

    stringObjectMap2.put(rs.getString(1), pair);

    return stringObjectMap2;
  }
}
