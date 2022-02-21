package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.persistence.domain.actioncondition.Condition;
import com.revealprecision.revealserver.persistence.domain.actioncondition.Query;
import com.revealprecision.revealserver.props.ConditionQueryProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class EntityFilterService {

  private final JdbcTemplate jdbcTemplate;
  private final ConditionQueryProperties conditionQueryProperties;

  final static String WHERE = " WHERE ";

  final static String PERSON = "person";

  final static String LOCATION = "location";

  @Autowired
  public EntityFilterService(JdbcTemplate jdbcTemplate,
      ConditionQueryProperties conditionQueryProperties) {
    this.conditionQueryProperties = conditionQueryProperties;
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<UUID> filterEntities(Query query, UUID planIdentifier) {

    List<String> locations = queryDBAndRetrieveListOfLocationsLinkedToPlanJurisdiction(
        planIdentifier);

    String locationJurisdictionsPart = getConditionFragmentsForPlanLocationJurisdictions(locations);

    String queryFinal = null;

    if (!(query.getAndConditions().size() == 0 && query.getOrConditions().size() == 0)) {

      List<Condition> andConditions = query.getAndConditions();
      List<Condition> orConditions = query.getOrConditions();

      List<String> groupedOrConditions = getQueryFragmentListForGroupedOrConditions(orConditions);

      List<String> andConditionList = getQueryFragmentListForAndConditions(andConditions);

      String wherePartBeforeLocationJurisdiction = combineGroupedOrConditionsAndAndConditions(
          groupedOrConditions, andConditionList);

      if (query.getEntity().equalsIgnoreCase(LOCATION)) {

        queryFinal = conditionQueryProperties.getLocationWithConditionsQuery() + "\n"
            + WHERE
            + (wherePartBeforeLocationJurisdiction.equals("") ? " "
            : wherePartBeforeLocationJurisdiction + " AND \n")
            + locationJurisdictionsPart;
      }

      if (query.getEntity().equalsIgnoreCase(PERSON)) {
        queryFinal =
            conditionQueryProperties.getPersonWithConditionQueryWithinALocationJurisdiction() + "\n"
                + WHERE
                + (wherePartBeforeLocationJurisdiction.equals("") ? " "
                : wherePartBeforeLocationJurisdiction + " AND \n")
                + locationJurisdictionsPart;
      }
    } else {
      queryFinal =
          conditionQueryProperties.getLocationWithoutConditionQuery() + "\n"
              + WHERE
              + locationJurisdictionsPart;
    }

    return new ArrayList<>(jdbcTemplate.query(queryFinal,
        (rs, rowNum) -> ((UUID) rs.getObject("identifier"))));
  }

  private String combineGroupedOrConditionsAndAndConditions(List<String> groupedOrConditions,
      List<String> andConditionList) {
    return String.join(" AND ", groupedOrConditions) +
        ((!groupedOrConditions.isEmpty() && !andConditionList.isEmpty()) ? " AND " : "")
        + String.join(" AND ", andConditionList);
  }

  private List<String> getQueryFragmentListForAndConditions(List<Condition> andConditions) {
    List<String> andConditionList = new ArrayList<>();
    for (Condition andCondition : andConditions) {
      andConditionList = getConditionQueryParts(andCondition, andConditionList);
    }
    return andConditionList;
  }

  /**
   * Given a list of Grouped Or conditions the method will create the condition query fragments with the applicable data types and either reference the
   * core fields (table attributes) or the tagged values
   *
   * Example:
   *  Given
   *  #group0 -> List.of [ (am.entity_value->'eye_color' = 'brown' ), (am.entity_value->'eye_color' = 'black' )]
   *
   *  Results
   *
   */
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
      String str = "(" + v.stream().collect(Collectors.joining(" OR ")) + ")";
      groupedOrConditions.add(str);
    });
    return groupedOrConditions;
  }


  /**
   * Returns query condition segments given a list of location uuid with the placement of parenthesis
   * Example give List of ["9f34814c-fc38-4ddb-9cac-a9b2890fea9f","36e8d03e-23d9-41bd-b2c0-45dd8bcdd8ee"]:
   * will return -
   * lr.ancestry @> ARRAY["9f34814c-fc38-4ddb-9cac-a9b2890fea9f"] OR lr.ancestry @> ARRAY["36e8d03e-23d9-41bd-b2c0-45dd8bcdd8ee"]
   */
  private String getConditionFragmentsForPlanLocationJurisdictions(List<String> locations) {

    return "(".concat(locations.stream()
        .map(planLocation -> "lr.ancestry @> ARRAY['".concat(planLocation).concat("']::uuid[]"))
        .collect(Collectors.joining(" OR "))).concat(")");
  }

  private List<String> queryDBAndRetrieveListOfLocationsLinkedToPlanJurisdiction(
      UUID planIdentifier) {
    List<String> location_identifier = jdbcTemplate.query(
        conditionQueryProperties.getPlanLocationsQuery() + "'" + planIdentifier + "'",
        (rs, rowNum) -> rs.getObject("location_identifier").toString());
    return location_identifier;
  }

  /**
   * Generates the query conditions for core fields and tagged properties
   * Each field is matched against its datatype and an applicable condition is created.
   *
   * Examples:
   * Person.[core_date]birth_date BETWEEN 2012-02-17,2017-02-17 will be -
   * ( a.birth_date >= '2012-02-17' and a.birth_date <= '2017-02-17' )
   *
   * Location.[tag_boolean]is_eligibleForMDA == true will be -
   * ( (am.entity_value->'is_eligibleForMDA')::bool = true )
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
