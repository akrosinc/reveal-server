package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.exceptions.QueryGenerationException;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.actioncondition.Condition;
import com.revealprecision.revealserver.persistence.domain.actioncondition.Query;
import com.revealprecision.revealserver.props.ConditionQueryProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service

public class EntityFilterService {

  private final JdbcTemplate jdbcTemplate;
  private final ConditionQueryProperties conditionQueryProperties;
  private final LocationRelationshipService locationRelationshipService;
  private final LocationHierarchyService locationHierarchyService;

  private final static String WHERE = " WHERE ";

  private final static String PERSON_TABLE = "person";

  private final static String LOCATION_TABLE = "location";

  @Autowired
  public EntityFilterService(JdbcTemplate jdbcTemplate,
      ConditionQueryProperties conditionQueryProperties,
      LocationRelationshipService locationRelationshipService,
      LocationHierarchyService locationHierarchyService) {
    this.conditionQueryProperties = conditionQueryProperties;
    this.jdbcTemplate = jdbcTemplate;
    this.locationRelationshipService = locationRelationshipService;
    this.locationHierarchyService = locationHierarchyService;
  }

  public List<UUID> filterEntities(Query query, UUID planIdentifier,
      UUID locationHierarchyIdentifier)
      throws QueryGenerationException {

    List<Pair<UUID, String>> locations = queryDBAndRetrieveListOfLocationsLinkedToPlanJurisdiction(
        planIdentifier);

    LocationHierarchy locationHierarchy = locationHierarchyService.findByIdentifier(
        locationHierarchyIdentifier);

    Set<Location> structures = locationRelationshipService.getStructuresForPlanIfHierarchyHasStructure(
        locationHierarchy,
        locations);

    List<String> taskLocationUuids = locations.stream().map(Pair::getFirst).map(UUID::toString)
        .collect(Collectors.toList());

    taskLocationUuids.addAll(structures.stream().map(Location::getIdentifier).map(UUID::toString)
        .collect(Collectors.toList()));

    String planLocations = getConditionFragmentsForPlanLocations(taskLocationUuids);

    String queryFinal = null;

    List<Condition> joinConditions = query.getJoinConditions();
    String joinTableFragment = getJoinFragment(joinConditions);

    List<String> joinedConditionList = getQueryFragmentListJoinedConditions(joinConditions);

    if (!(query.getAndConditions().size() == 0 && query.getOrConditions().size() == 0)) {

      List<Condition> andConditions = query.getAndConditions();
      List<Condition> orConditions = query.getOrConditions();

      List<String> groupedOrConditions = getQueryFragmentListForGroupedOrConditions(orConditions);

      List<String> andConditionList = getQueryFragmentListForAndConditions(andConditions);

      String wherePartBeforeLocationJurisdiction = combineGroupedOrConditionsAndAndConditions(
          groupedOrConditions, andConditionList, joinedConditionList);

      if (query.getEntity().equalsIgnoreCase(LOCATION_TABLE)) {

        queryFinal = conditionQueryProperties.getLocationWithConditionsQuery() + "\n"
            + joinTableFragment
            + WHERE
            + (wherePartBeforeLocationJurisdiction.equals("") ? " "
            : wherePartBeforeLocationJurisdiction + " AND \n")
            + planLocations;
      }

      if (query.getEntity().equalsIgnoreCase(PERSON_TABLE)) {
        queryFinal =
            conditionQueryProperties.getPersonWithConditionQueryWithinALocationJurisdiction() + "\n"
                + joinTableFragment
                + WHERE
                + (wherePartBeforeLocationJurisdiction.equals("") ? " "
                : wherePartBeforeLocationJurisdiction + " AND \n")
                + planLocations;
      }
    } else {

      String wherePartBeforeLocationJurisdiction = combineGroupedOrConditionsAndAndConditions(
          new ArrayList<>(), new ArrayList<>(), joinedConditionList);

      queryFinal =
          conditionQueryProperties.getLocationWithoutConditionQuery() + "\n"
              + joinTableFragment
              + WHERE
              + (wherePartBeforeLocationJurisdiction.equals("") ? " "
              : wherePartBeforeLocationJurisdiction + " AND \n")
              + planLocations;
    }
    if (queryFinal == null) {
      throw new QueryGenerationException(
          "Postgres query cannot be built for Query Object: " + query.toString());
    }
    return new ArrayList<>(jdbcTemplate.query(queryFinal,
        (rs, rowNum) -> ((UUID) rs.getObject("identifier"))));
  }

