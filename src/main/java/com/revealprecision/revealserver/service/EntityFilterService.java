package com.revealprecision.revealserver.service;

import static com.revealprecision.revealserver.constants.EntityTagDataTypes.DATE;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.DOUBLE;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.INTEGER;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.STRING;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.dto.factory.LocationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.PersonMainDataResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.DataFilterRequest;
import com.revealprecision.revealserver.api.v1.dto.request.EntityFilterRequest;
import com.revealprecision.revealserver.api.v1.dto.request.SearchValue;
import com.revealprecision.revealserver.api.v1.dto.response.FeatureSetResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationPropertyResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.api.v1.dto.response.PersonMainData;
import com.revealprecision.revealserver.constants.EntityTagDataTypes;
import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.enums.SignEntity;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.exceptions.QueryGenerationException;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.CoreField;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LookupEntityType;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.actioncondition.Condition;
import com.revealprecision.revealserver.persistence.domain.actioncondition.Query;
import com.revealprecision.revealserver.persistence.es.LocationElastic;
import com.revealprecision.revealserver.persistence.es.PersonElastic;
import com.revealprecision.revealserver.props.ConditionQueryProperties;
import com.revealprecision.revealserver.util.ActionUtils;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.InnerHitBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EntityFilterService {

  private final JdbcTemplate jdbcTemplate;
  private final ConditionQueryProperties conditionQueryProperties;
  private final LocationRelationshipService locationRelationshipService;
  private final LocationHierarchyService locationHierarchyService;
  private final PersonService personService;
  private final LookupEntityTypeService lookupEntityTypeService;
  private final EntityTagService entityTagService;
  private final CoreFieldService coreFieldService;
  private final RestHighLevelClient client;

  @Value("${reveal.elastic.index-name}")
  String elasticIndex;

  private final static String WHERE = " WHERE ";

  private final static String PERSON_TABLE = "person";

  private final static String LOCATION_TABLE = "location";

  @Autowired
  public EntityFilterService(JdbcTemplate jdbcTemplate,
      ConditionQueryProperties conditionQueryProperties,
      LocationRelationshipService locationRelationshipService,
      LocationHierarchyService locationHierarchyService,
      PersonService personService,
      LookupEntityTypeService lookupEntityTypeService,
      EntityTagService entityTagService,
      CoreFieldService coreFieldService,
      RestHighLevelClient client) {
    this.conditionQueryProperties = conditionQueryProperties;
    this.jdbcTemplate = jdbcTemplate;
    this.locationRelationshipService = locationRelationshipService;
    this.locationHierarchyService = locationHierarchyService;
    this.personService = personService;
    this.lookupEntityTypeService = lookupEntityTypeService;
    this.entityTagService = entityTagService;
    this.coreFieldService = coreFieldService;
    this.client = client;
  }

  public List<UUID> filterEntities(Query query, Plan plan,
      UUID locationHierarchyIdentifier, Action action)
      throws QueryGenerationException {

    List<Pair<UUID, String>> locations = queryDBAndRetrieveListOfLocationsLinkedToPlanJurisdiction(
        plan.getIdentifier());

    log.debug("plan_locations size {} ", locations.size());
    log.trace("plan_locations {} ", locations);

    if (locations.isEmpty()) {
      return new ArrayList<>();
    } else {

      LocationHierarchy locationHierarchy = locationHierarchyService.findByIdentifier(
          locationHierarchyIdentifier);

      List<Pair<UUID, String>> locationsForTaskGeneration = new ArrayList<>();

      if (plan.getPlanTargetType().getGeographicLevel().getName()
          .equals(LocationConstants.STRUCTURE)) {
        String nodeAboveStructure = locationHierarchy.getNodeOrder().get(
            locationHierarchy.getNodeOrder().indexOf(LocationConstants.STRUCTURE) - 1);
        locationsForTaskGeneration.addAll(locations.stream()
            .filter(location -> location.getSecond().equals(nodeAboveStructure))
            .collect(Collectors.toList()));
      } else {
        locationsForTaskGeneration.addAll(locations.stream()
            .filter(location -> location.getSecond()
                .equals(plan.getPlanTargetType().getGeographicLevel().getName()))
            .collect(Collectors.toList()));
      }

      List<String> taskLocationUuids = new ArrayList<>();
      if (query == null) {

        if (plan.getPlanTargetType().getGeographicLevel().getName()
            .equals(LocationConstants.STRUCTURE)) {
          Set<Location> structures = locationRelationshipService.getStructuresForPlanIfHierarchyHasStructure(
              locationHierarchy,
              locationsForTaskGeneration);

          log.debug("structures size: {}", structures.size());
          log.trace("structures: {}", structures);

          taskLocationUuids.addAll(
              structures.stream().map(Location::getIdentifier).map(UUID::toString)
                  .collect(Collectors.toList()));

          if (ActionUtils.isActionForLocation(action)) {
            return structures.stream().map(Location::getIdentifier).collect(Collectors.toList());
          } else if (ActionUtils.isActionForPerson(action)) {
            return personService.getPeopleByLocations(new ArrayList<>(structures)).stream().map(
                Person::getIdentifier).collect(Collectors.toList());
          } else {
            return structures.stream().map(Location::getIdentifier).collect(Collectors.toList());
          }
        } else {
          return locationsForTaskGeneration.stream().map(Pair::getFirst).collect(
              Collectors.toList());
        }
      } else {
        taskLocationUuids = locations.stream().map(Pair::getFirst).map(UUID::toString)
            .collect(Collectors.toList());

        String planLocations = getConditionFragmentsForPlanLocations(taskLocationUuids);

        String queryFinal = null;

        List<Condition> joinConditions = query.getJoinConditions();
        String joinTableFragment = getJoinFragment(joinConditions);

        List<String> joinedConditionList = getQueryFragmentListJoinedConditions(joinConditions);

        if (!(query.getAndConditions().size() == 0 && query.getOrConditions().size() == 0)) {

          List<Condition> andConditions = query.getAndConditions();
          List<Condition> orConditions = query.getOrConditions();

          List<String> groupedOrConditions = getQueryFragmentListForGroupedOrConditions(
              orConditions);

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
                conditionQueryProperties.getPersonWithConditionQueryWithinALocationJurisdiction()
                    + "\n"
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
              "Postgres query cannot be built for Query Object: " + query);
        }
        return new ArrayList<>(jdbcTemplate.query(queryFinal,
            (rs, rowNum) -> ((UUID) rs.getObject("identifier"))));
      }
    }
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
   * will return - lr.ancestry @> ARRAY["9f34814c-fc38-4ddb-9cac-a9b2890fea9f"] OR lr.ancestry @>
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

  private List<String> getConditionQueryParts(Condition condition,
      List<String> conditionListPassed) {

    List<String> conditionList = new ArrayList<>(conditionListPassed);

    String con = "";

    switch (condition.getType()) {
      case "tag":
//        con = "am.entity_value->'" + condition.getProperty() + "'->>'value'";
        break;
      case "core":
        con = "a." + condition.getProperty();
        break;
      case "coreJoin":
        con = condition.getJoinedEntity().concat("_join.").concat(condition.getProperty());
        break;
    }

    if (!condition.getType().equals("tag")) {
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
                "(" + con + ")::int " + condition.getOperator() + " " + condition.getValue()
                    .get(0));
          }
          if (condition.getDataType().equals("string")) {
            conditionList.add(
                con + " " + condition.getOperator() + " '" + condition.getValue().get(0) + "'");
          }
          if (condition.getDataType().equals("boolean")) {
            conditionList.add(
                "(" + con + ")::bool " + condition.getOperator() + " " + condition.getValue()
                    .get(0));
          }
          if (condition.getDataType().equals("date")) {
            conditionList.add(
                "" + con + " " + condition.getOperator() + " " + condition.getValue().get(0) + "'");
          }
          break;
      }
    } else {
      switch (condition.getOperator()) {
        case "BETWEEN":
          if (condition.getDataType().equals("int")) {

            conditionList.add("( am.item_object->>'tag' = '" + condition.getProperty() + "' AND " +
                "( am.item_object->'current'->'value'->>'valueInteger' >= " + condition.getValue()
                .get(0) + " OR " +
                " am.item_object->'current'->'value'->>'valueInteger' <= " + condition.getValue()
                .get(1) + " ))");

          }
          if (condition.getDataType().equals("date")) {
            conditionList.add("( am.item_object->>'tag' = '" + condition.getProperty() + "' AND " +
                "( am.item_object->'current'->'value'->>'valueDateTime' >= '" + condition.getValue()
                .get(0) + "' OR " +
                " am.item_object->'current'->'value'->>'valueDateTime' <= '" + condition.getValue()
                .get(1) + "' ))");
          }
          break;
        case "==":
          if (condition.getDataType().equals("int")) {
            conditionList.add("( am.item_object->>'tag' = '" + condition.getProperty() + "' AND " +
                " am.item_object->'current'->'value'->>'valueInteger' = " + condition.getValue()
                .get(0) + ")");
          }
          if (condition.getDataType().equals("string")) {
            conditionList.add("( am.item_object->>'tag' = '" + condition.getProperty() + "' AND " +
                " am.item_object->'current'->'value'->>'valueString' = '" + condition.getValue()
                .get(0) + "')");
          }
          if (condition.getDataType().equals("boolean")) {
            conditionList.add("( am.item_object->>'tag' = '" + condition.getProperty() + "' AND " +
                " am.item_object->'current'->'value'->>'valueBoolean' = " + condition.getValue()
                .get(0) + ")");
          }
          if (condition.getDataType().equals("date")) {
            conditionList.add("( am.item_object->>'tag' = '" + condition.getProperty() + "' AND " +
                " am.item_object->'current'->'value'->>'valueDate' = " + condition.getValue().get(0)
                + ")");
          }
          break;
        default:
          if (condition.getDataType().equals("int")) {
            conditionList.add("( am.item_object->>'tag' " + condition.getOperator() + " '"
                + condition.getProperty() + "' AND " +
                " am.item_object->'current'->'value'->>'valueInteger' = " + condition.getValue()
                .get(0) + ")");
          }
          if (condition.getDataType().equals("string")) {
            conditionList.add("( am.item_object->>'tag' = '" + condition.getProperty() + "' AND " +
                " am.item_object->'current'->'value'->>'valueString' " + condition.getOperator()
                + " '" + condition.getValue().get(0) + "')");
          }
          if (condition.getDataType().equals("boolean")) {
            conditionList.add("( am.item_object->>'tag' = '" + condition.getProperty() + "' AND " +
                " am.item_object->'current'->'value'->>'valueBoolean' " + condition.getOperator()
                + " " + condition.getValue().get(0) + ")");
          }
          if (condition.getDataType().equals("date")) {
            conditionList.add("( am.item_object->>'tag' = '" + condition.getProperty() + "' AND " +
                " am.item_object->'current'->'value'->>'valueDate' " + condition.getOperator() + " "
                + condition.getValue().get(0) + ")");
          }
          break;
      }
    }

    return conditionList;
  }

  public FeatureSetResponse filterEntites(DataFilterRequest request)
      throws IOException, ParseException {
    FeatureSetResponse response = new FeatureSetResponse();
    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
    List<EntityFilterRequest> personFilters = new ArrayList<>();
    List<EntityFilterRequest> locationFilters = new ArrayList<>();

    for (EntityFilterRequest req : request.getEntityFilters()) {
      LookupEntityType lookupEntityType = lookupEntityTypeService.getLookUpEntityTypeById(
          req.getEntityIdentifier());
      if (lookupEntityType.getTableName().equals("person")) {
        personFilters.add(req);
        boolQuery.must(nestedPersonQuery(personFilters));
      } else {
        locationFilters.add(req);
      }
    }

    for (EntityFilterRequest req : locationFilters) {
      if (req.getRange() == null && req.getValues() == null) {
        boolQuery.must(mustStatement(req));
      } else if (req.getSearchValue() == null && req.getValues() == null) {
        boolQuery.must(rangeStatement(req));
      } else if (req.getSearchValue() == null && req.getRange() == null) {
        boolQuery.must(shouldStatement(req));
      } else {
        throw new ConflictException("Request object bad formatted.");
      }
    }

    if (request.getHierarchyIdentifier() != null && request.getLocationIdentifier() != null) {
      boolQuery.filter(
          QueryBuilders.matchQuery("ancestry.".concat(request.getHierarchyIdentifier().toString()),
              request.getLocationIdentifier().toString()));
    }



    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    sourceBuilder.size(10000);
    sourceBuilder.query(boolQuery);
    SearchRequest searchRequest = new SearchRequest(elasticIndex);
    searchRequest.source(sourceBuilder);
    SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

    List<LocationResponse> locationResponses = new ArrayList<>();
    List<String> parentLocations = new ArrayList<>();
    List<LocationResponse> parentLocationResponses = new ArrayList<>();

    for (SearchHit hit : searchResponse.getHits().getHits()) {
      LocationResponse locToAdd = LocationResponseFactory.fromSearchHit(hit, parentLocations,
          request.getHierarchyIdentifier().toString());

      addPersonsToLocationProperties(hit.getInnerHits(), locToAdd.getProperties());
      locToAdd.getProperties().setSimulationSearchResult(true);

      locationResponses.add(locToAdd);
    }

    SearchSourceBuilder sourceBuilderAll = new SearchSourceBuilder();
    sourceBuilderAll.size(10000);

    SearchRequest searchRequestAll = new SearchRequest(elasticIndex);
    searchRequestAll.source(sourceBuilderAll);
    SearchResponse searchResponseAll = client.search(searchRequestAll, RequestOptions.DEFAULT);

    for (SearchHit hit : searchResponseAll.getHits().getHits()) {
      LocationResponse locToAdd = LocationResponseFactory.fromSearchHit(hit, parentLocations,
          request.getHierarchyIdentifier().toString());
      locToAdd.getProperties().setLevelColor(getGeoLevelColor(locToAdd.getProperties().getGeographicLevel()));
      parentLocationResponses.add(locToAdd);
    }

    if (!parentLocationResponses.isEmpty()) {
      response.setParents(parentLocationResponses);
    }

    response.setType("FeatureCollection");
    response.setFeatures(locationResponses);
    return response;
  }

  private String getGeoLevelColor(String geolevel){

    switch (geolevel){
      case "country":
        return "#ABABAB";
      case "province":
        return "#ABABAB";
      case "county":
        return "#ABABAB";
      case "subcounty":
        return "#ABABAB";
      case "ward":
        return "#ABABAB";
      case "catchment":
        return "#ABABAB";
      default:return "#ABABAB";
    }
  }

  private List<LocationResponse> retrieveParentLocations(List<String> parentIds)
      throws IOException {
    List<LocationResponse> responses = new ArrayList<>();
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    sourceBuilder.query(QueryBuilders.termsQuery("_id", parentIds));
    SearchRequest searchRequest = new SearchRequest(elasticIndex);
    searchRequest.source(sourceBuilder);
    SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
    ObjectMapper mapper = new ObjectMapper();
    for (SearchHit hit : searchResponse.getHits().getHits()) {
      LocationElastic parent = mapper.readValue(hit.getSourceAsString(), LocationElastic.class);
      responses.add(LocationResponseFactory.fromElasticModel(parent));
    }
    return responses;
  }

  private void addPersonsToLocationProperties(Map<String, SearchHits> innerHits,
      LocationPropertyResponse propertyResponse)
      throws JsonProcessingException {
    propertyResponse.setPersons(new ArrayList<>());
    ObjectMapper mapper = new ObjectMapper();
    if (innerHits != null) {
      if (innerHits.containsKey("person")) {
        for (SearchHit hit : innerHits.get("person")) {
          String source = hit.getSourceAsString();
          PersonElastic personElastic = mapper.readValue(source, PersonElastic.class);
          propertyResponse.getPersons()
              .add(PersonMainDataResponseFactory.fromPersonElasticSummary(personElastic));
        }
      }
    }

  }

  public NestedQueryBuilder nestedPersonQuery(List<EntityFilterRequest> requests)
      throws ParseException {
    BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
    for (EntityFilterRequest req : requests) {
      if (req.getRange() == null && req.getValues() == null) {
        boolQueryBuilder.must(mustStatement(req));
      } else if (req.getSearchValue() == null && req.getValues() == null) {
        boolQueryBuilder.must(rangeStatement(req));
      } else if (req.getSearchValue() == null && req.getRange() == null) {
        boolQueryBuilder.must(shouldStatement(req));
      } else {
        throw new ConflictException("Request object bad formatted.");
      }
    }
    return QueryBuilders.nestedQuery("person", boolQueryBuilder, ScoreMode.None).innerHit(
        new InnerHitBuilder());
  }

  private AbstractQueryBuilder<?> mustStatement(EntityFilterRequest request) throws ParseException {
    LookupEntityType lookupEntityType = lookupEntityTypeService.getLookUpEntityTypeById(
        request.getEntityIdentifier());
    BoolQueryBuilder andStatement = QueryBuilders.boolQuery();

    String searchField;
    if (request.getFieldType().equals("tag")) {
      EntityTag entityTag = entityTagService.getEntityTagByIdentifier(request.getFieldIdentifier());
      if (entityTag.getValueType().equals("date")) {
        prepareDate(request);
      }
      if (lookupEntityType.getTableName().equals("location")) {
        searchField = "metadata.";
      } else {
        searchField = lookupEntityType.getTableName().concat(".metadata.");
      }
      andStatement.must(
          QueryBuilders.matchPhraseQuery(searchField.concat("tag"), entityTag.getTag()));

      if (entityTag.getValueType().equals(STRING)) {
        andStatement.must(QueryBuilders.matchPhraseQuery(searchField.concat("value"),
            request.getSearchValue().getValue()));
      } else if (entityTag.getValueType().equals(INTEGER) || entityTag.getValueType()
          .equals(DOUBLE)) {
        if (request.getSearchValue().getSign().equals(SignEntity.EQ)) {
          andStatement.must(QueryBuilders.matchQuery(searchField.concat("valueNumber"),
              request.getSearchValue().getValue()));
        } else {
          andStatement.must(
              rangeQuery(request.getSearchValue().getSign(), searchField.concat("valueNumber"),
                  request.getSearchValue().getValue()));
        }
      } else {
        andStatement.must(QueryBuilders.matchQuery(searchField.concat("value"),
            request.getSearchValue().getValue()));
      }
      return QueryBuilders.nestedQuery("metadata", andStatement,
          ScoreMode.None);
    } else if (request.getFieldType().equals("core")) {
      CoreField coreField = coreFieldService.getCoreFieldByIdentifier(request.getFieldIdentifier());
      if (coreField.getValueType().equals(EntityTagDataTypes.DATE)) {
        prepareDate(request);
      }
      searchField = lookupEntityType.getTableName().concat(".").concat(coreField.getField());
      if (request.getSearchValue().getSign() == SignEntity.EQ) {
        andStatement.must(
            QueryBuilders.matchQuery(searchField, request.getSearchValue().getValue()));
      } else {
        andStatement.must(rangeQuery(request.getSearchValue().getSign(), searchField,
            request.getSearchValue().getValue()));
      }
    } else {
      throw new ConflictException("Unexpected field type: " + request.getFieldType());
    }
    return andStatement;
  }

  private AbstractQueryBuilder<?> shouldStatement(EntityFilterRequest request)
      throws ParseException {
    LookupEntityType lookupEntityType = lookupEntityTypeService.getLookUpEntityTypeById(
        request.getEntityIdentifier());

    BoolQueryBuilder shouldStatement = QueryBuilders.boolQuery();
    String searchField;
    if (request.getFieldType().equals("tag")) {
      EntityTag entityTag = entityTagService.getEntityTagByIdentifier(request.getFieldIdentifier());
      if (entityTag.getValueType().equals(EntityTagDataTypes.DATE)) {
        prepareDate(request);
      }
      if (lookupEntityType.getTableName().equals("location")) {
        searchField = "metadata.";
      } else {
        searchField = lookupEntityType.getTableName().concat(".metadata.");
      }
      shouldStatement.must(
          QueryBuilders.matchPhraseQuery(searchField.concat("tag"), entityTag.getTag()));
      BoolQueryBuilder orStatement = QueryBuilders.boolQuery();
      for (SearchValue value : request.getValues()) {
        if (entityTag.getValueType().equals(STRING)) {
          orStatement.should(
              QueryBuilders.matchPhraseQuery(searchField.concat("value"), value.getValue()));
        } else if (entityTag.getValueType().equals(INTEGER) || entityTag.getValueType()
            .equals(DOUBLE)) {
          if (value.getSign().equals(SignEntity.EQ)) {
            orStatement.should(QueryBuilders.matchQuery(searchField.concat("valueNumber"),
                value.getValue()));
          } else {
            orStatement.should(
                rangeQuery(value.getSign(), searchField.concat("valueNumber"), value.getValue()));
          }
        } else {
          orStatement.should(QueryBuilders.matchQuery(searchField.concat("value"),
              value.getValue()));
        }
      }
      shouldStatement.must(orStatement);
      return QueryBuilders.nestedQuery("metadata", shouldStatement,
          ScoreMode.None);
    } else if (request.getFieldType().equals("core")) {
      CoreField coreField = coreFieldService.getCoreFieldByIdentifier(request.getFieldIdentifier());
      if (coreField.getValueType().equals(DATE)) {
        prepareDate(request);
      }
      searchField = lookupEntityType.getTableName().concat(".").concat(coreField.getField());
      for (SearchValue value : request.getValues()) {
        shouldStatement.should(QueryBuilders.matchQuery(searchField, value.getValue()));
      }
      return shouldStatement;
    } else {
      throw new ConflictException("Unexpected field type: " + request.getFieldType());
    }
  }

  private AbstractQueryBuilder<?> rangeStatement(EntityFilterRequest request)
      throws ParseException {
    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
    LookupEntityType lookupEntityType = lookupEntityTypeService.getLookUpEntityTypeById(
        request.getEntityIdentifier());
    String searchField;
    if (request.getRange().getMaxValue() != null && request.getRange().getMinValue() != null) {
      if (request.getFieldType().equals("tag")) {
        EntityTag entityTag = entityTagService.getEntityTagByIdentifier(
            request.getFieldIdentifier());
        if (entityTag.getValueType().equals(DATE)) {
          prepareDate(request);
        }

        if (lookupEntityType.getTableName().equals("location")) {
          searchField = "metadata.";
        } else {
          searchField = lookupEntityType.getTableName().concat(".metadata.");
        }
        boolQuery.must(
            QueryBuilders.matchPhraseQuery(searchField.concat("tag"), entityTag.getTag()));
        String searchFieldName = "value";
        if (entityTag.getValueType().equals(INTEGER) || entityTag.getValueType().equals(DOUBLE)) {
          searchFieldName = "valueNumber";
        }
        boolQuery.must(QueryBuilders.rangeQuery(searchField.concat(searchFieldName))
            .lte(request.getRange().getMaxValue())
            .gte(request.getRange().getMinValue()));
        return QueryBuilders.nestedQuery("metadata", boolQuery,
            ScoreMode.None);
      } else if (request.getFieldType().equals("core")) {
        CoreField coreField = coreFieldService.getCoreFieldByIdentifier(
            request.getFieldIdentifier());
        if (coreField.getValueType().equals(DATE)) {
          prepareDate(request);
        }
        searchField = lookupEntityType.getTableName().concat(".").concat(coreField.getField());
        boolQuery.must(QueryBuilders.rangeQuery(searchField)
            .lte(request.getRange().getMaxValue())
            .gte(request.getRange().getMinValue()));
      } else {
        throw new ConflictException("Unexpected field type: " + request.getFieldType());
      }
    } else {
      throw new ConflictException("Must set min and max value for range.");
    }

    return boolQuery;
  }

  private RangeQueryBuilder rangeQuery(SignEntity sign, String searchField, Object value) {
    RangeQueryBuilder rangeQuery = new RangeQueryBuilder(searchField);
    switch (sign) {
      case GT:
        rangeQuery.gt(value);
        break;
      case GTE:
        rangeQuery.gte(value);
        break;
      case LT:
        rangeQuery.lt(value);
        break;
      case LTE:
        rangeQuery.lte(value);
        break;
    }
    return rangeQuery;
  }

  private void prepareDate(EntityFilterRequest req) throws ParseException {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
    if (req.getRange() == null && req.getValues() == null) {
      req.getSearchValue()
          .setValue(formatter.parse((String) req.getSearchValue().getValue()).getTime());

    } else if (req.getSearchValue() == null && req.getValues() == null) {

    } else if (req.getSearchValue() == null && req.getRange() == null) {

    }
  }

  public PersonMainData getPersonsDetails(UUID personIdentifier) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    sourceBuilder.query(QueryBuilders.nestedQuery("person",
            QueryBuilders.termQuery("person.identifier", personIdentifier.toString()), ScoreMode.None)
        .innerHit(new InnerHitBuilder()));
    SearchRequest searchRequest = new SearchRequest(elasticIndex);
    searchRequest.source(sourceBuilder);
    SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
    if (Arrays.stream(searchResponse.getHits().getHits()).findFirst().isPresent()) {
      for (SearchHit hit : Arrays.stream(searchResponse.getHits().getHits()).findFirst().get()
          .getInnerHits().get("person")) {
        PersonElastic personElastic = mapper.readValue(hit.getSourceAsString(),
            PersonElastic.class);
        return PersonMainDataResponseFactory.fromPersonElastic(personElastic);
      }
    }
    throw new NotFoundException("Person not found");
  }
}
