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
import com.revealprecision.revealserver.api.v1.dto.request.EntityTagRequest;
import com.revealprecision.revealserver.api.v1.dto.request.SearchValue;
import com.revealprecision.revealserver.api.v1.dto.response.FeatureSetResponse;
import com.revealprecision.revealserver.api.v1.dto.response.FeatureSetResponseContainer;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.api.v1.dto.response.PersonMainData;
import com.revealprecision.revealserver.api.v1.dto.response.SimulationCountResponse;
import com.revealprecision.revealserver.constants.EntityTagDataTypes;
import com.revealprecision.revealserver.enums.SignEntity;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.CoreField;
import com.revealprecision.revealserver.persistence.domain.LookupEntityType;
import com.revealprecision.revealserver.persistence.domain.SimulationRequest;
import com.revealprecision.revealserver.persistence.es.LocationElastic;
import com.revealprecision.revealserver.persistence.es.PersonElastic;
import com.revealprecision.revealserver.persistence.projection.LocationCoordinatesProjection;
import com.revealprecision.revealserver.persistence.repository.SimulationRequestRepository;
import com.revealprecision.revealserver.props.ConditionQueryProperties;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.InnerHitBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Profile("Elastic")
@RequiredArgsConstructor
public class EntityFilterEsService {

  private final JdbcTemplate jdbcTemplate;
  private final ConditionQueryProperties conditionQueryProperties;
  private final LocationRelationshipService locationRelationshipService;
  private final LocationHierarchyService locationHierarchyService;
  private final PersonService personService;
  private final LookupEntityTypeService lookupEntityTypeService;
  private final EntityTagService entityTagService;
  private final CoreFieldService coreFieldService;
  private final RestHighLevelClient client;
  private final SimulationRequestRepository simulationRequestRepository;
  private final LocationService locationService;

  @Value("${reveal.elastic.index-name}")
  String elasticIndex;

  public Optional<SimulationRequest> getSimulationRequestById(String simulationRequestId) {
    return simulationRequestRepository.findById(UUID.fromString(simulationRequestId));
  }

  public void updateRequestWithEntityTags(String simulationRequestId,
      List<EntityTagRequest> entityTagRequests) {

    log.trace(simulationRequestId, entityTagRequests);
    if (simulationRequestId != null) {
      Optional<SimulationRequest> simulationRequestOptional = simulationRequestRepository.findById(
          UUID.fromString(simulationRequestId));
      if (simulationRequestOptional.isPresent()) {
        SimulationRequest simulationRequest = simulationRequestOptional.get();
        simulationRequest.getRequest().setResultTags(entityTagRequests);
        SimulationRequest save = simulationRequestRepository.save(simulationRequest);
        log.trace("Simulation request {}", save);
      } else {
        throw new NotFoundException(simulationRequestId + " not found");
      }
    } else {
      throw new IllegalArgumentException(simulationRequestId + " is null !");
    }
  }