  private String getJoinFragment(List<Condition> joinConditions) {
    //LEFT JOIN condition.joinentity joinenetity_join on "joinenetity_join".identifier = a.joinentity_identifier
    return joinConditions.stream().map(condition ->
        "LEFT JOIN ".concat(condition.getJoinedEntity()).concat(" ")
            .concat(condition.getJoinedEntity()).concat("_join").concat(" ON ")
            .concat(condition.getJoinedEntity()).concat("_join").concat(".").concat("identifier")
            .concat("=")
            .concat("a.").concat(condition.getJoinedEntity()).concat("_identifier")
    ).collect(Collectors.joining("\n"));
  }

  private String combineGroupedOrConditionsAndAndConditions(List<String> groupedOrConditions,
      List<String> andConditionList, List<String> joinConditions) {
    return String.join(" AND ", groupedOrConditions) +
        ((!groupedOrConditions.isEmpty() && !andConditionList.isEmpty()) ? " AND " : "")
        + String.join(" AND ", andConditionList)
        + ((!groupedOrConditions.isEmpty() && !andConditionList.isEmpty()) ? " AND " : "")
        + String.join(" AND ", joinConditions);
  }

  private List<String> getQueryFragmentListForAndConditions(List<Condition> andConditions) {
    List<String> andConditionList = new ArrayList<>();
    for (Condition andCondition : andConditions) {
      andConditionList = getConditionQueryParts(andCondition, andConditionList);
    }
    return andConditionList;
  }

  private List<String> getQueryFragmentListJoinedConditions(List<Condition> joinConditions) {
    List<String> joinedConditions = new ArrayList<>();
    for (Condition joinCondition : joinConditions) {
      joinedConditions = getConditionQueryParts(joinCondition, joinedConditions);
    }

    return joinedConditions;
  }


  private List<String> getQueryFragmentListForGroupedOrConditions(List<Condition> orConditions) {
    Map<String, List<String>> whereOrConditions = new HashMap<>();
    for (Condition orCondition : orConditions) {
      if (whereOrConditions.containsKey(orCondition.getGroup())) {
        List<String> orConditionList = whereOrConditions.get(orCondition.getGroup());
        orConditionList = getConditionQueryParts(orCondition, orConditionList);
        whereOrConditions.put(orCondition.getGroup(), orConditionList);
      } else {
        List<String> orConditionList = new ArrayList<>();
        orConditionList = getConditionQueryParts(orCondition, orConditionList);
        whereOrConditions.put(orCondition.getGroup(), orConditionList);
      }
    }

    List<String> groupedOrConditions = new ArrayList<>();

    whereOrConditions.forEach((k, v) -> {
      String str = "(" + String.join(" OR ", v) + ")";
      groupedOrConditions.add(str);
    });
    return groupedOrConditions;
  }


  private String getConditionFragmentsForPlanLocations(List<String> locations) {

    return "lr.location_identifier in (".concat(locations.stream()
        .map(planLocation -> "'".concat(planLocation).concat("'"))
        .collect(Collectors.joining(" , "))).concat(")");
  }

