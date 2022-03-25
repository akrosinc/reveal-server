package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.props.BusinessStatusProperties;
import com.revealprecision.revealserver.util.UserUtils;
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
  BusinessStatusProperties businessStatusProperties;

  @Autowired
  EntityTagService entityTagService;


  public Map<String, Pair<Class<?>, Object>> getMetadataFor(String entityTable, UUID identifier) {

    String metadataTable = entityTable + "_metadata";

    String query = "SELECT t.key,t.value->>'value',t.value->>'entity_tag',et.value_type From (\n"
        + "SELECT  (jsonb_each(lm.entity_value)).*  from " + entityTable + " l\n" + "inner join "
        + metadataTable+" lm on l.identifier = lm." + entityTable + "_identifier\n"
        + "where l.identifier = '" + identifier + "'\n"
        + "\t) as t left join entity_tag et on et.identifier = (t.value->>'entity_tag')::uuid";

    return jdbcTemplate.query(query, (rs, rowNum) -> getMapFromResultSet(rs)).stream()
        .filter(Objects::nonNull).flatMap(mapOfKeyAndPair -> mapOfKeyAndPair.entrySet().stream())
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

  }

  public void createOrUpdateMetadata(UUID entityIdentifier, String entity, String tagName,
      Object value, Class<?> entityDataType) {
    String quotes = (entityDataType.equals(String.class) ? "'" : "");


// upsert String resolves to below insert statement

//    INSERT INTO location_metadata(
//        identifier, entity_value, location_identifier, entity_status, created_by, created_datetime, modified_by, modified_datetime)
//    values
//        (uuid_generate_v4()
//            ,(jsonb_build_object(
//                'business_status'
//                ,(jsonb_build_object(
//                    'entity_tag'
//                    ,(SELECT identifier from entity_tag where tag = 'business_status'
//                    AND lookup_entity_type_identifier = (SELECT identifier FROM lookup_entity_type WHERE table_name = 'location'))
//                ,'value'
//                ,'NOT_VISITED'
//            ))
//        ))
//	  ,'76a83898-efec-4470-921e-4834d8245c7d'
//        , 'ACTIVE'
//        , '649f338b-eb53-4832-9562-f695e9cc44e7'
//        , '2022-01-12 13:54:22.106221+00'
//        , '649f338b-eb53-4832-9562-f695e9cc44e7'
//        , '2022-01-12 13:54:22.106221+00')
//    ON CONFLICT (location_identifier) DO UPDATE
//    SET entity_value =
//        jsonb_set(
//            location_metadata.entity_value
//            ,'{business_status}'
//            ,(jsonb_build_object(
//                'entity_tag'
//                ,(SELECT identifier from entity_tag where tag = 'business_status' AND lookup_entity_type_identifier =
//                    (SELECT identifier FROM lookup_entity_type WHERE table_name = 'location'))
//            ,  'value','NOT_VISITED'
//        ))
//				,true
//			),
//    WHERE location_metadata.location_identifier = '76a83898-efec-4470-921e-4834d8245c7d'

    String metadataTable = entity + "_metadata";

    String upsertStatement =
        "INSERT INTO " + metadataTable + "( identifier,  entity_value, " + entity
            + "_identifier,entity_status,created_by,created_datetime,modified_by,modified_datetime)"
            + " values ( " + "uuid_generate_v4()," + "(jsonb_build_object( '" + tagName + "',"
            + "(jsonb_build_object('entity_tag',"
            + "( SELECT identifier from entity_tag where tag = '" + tagName
            + "' AND lookup_entity_type_identifier = (SELECT identifier FROM lookup_entity_type WHERE table_name = '"
            + entity + "')) " + ",'value'," + quotes + value + quotes + "))))" + ",'"
            + entityIdentifier + "'," + "'" + EntityStatus.ACTIVE + "', '"
            + UserUtils.getKeyCloakPrincipal().getName() + "', now(), '"
            + UserUtils.getKeyCloakPrincipal().getName() + "', now())" + "ON CONFLICT (" + metadataTable
            + "_identifier) DO UPDATE SET  entity_value = jsonb_set( " + metadataTable
            + ".entity_value," + "'{" + tagName + "}'," + "(jsonb_build_object("
            + "'entity_tag'," + "(SELECT identifier from entity_tag where tag = '" + tagName
            + "' AND lookup_entity_type_identifier = (SELECT identifier FROM lookup_entity_type WHERE table_name = '"
            + entity + "'))," + "  'value'," + quotes + value + quotes + "" + ")),true)" + "WHERE "
            + metadataTable + "." + entity + "_identifier = '" + entityIdentifier + "';";

    jdbcTemplate.update(upsertStatement);
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