  public SimulationCountResponse saveRequestAndCountResults(DataFilterRequest request) {

    List<String> geographicLevelList = new ArrayList<>();
    List<String> inactiveGeographicLevelList = new ArrayList<>();

    List<String> nodeOrder = locationHierarchyService.findByIdentifier(
            request.getHierarchyIdentifier())
        .getNodeOrder();

    if (request.getFilterGeographicLevelList() != null
        && request.getFilterGeographicLevelList().size() > 0) {
      geographicLevelList.addAll(request.getFilterGeographicLevelList());
    } else {
      geographicLevelList.addAll(nodeOrder);
    }
    if (request.getInactiveGeographicLevelList() != null
        && request.getInactiveGeographicLevelList().size() > 0) {
      inactiveGeographicLevelList.addAll(request.getInactiveGeographicLevelList());
    } else {
      inactiveGeographicLevelList.addAll(nodeOrder);
    }

    Map<String, Long> collect = geographicLevelList.stream().map(geographicLevel -> {
      try {
        List<EntityFilterRequest> personFilters = new ArrayList<>();
        List<EntityFilterRequest> locationFilters = new ArrayList<>();

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        boolQuery.must(
            QueryBuilders.matchPhraseQuery("level", geographicLevel));

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
          boolQuery.must(QueryBuilders.nestedQuery("hierarchyDetailsElastic",
              QueryBuilders.matchQuery("hierarchyDetailsElastic".concat(".")
                      .concat(request.getHierarchyIdentifier().toString()).concat(".")
                      .concat("ancestry"),
                  request.getLocationIdentifier().toString()
              ), ScoreMode.None));
        }

        List<String> strings = List.of(elasticIndex);

        String[] myArray = new String[strings.size()];
        strings.toArray(myArray);
        CountRequest countRequest = new CountRequest(myArray, boolQuery);
        CountResponse searchResponse = client.count(countRequest, RequestOptions.DEFAULT);
        return new SimpleEntry<>(geographicLevel, searchResponse.getCount());
      } catch (ParseException | IOException e) {
        return new SimpleEntry<>(geographicLevel, 0L);
      }
    }).collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));

    SimulationRequest simulationRequest = new SimulationRequest();
    simulationRequest.setRequest(request);

    SimulationRequest simulationRequestSaved = simulationRequestRepository.save(simulationRequest);
    SimulationCountResponse simulationCountResponse = new SimulationCountResponse();
    simulationCountResponse.setCountResponse(collect);

    if (request.isIncludeInactive()) {
      Map<String, Long> inactive = inactiveGeographicLevelList.stream().map(geographicLevel -> {
        try {
          BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

          boolQuery.must(
              QueryBuilders.matchPhraseQuery("level", geographicLevel));

          List<String> strings = List.of(elasticIndex);
          String[] myArray = new String[strings.size()];
          strings.toArray(myArray);
          CountRequest countRequest = new CountRequest(myArray, boolQuery);
          CountResponse searchResponse = client.count(countRequest, RequestOptions.DEFAULT);
          return new SimpleEntry<>(geographicLevel, searchResponse.getCount());
        } catch (IOException e) {
          return new SimpleEntry<>(geographicLevel, 0L);
        }
      }).collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
      simulationCountResponse.setInactiveCountResponse(inactive);
    }

    simulationCountResponse.setSearchRequestId(simulationRequestSaved.getIdentifier());

    return simulationCountResponse;
  }


  public FeatureSetResponseContainer filterEntites(DataFilterRequest request, int batchSize,
      boolean excludeFields, List<String> exclusionList)
      throws IOException, ParseException {
    FeatureSetResponse response = new FeatureSetResponse();
    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
    QueryBuilder matchAllQuery = QueryBuilders.matchAllQuery();
    List<EntityFilterRequest> personFilters = new ArrayList<>();
    List<EntityFilterRequest> locationFilters = new ArrayList<>();

    if (request.getFilterGeographicLevelList() != null) {
      BoolQueryBuilder geoBoolQuery = QueryBuilders.boolQuery();
      request.getFilterGeographicLevelList().stream().forEach(geoList ->
          geoBoolQuery.should().add(QueryBuilders.matchPhraseQuery("level", geoList)));

      boolQuery.must(geoBoolQuery);
    }

    if (request.getEntityFilters() != null) {
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
      boolQuery.must(QueryBuilders.nestedQuery("hierarchyDetailsElastic",
          QueryBuilders.matchQuery("hierarchyDetailsElastic".concat(".")
                  .concat(request.getHierarchyIdentifier().toString()).concat(".").concat("ancestry"),
              request.getLocationIdentifier().toString()
          ), ScoreMode.None));
    }

    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    sourceBuilder.size(batchSize);

    if (request.getFilterGeographicLevelList() != null || request.getLocationIdentifier() != null
        || (request.getEntityFilters() != null && request.getEntityFilters().size() > 0)) {
      sourceBuilder.query(boolQuery);
    } else {
      sourceBuilder.query(matchAllQuery);
    }


    String collect = request.getResultTags().stream()
        .map(entityFilterRequest -> "n.add('" + entityFilterRequest.getTag() + "');")
        .collect(Collectors.joining(""));
    Script inline = new Script(ScriptType.INLINE, "painless",
        "List a = params['_source']['metadata']; List n = new ArrayList(); "+collect+"  return a.stream().filter(val->n.contains(val.tag)).collect(Collectors.toList());", new HashMap<>());

    sourceBuilder.scriptField("meta",inline);

    String[] val = new String[0];

    if (excludeFields) {
      sourceBuilder.fetchSource(new ArrayList<String>().toArray(val), exclusionList.toArray(val));
    } else {
      sourceBuilder.fetchSource(new ArrayList<String>().toArray(val), new ArrayList<String>().toArray(val));
    }
    SearchRequest searchRequest = new SearchRequest(elasticIndex);

    List<SortBuilder<?>> sortBuilders = List.of(SortBuilders.fieldSort("name").order(
            SortOrder.DESC),
        SortBuilders.fieldSort("hashValue").order(
            SortOrder.DESC));


    sourceBuilder.sort(sortBuilders);

    if (request.getLastHit() != null) {
      sourceBuilder.searchAfter(request.getLastHit().getSortValues());
    }

    searchRequest.source(sourceBuilder);

    log.trace("calling search {}", searchRequest.source().toString());
    SearchResponse searchResponse = client
        .search(searchRequest, RequestOptions.DEFAULT);
    log.trace("called search");

    Set<String> parentLocations = new HashSet<>();

    SearchHit lastHit = null;

    log.trace("processing search results");

    List<LocationResponse> locationResponses = Arrays.stream(searchResponse.getHits().getHits())

        .map(hit -> {
          try {
            LocationResponse locToAdd = LocationResponseFactory.fromSearchHit(hit, parentLocations,
                request.getHierarchyIdentifier().toString());
            locToAdd.getProperties().setSimulationSearchResult(true);
            locToAdd.getProperties()
                .setLevelColor(getGeoLevelColor(locToAdd.getProperties().getGeographicLevel()));

            return locToAdd;
          } catch (JsonProcessingException e) {
            e.printStackTrace();
          }
          return null;
        }).collect(Collectors.toList());
    log.info("processed search results");

    response.setParents(parentLocations);

    locationResponses = getLocationCenters(locationResponses);
    response.setFeatures(locationResponses);

    if (searchResponse.getHits().getHits().length > 0) {
      lastHit =
          searchResponse.getHits().getHits()[searchResponse.getHits().getHits().length - 1];
    }

    response.setType("FeatureCollection");

    return new FeatureSetResponseContainer(response, lastHit);

  }

  private List<LocationResponse> getLocationCenters(List<LocationResponse> locationResponses) {
    List<UUID> collect = locationResponses.stream().map(LocationResponse::getIdentifier)
        .collect(Collectors.toList());

    Map<String, LocationCoordinatesProjection> locationCentroidCoordinatesMap = locationService.getLocationCentroidCoordinatesMap(
        collect);

    return locationResponses.stream().map(locationResponse -> {
      LocationCoordinatesProjection locationCoordinatesProjection = locationCentroidCoordinatesMap.get(
          locationResponse.getIdentifier().toString());
      if (locationCoordinatesProjection != null) {
        locationResponse.getProperties().setXCentroid(locationCoordinatesProjection.getLongitude());
        locationResponse.getProperties().setYCentroid(locationCoordinatesProjection.getLatitude());
      }

      return locationResponse;
    }).collect(Collectors.toList());
  }

  private String getGeoLevelColor(String geolevel) {

    switch (geolevel) {
      case "country":
        return "#EFEFEF";
      case "province":
        return "#CDCDCD";
      case "county":
        return "#ABABAB";
      case "subcounty":
        return "#9A9A9A";
      case "ward":
        return "#898989";
      case "catchment":
        return "#787878";
      default:
        return "#898989";
    }
  }

  public List<LocationResponse> retrieveParentLocations(Set<String> parentIds, String hierarchyId)
      throws IOException {
    List<LocationResponse> responses = new ArrayList<>();
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    sourceBuilder.query(QueryBuilders.termsQuery("_id", parentIds));
    SearchRequest searchRequest = new SearchRequest(elasticIndex);
    searchRequest.source(sourceBuilder);
    sourceBuilder.size(10000);
    SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

    ObjectMapper mapper = new ObjectMapper();
    for (SearchHit hit : searchResponse.getHits().getHits()) {
      LocationElastic parent = mapper.readValue(hit.getSourceAsString(), LocationElastic.class);
      LocationResponse locationResponse = LocationResponseFactory.fromElasticModel(parent,
          parent.getHierarchyDetailsElastic()!=null?parent.getHierarchyDetailsElastic().get(hierarchyId):null, null);
      locationResponse.getProperties()
          .setLevelColor(getGeoLevelColor(locationResponse.getProperties().getGeographicLevel()));
      responses.add(locationResponse);
    }
    return responses;
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
      if (request.getValueType().equals("date")) {
        prepareDate(request);
      }
      if (lookupEntityType.getTableName().equals("location")) {
        searchField = "metadata.";
      } else {
        searchField = lookupEntityType.getTableName().concat(".metadata.");
      }
      andStatement.must(
          QueryBuilders.matchPhraseQuery(searchField.concat("tag"), request.getTag()));

      if (request.getValueType().equals(STRING)) {
        andStatement.must(QueryBuilders.matchPhraseQuery(searchField.concat("value"),
            request.getSearchValue().getValue()));
      } else if (request.getValueType().equals(INTEGER) || request.getValueType()
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
      if (request.getValueType().equals(EntityTagDataTypes.DATE)) {
        prepareDate(request);
      }
      if (lookupEntityType.getTableName().equals("location")) {
        searchField = "metadata.";
      } else {
        searchField = lookupEntityType.getTableName().concat(".metadata.");
      }
      shouldStatement.must(
          QueryBuilders.matchPhraseQuery(searchField.concat("tag"), request.getTag()));
      BoolQueryBuilder orStatement = QueryBuilders.boolQuery();
      for (SearchValue value : request.getValues()) {
        if (request.getValueType().equals(STRING)) {
          orStatement.should(
              QueryBuilders.matchPhraseQuery(searchField.concat("value"), value.getValue()));
        } else if (request.getValueType().equals(INTEGER) || request.getValueType()
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
        if (request.getValueType().equals(DATE)) {
          prepareDate(request);
        }

        if (lookupEntityType.getTableName().equals("location")) {
          searchField = "metadata.";
        } else {
          searchField = lookupEntityType.getTableName().concat(".metadata.");
        }
        boolQuery.must(
            QueryBuilders.matchPhraseQuery(searchField.concat("tag"), request.getTag()));
        String searchFieldName = "value";
        if (request.getValueType().equals(INTEGER) || request.getValueType().equals(DOUBLE)) {
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