  /**
   * Returns query condition segments given a list of location uuid with the placement of
   * parenthesis Example give List of ["9f34814c-fc38-4ddb-9cac-a9b2890fea9f","36e8d03e-23d9-41bd-b2c0-45dd8bcdd8ee"]:
   * will return -
   * lr.ancestry @> ARRAY["9f34814c-fc38-4ddb-9cac-a9b2890fea9f"] OR lr.ancestry @>
   * ARRAY["36e8d03e-23d9-41bd-b2c0-45dd8bcdd8ee"]
   */
  private String getConditionFragmentsForPlanLocationJurisdictions(List<String> locations) {

    return "(".concat(locations.stream()
        .map(planLocation -> "lr.ancestry @> ARRAY['".concat(planLocation).concat("']::uuid[]"))
        .collect(Collectors.joining(" OR "))).concat(")");
  }

  private List<Pair<UUID, String>> queryDBAndRetrieveListOfLocationsLinkedToPlanJurisdiction(
      UUID planIdentifier) {
    return jdbcTemplate.query(
        conditionQueryProperties.getPlanLocationsQuery() + "'" + planIdentifier + "'",
        (rs, rowNum) -> {
          Object location_identifier = rs.getObject("location_identifier");
          String geographic_level_name = rs.getString("geographic_level_name");
          return Pair.of(UUID.fromString(location_identifier.toString()), geographic_level_name);
        });
  }

  /**
   * Generates the query conditions for core fields and tagged properties Each field is matched
   * against its datatype and an applicable condition is created.
   * <p>
   * Examples:
   * Person.[core_date]birth_date BETWEEN 2012-02-17,2017-02-17 will be -
   * ( a.birth_date >= '2012-02-17' and a.birth_date <= '2017-02-17' )
   * <p>
   * Location.[tag_boolean]is_eligibleForMDA == true will be -
   *
   * ( (am.entity_value->'is_eligibleForMDA')::bool
   * = true )
   */
  private List<String> getConditionQueryParts(Condition condition,
      List<String> conditionListPassed) {

    List<String> conditionList = new ArrayList<>(conditionListPassed);

    String con = "";

    switch (condition.getType()) {
      case "tag":
        con = "am.entity_value->'" + condition.getProperty() + "'->>'value'";
        break;
      case "core":
        con = "a." + condition.getProperty();
        break;
      case "coreJoin":
        con = condition.getJoinedEntity().concat("_join.").concat(condition.getProperty());
        break;
    }

    switch (condition.getOperator()) {
      case "BETWEEN":
        if (condition.getDataType().equals("int")) {
          conditionList.add("((" + con + ")::int >= " + condition.getValue().get(0) + " OR " +
              "(" + con + ")::int <= " + condition.getValue().get(1) + ")");
        }
        if (condition.getDataType().equals("date")) {
          conditionList.add("(" + con + " >= '" + condition.getValue().get(0) + "' OR " +
              "" + con + " <= '" + condition.getValue().get(1) + "')");
        }
        break;
      case "==":
        if (condition.getDataType().equals("int")) {
          conditionList.add("(" + con + ")::int = " + condition.getValue().get(0));
        }
        if (condition.getDataType().equals("string")) {
          conditionList.add(con + " = '" + condition.getValue().get(0) + "'");
        }
        if (condition.getDataType().equals("boolean")) {
          conditionList.add("(" + con + ")::bool = " + condition.getValue().get(0));
        }
        if (condition.getDataType().equals("date")) {
          conditionList.add("" + con + " = '" + condition.getValue().get(0) + "'");
        }
        break;
      default:
        if (condition.getDataType().equals("int")) {
          conditionList.add(
              "(" + con + ")::int " + condition.getOperator() + " " + condition.getValue().get(0));
        }
        if (condition.getDataType().equals("string")) {
          conditionList.add(
              con + " " + condition.getOperator() + " '" + condition.getValue().get(0) + "'");
        }
        if (condition.getDataType().equals("boolean")) {
          conditionList.add(
              "(" + con + ")::bool " + condition.getOperator() + " " + condition.getValue().get(0));
        }
        if (condition.getDataType().equals("date")) {
          conditionList.add(
              "" + con + " " + condition.getOperator() + " " + condition.getValue().get(0) + "'");
        }
        break;
    }

    return conditionList;
  }
}
