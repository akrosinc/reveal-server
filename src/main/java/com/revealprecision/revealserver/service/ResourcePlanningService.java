package com.revealprecision.revealserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.dto.models.ColumnData;
import com.revealprecision.revealserver.api.v1.dto.request.EntityTagItem;
import com.revealprecision.revealserver.api.v1.dto.request.EntityTagRequest;
import com.revealprecision.revealserver.api.v1.dto.request.ResourcePlanningDashboardRequest;
import com.revealprecision.revealserver.api.v1.dto.request.ResourcePlanningRequest;
import com.revealprecision.revealserver.api.v1.dto.response.FieldType;
import com.revealprecision.revealserver.api.v1.dto.response.FormulaResponse;
import com.revealprecision.revealserver.api.v1.dto.response.ResourcePlanningHistoryResponse;
import com.revealprecision.revealserver.api.v1.dto.response.SecondStepQuestionsResponse;
import com.revealprecision.revealserver.constants.EntityTagDataAggregationMethods;
import com.revealprecision.revealserver.constants.EntityTagDataTypes;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.enums.InputTypeEnum;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.messaging.message.LocationIdEvent;
import com.revealprecision.revealserver.model.GenericHierarchy;
import com.revealprecision.revealserver.persistence.domain.AgeGroup;
import com.revealprecision.revealserver.persistence.domain.CampaignDrug;
import com.revealprecision.revealserver.persistence.domain.CampaignDrug.Fields;
import com.revealprecision.revealserver.persistence.domain.Drug;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.ResourcePlanningHistory;
import com.revealprecision.revealserver.persistence.domain.aggregation.ResourceAggregationNumeric;
import com.revealprecision.revealserver.persistence.es.EntityMetadataElastic;
import com.revealprecision.revealserver.persistence.es.LocationElastic;
import com.revealprecision.revealserver.persistence.projection.LocationStructureCount;
import com.revealprecision.revealserver.persistence.repository.CampaignDrugRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRelationshipRepository;
import com.revealprecision.revealserver.persistence.repository.ResourceAggregationNumericRepository;
import com.revealprecision.revealserver.persistence.repository.ResourcePlanningHistoryRepository;
import com.revealprecision.revealserver.props.CountryCampaign;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.models.LocationResourcePlanning;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("Elastic")
public class ResourcePlanningService {


  public static final String TOTAL_POPULATION_WITH_GROWTH_RATE_APPLIED = "Total population with growth rate applied";
  public static final String TOTAL_TARGET_POPULATION = "Total target population";
  public static final String CDDS_PLANNED = "CDDs planned";
  public static final String DAYS_PLANNED = "Days planned";
  public static final String ANTICIPATED_CAMPAIGN_POPULATION_COVERAGE_BASED_ON_CDDS = "Anticipated campaign population coverage based on CDDs";
  public static final String NUMBER_OF_STRUCTURES_IN_THE_CAMPAIGN_LOCATION = "Number of structures in the campaign location";
  public static final String ANTICIPATED_NUMBER_OF_STRUCTURES_THAT_CAN_BE_VISITED = "Anticipated number of structures that can be visited";
  public static final String ANTICIPATED_CAMPAIGN_COVERAGE_OF_STRUCTURES_BASED_ON_NUMBER_OF_STRUCTURES_IN_THE_LOCATION = "Anticipated campaign coverage of structures based on number of structures in the location";
  public static final String CDD_SUPERVISORS_PLANNED = "CDD Supervisors planned";
  public static final String AGE_GROUP = "Age Group";
  public static final String DRUG = "Drug";
  public static final String DRUG_BUFFER = "Drug Buffer";
  private static final Map<String, String> columnKeyMap = new HashMap<>();

  static {
    columnKeyMap.put(TOTAL_POPULATION_WITH_GROWTH_RATE_APPLIED, "population-with-growth");
    columnKeyMap.put(TOTAL_TARGET_POPULATION, "target-population");
    columnKeyMap.put(CDDS_PLANNED, "cdds-planned");
    columnKeyMap.put(DAYS_PLANNED, "days-planned");
    columnKeyMap.put(ANTICIPATED_CAMPAIGN_POPULATION_COVERAGE_BASED_ON_CDDS,
        "anticipated-population-coverage-based-cdds");
    columnKeyMap.put(ANTICIPATED_NUMBER_OF_STRUCTURES_THAT_CAN_BE_VISITED,
        "anticipated-structures");
    columnKeyMap.put(
        ANTICIPATED_CAMPAIGN_COVERAGE_OF_STRUCTURES_BASED_ON_NUMBER_OF_STRUCTURES_IN_THE_LOCATION,
        "anticipated-structures-based-on-structures");
    columnKeyMap.put(CDD_SUPERVISORS_PLANNED, "cdd-supervisors-planned");
    columnKeyMap.put(AGE_GROUP, "agegroup");
    columnKeyMap.put(DRUG, "drug");
    columnKeyMap.put(DRUG_BUFFER, "drugbuffer");
  }

  private final CampaignDrugRepository campaignDrugRepository;
  private final LocationRelationshipRepository locationRelationshipRepository;
  private final ResourcePlanningHistoryRepository resourcePlanningHistoryRepository;
  private final RestHighLevelClient client;
  private final EntityTagService entityTagService;
  private final CountryCampaign countryCampaign;
  private final PublisherService publisherService;
  private final KafkaProperties kafkaProperties;
  private final ResourceAggregationNumericRepository resourceAggregationNumericRepository;

  @Value("${reveal.elastic.index-name}")
  String elasticIndex;

  private static final String suffix = "_buffer";
  private static final String bufferQuestion = "What percent of buffer stock of '%s' is planned?";
  private static final String drugDosageQuestionTablets = "What is the average number of '%s' tablets required to treat 1 person in '%s' age group? (select 0 if not eligible)";
  private static final String drugDosageQuestionMillis = "What is the average number of '%s' millilitres required to treat 1 person in '%s' age group? (select 0 if not eligible)";
  private static final String popPercent = "What percent is '%s' age group out of the total population (in percentage)?";
  private static final String coveragePercent = "What percent of this population do you expect to reach during the campaign (in percentage)?";

  public List<CountryCampaign> getCountries() {
    return List.of(countryCampaign);
  }

  public List<CampaignDrug> getCampaigns() {
    return campaignDrugRepository.findAll();
  }

  public CampaignDrug getCampaignByIdentifier(UUID identifier) {
    return campaignDrugRepository.findById(identifier)
        .orElseThrow(() -> new NotFoundException(Pair.of(
            Fields.identifier, identifier), CampaignDrug.class));
  }

  public CountryCampaign getCountryCampaignByIdentifier(UUID identifier) {
    if (identifier.equals(countryCampaign.getIdentifier())) {
      return countryCampaign;
    } else {
      throw new NotFoundException("Country with id: " + identifier + " not found");
    }
  }

  public List<SecondStepQuestionsResponse> getSecondStepQuestions(ResourcePlanningRequest request) {
    List<SecondStepQuestionsResponse> response = new ArrayList<>();

    List<CountryCampaign> countryCampaigns = List.of(countryCampaign);
    List<CampaignDrug> campaignDrugs = campaignDrugRepository.getAllByIdentifiers(
        request.getCampaignIdentifiers());
    if (countryCampaigns.size() != request.getCountryIdentifiers().size()
        || campaignDrugs.size() != request.getCampaignIdentifiers().size()) {
      throw new ConflictException("Did not find all countries and campaigns");
    } else {
      for (CountryCampaign con : countryCampaigns) {
        SecondStepQuestionsResponse questions = new SecondStepQuestionsResponse();
        questions.setCountry(con.getName());

        int index = IntStream.range(0, con.getGroups().size())
            .filter(i -> con.getGroups().get(i).getKey().equals(request.getAgeGroupKey()))
            .findFirst()
            .orElse(-1);

        if (index != -1) {
          List<AgeGroup> targetedAgeGroups = con.getGroups()
              .subList(index, con.getGroups().size());
          targetedAgeGroups.forEach(el -> {
            questions.getQuestions().add(
                new FormulaResponse(String.format(popPercent, el.getName()),
                    el.getKey().concat("_percent").concat("_" + con.getKey()), new FieldType(
                    InputTypeEnum.DECIMAL, null, 0, 100), null));
            questions.getQuestions().add(new FormulaResponse(coveragePercent,
                el.getKey().concat("_coverage").concat("_" + con.getKey()), new FieldType(
                InputTypeEnum.INTEGER, null, 0, 100), null));

            for (CampaignDrug campaign : campaignDrugs) {
              campaign.getDrugs().forEach(drug ->
                  questions.getQuestions().add(new FormulaResponse(String.format(
                      drug.isMillis() ? drugDosageQuestionMillis : drugDosageQuestionTablets,
                      drug.getName(), el.getName()),
                      drug.getKey() + "_" + el.getKey() + "_" + con.getKey(), new FieldType(
                      InputTypeEnum.DROPDOWN, getPossibleValues(drug), null, null), null))
              );
            }
          });
        }

        for (CampaignDrug campaign : campaignDrugs) {

          //buffer questions
          campaignDrugs.forEach(el ->
              el.getDrugs().forEach(drug ->
                  questions.getQuestions().add(
                      new FormulaResponse(String.format(bufferQuestion, drug.getName()),
                          drug.getKey().concat(suffix).concat("_" + con.getKey()), new FieldType(
                          InputTypeEnum.INTEGER, null, 0, 100), null))
              ));
        }
        response.add(questions);
      }

    }
    return response;
  }

  private List<Object> getPossibleValues(Drug drug) {
    List<Object> response = new ArrayList<>();
    response.add(0);
    response.add(drug.getMin());
    if (drug.isFull()) {
      int counter = (int) drug.getMin();
      while (counter < (int) drug.getMax()) {
        counter++;
        response.add(counter);
      }
    } else if (drug.isHalf()) {
      double counter = (double) drug.getMin();
      while (counter < (double) drug.getMax()) {
        counter += 0.5;
        response.add(counter);
      }
    } else if (drug.isMillis()) {
      double counter = (double) drug.getMin();
      while (counter < (double) drug.getMax()) {
        counter += 0.1;
        response.add(new BigDecimal(Double.toString(counter)).setScale(2, RoundingMode.HALF_UP)
            .doubleValue());
      }
    }
    return response;
  }

  public void submitDashboard(ResourcePlanningDashboardRequest request,
      boolean saveData) throws IOException {
    List<LocationResourcePlanning> dashboardData = getDashboardData(request,
        saveData);

    Set<String> collect = dashboardData.stream().flatMap(locationResourcePlanning ->
        locationResourcePlanning.getColumnDataMap().values().stream()
            .filter(columnData -> columnData.getKey() != null)
            .map(columnData -> request.getName()
                .concat(EntityTagDataAggregationMethods.DELIMITER)
                .concat(columnData.getKey()))
    ).collect(Collectors.toSet());

    EntityTagRequest entityTagRequest = EntityTagRequest.builder()
        .tags(collect.stream().map(EntityTagItem::new).collect(Collectors.toList()))
        .aggregationMethod(EntityTagService.aggregationMethods.get(
            EntityTagDataTypes.DOUBLE))
        .isAggregate(false)
        .valueType(EntityTagDataTypes.DOUBLE)
        .build();

    entityTagService.createEntityTagsSkipExisting(
        entityTagRequest, true);

    List<String> locationIdentifiers = dashboardData.stream()
        .flatMap(dashboardRow -> {
              List<String> ancestry = dashboardRow.getAncestry();
              ancestry.add(dashboardRow.getIdentifier().toString());
              return ancestry.stream();
            }
        ).collect(
            Collectors.toList());

    List<ResourceAggregationNumeric> existingResourceMetadatas = resourceAggregationNumericRepository.findResourceAggregationNumericByHierarchyIdentifierAndAncestorInAndResourcePlanName(
        request.getLocationHierarchy().getIdentifier(),
        locationIdentifiers,
        request.getName()
    );

    Map<String, Map<String, List<ResourceAggregationNumeric>>> metaByLocationIdAndTag = existingResourceMetadatas.stream()
        .collect(Collectors.groupingBy(ResourceAggregationNumeric::getAncestor,
            Collectors.groupingBy(ResourceAggregationNumeric::getFieldCode)));

    List<ResourceAggregationNumeric> savedMetaByAncestor = resourceAggregationNumericRepository.saveAll(
        dashboardData.stream().flatMap(dashboardRow ->
            {
              List<String> ancestry = dashboardRow.getAncestry();
              if (!ancestry.contains(dashboardRow.getIdentifier().toString())) {
                ancestry.add(dashboardRow.getIdentifier().toString());
              }

              return ancestry
                  .stream().flatMap(ancestor -> {
                        Map<String, List<ResourceAggregationNumeric>> metaByTag = metaByLocationIdAndTag.get(
                            ancestor);

                        return dashboardRow.getColumnDataMap().values().stream()
                            .filter(column -> column.getKey() != null)
                            .map(column -> {

                              if (metaByTag != null && metaByTag.containsKey(column.getKey())) {
                                List<ResourceAggregationNumeric> resourceAggregationNumerics = metaByTag.get(
                                    column.getKey());
                                Optional<ResourceAggregationNumeric> first = resourceAggregationNumerics.stream()
                                    .findFirst();

                                if (first.isPresent()) {
                                  ResourceAggregationNumeric numeric = first.get();
                                  numeric.setVal((column.getValue() instanceof Integer
                                      ? (Integer) column.getValue() : (double) column.getValue()));
                                  return numeric;
                                }
                              }

                              return ResourceAggregationNumeric.builder()
                                  .ancestor(ancestor)
                                  .resourcePlanName(request.getName())
                                  .fieldCode(column.getKey())
                                  .val((column.getValue() instanceof Integer
                                      ? (Integer) column.getValue() : (double) column.getValue()))
                                  .hierarchyIdentifier(request.getLocationHierarchy().getIdentifier())
                                  .locationName(dashboardRow.getName())
                                  .build();
                            });
                      }
                  );
            }
        ).collect(Collectors.toList()));

    submitToMessaging(savedMetaByAncestor);

  }

  public void submitToMessaging(List<ResourceAggregationNumeric> savedMetaByAncestor) {
    savedMetaByAncestor
        .forEach(ancestor -> publisherService.send(
            kafkaProperties.getTopicMap().get(KafkaConstants.AGGREGATION_STAGING),
            LocationIdEvent.builder()
                .hierarchyIdentifier(ancestor.getHierarchyIdentifier())
                .nodeOrder(null)
                .uuids(List.of(UUID.fromString(ancestor.getAncestor())))
                .build()));
  }

  public List<LocationResourcePlanning> getDashboardData(ResourcePlanningDashboardRequest request,
      boolean saveData) throws IOException {
    CampaignDrug campaign = getCampaignByIdentifier(request.getCampaign());
    GenericHierarchy locationHierarchy = GenericHierarchy.builder()
        .name(request.getLocationHierarchy().getName())
        .identifier(request.getLocationHierarchy().getIdentifier())
        .nodeOrder(request.getLocationHierarchy().getNodeOrder())
        .build();
    CountryCampaign countryCampaign = getCountryCampaignByIdentifier(request.getCountry());
    String minAgeGroup = (String) request.getStepTwoAnswers().get("ageGroup");
    List<AgeGroup> targetedAgeGroups;
    if (request.getLowestGeography() != null) {
      if (!locationHierarchy.getNodeOrder().contains(request.getLowestGeography())) {
        throw new ConflictException("Geography level does not exist in Location hierarch.");
      }
    }
    List<LocationResourcePlanning> response = getDataFromElastic(request);

    int index = IntStream.range(0, countryCampaign.getGroups().size())
        .filter(i -> countryCampaign.getGroups().get(i).getKey().equals(minAgeGroup))
        .findFirst()
        .orElse(-1);

    if (index != -1) {
      targetedAgeGroups = countryCampaign.getGroups()
          .subList(index, countryCampaign.getGroups().size());
    } else {
      throw new ConflictException("Age group does not exist");
    }

    calculateAdjustedPopulation(response, request);
    calculateAgeGroupColumns(response, targetedAgeGroups, request, countryCampaign.getKey());
    calculateTotals(response, request);
    calculateDrugs(response, request, campaign, countryCampaign, targetedAgeGroups);

    roundDoubleValues(response);

    if (saveData) {
      if (request.getName().isBlank()) {
        throw new ConflictException("Name cannot be blank");
      }
      int count = resourcePlanningHistoryRepository.countByBaseName(request.getBaseName());
      if (count > 0) {
        if (request.getBaseName() == null) {
          request.setBaseName(request.getName());
        }
        count++;
        request.setName(request.getBaseName().concat("-".concat(String.valueOf(count))));
      }

      ResourcePlanningHistory resourcePlanningHistory = new ResourcePlanningHistory(request,
          request.getName(), request.getBaseName());
      resourcePlanningHistoryRepository.save(resourcePlanningHistory);
    }
    return response;
  }

  private void roundDoubleValues(List<LocationResourcePlanning> response) {

    for (LocationResourcePlanning locationResourcePlanning : response) {
      for (String j : locationResourcePlanning.getColumnDataMap().keySet()) {
        if (locationResourcePlanning.getColumnDataMap().get(j).getDataType().equals("double")) {
          log.debug("{} - {}", j, locationResourcePlanning.getColumnDataMap().get(j).getValue());
          locationResourcePlanning.getColumnDataMap().get(j)
              .setValue(new BigDecimal(Double.toString(
                  ((double) locationResourcePlanning.getColumnDataMap().get(j)
                      .getValue()))).setScale(0,
                      RoundingMode.HALF_UP)
                  .doubleValue());
        }
      }
    }
  }

  private List<LocationResourcePlanning> getDataFromElastic(
      ResourcePlanningDashboardRequest request) throws IOException {
    List<LocationElastic> foundLocations = new ArrayList<>();
    EntityTag popTag = entityTagService.getEntityTagByIdentifier(request.getPopulationTag());
    String structureCountTag = null;
    List<LocationStructureCount> structureCounts;
    Map<String, Long> mapStructureCount = new HashMap<>();
    if (!request.isCountBasedOnImportedLocations()) {
      structureCountTag = entityTagService.getEntityTagByIdentifier(
              request.getStructureCountTag())
          .getTag();
    } else {
      structureCounts = locationRelationshipRepository.getNumberOfStructures(
          UUID.fromString(request.getLocationHierarchy().getIdentifier()),
          request.getLowestGeography());
      mapStructureCount = structureCounts.stream()
          .collect(Collectors.toMap(LocationStructureCount::getIdentifier,
              LocationStructureCount::getStructureCount));
    }

    List<LocationResourcePlanning> response = new ArrayList<>();
    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

    if (request.getLowestGeography() != null) {
      boolQuery.must(QueryBuilders.termQuery("level", request.getLowestGeography()));
    }

    ExistsQueryBuilder existsQueryBuilder = QueryBuilders.existsQuery(
        "hierarchyDetailsElastic." + request.getLocationHierarchy().getIdentifier());
    NestedQueryBuilder nestedQueryBuilder = QueryBuilders.nestedQuery(
        "hierarchyDetailsElastic", existsQueryBuilder, ScoreMode.None);

    boolQuery.must(nestedQueryBuilder);

    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    sourceBuilder.size(10000);
    sourceBuilder.fetchSource(
        new String[]{"id", "name", "level", "metadata.tag", "metadata.valueNumber",
            "metadata.hierarchyIdentifier", "hierarchyDetailsElastic"},
        null);

    sourceBuilder.query(boolQuery);
    SearchRequest searchRequest = new SearchRequest(elasticIndex);
    searchRequest.source(sourceBuilder);
    SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
    ObjectMapper mapper = new ObjectMapper();
    for (SearchHit hit : searchResponse.getHits().getHits()) {
      foundLocations.add(mapper.readValue(hit.getSourceAsString(), LocationElastic.class));
    }

    for (LocationElastic loc : foundLocations) {
      Optional<EntityMetadataElastic> entityMetadataElastic = loc.getMetadata().stream()
          .filter(el -> el.getTag().equals(popTag.getTag()) && el.getHierarchyIdentifier()
              .equals(request.getLocationHierarchy().getIdentifier())).findFirst();
      Object total_population = null;
      if (entityMetadataElastic.isPresent()) {
        total_population = entityMetadataElastic.get().getValueNumber();
      }

      Object total_structure = null;
      if (!request.isCountBasedOnImportedLocations()) {
        String finalStructureCountTag = structureCountTag;
        entityMetadataElastic = loc.getMetadata().stream()
            .filter(el -> el.getTag().equals(
                finalStructureCountTag) && el.getHierarchyIdentifier()
                .equals(request.getLocationHierarchy().getIdentifier())).findFirst();
        if (entityMetadataElastic.isPresent()) {
          total_structure = entityMetadataElastic.get().getValueNumber();
        }
      } else {
        total_structure = mapStructureCount.get(loc.getId());
      }

      response.add(new LocationResourcePlanning(loc, total_population, total_structure,
          loc.getHierarchyDetailsElastic() != null && loc.getHierarchyDetailsElastic()
              .get(request.getLocationHierarchy().getIdentifier()) != null
              ? loc.getHierarchyDetailsElastic()
              .get(request.getLocationHierarchy().getIdentifier()).getAncestry() : null));
    }

    return response;
  }

  private void calculateAdjustedPopulation(List<LocationResourcePlanning> data,
      ResourcePlanningDashboardRequest request) {
    int mdaYear = Integer.parseInt((String) request.getStepOneAnswers().get("mda_year"));
    int popYear = Integer.parseInt((String) request.getStepOneAnswers().get("pop_year"));
    double popGrowth = Double.parseDouble((String) request.getStepOneAnswers().get("pop_growth"));

    data.forEach(el -> {
      double official_population = (double) el.getColumnDataMap().get("Official population")
          .getValue();

      // 10000 * ((100 + 10) / 100 )^(2023 - 2022)
      // 10000 * (1.1)^(1) = 11000
      double val = official_population * (
          Math.pow(((100 + popGrowth) / 100), (mdaYear - popYear)));

      el.getColumnDataMap().put(TOTAL_POPULATION_WITH_GROWTH_RATE_APPLIED,
          ColumnData.builder().isPercentage(false).dataType("double")
              .key(columnKeyMap.get(TOTAL_POPULATION_WITH_GROWTH_RATE_APPLIED)).value(val)
              .build());
    });
  }

  private void calculateAgeGroupColumns(List<LocationResourcePlanning> data,
      List<AgeGroup> targetedAgeGroups, ResourcePlanningDashboardRequest request,
      String countryKey) {
    double total;
    for (LocationResourcePlanning el : data) {
      total = 0;
      double pop_adjust = (double) el.getColumnDataMap()
          .get(TOTAL_POPULATION_WITH_GROWTH_RATE_APPLIED).getValue();
      for (AgeGroup ageGroup : targetedAgeGroups) {
        double ageGroupPercent = Double.parseDouble((String) request.getStepTwoAnswers()
            .get(ageGroup.getKey().concat("_percent_").concat(countryKey)));
        int ageGroupCoverage = Integer.parseInt((String) request.getStepTwoAnswers()
            .get(ageGroup.getKey().concat("_coverage_").concat(countryKey)));
        double value = ageGroupPercent * pop_adjust * ageGroupCoverage / 10000;
        total += value;
        el.getColumnDataMap().put(ageGroup.getName(),
            ColumnData.builder()
                .key(columnKeyMap.get(AGE_GROUP).concat("-").concat(ageGroup.getKey()))
                .isPercentage(false).dataType("double").value(value).build());
      }
      el.getColumnDataMap().put(TOTAL_TARGET_POPULATION,
          ColumnData.builder().isPercentage(false).key(columnKeyMap.get(TOTAL_TARGET_POPULATION))
              .dataType("double").value(total).build());
    }
  }

  private void calculateTotals(List<LocationResourcePlanning> data,
      ResourcePlanningDashboardRequest request) {
    //cdd_denom yes -> cdd_total = (target_pop / mda_days)/cdd_target
    //              -> days_total = target_pop/(cdd_number*cdd_target)
    //              -> ((cdd_total*days_total*cdd_target)/target_pop)*100
    //          no  -> cdd_total = (pop_adjust / mda_days)/cdd_target
    //              -> days_total = pop_adjust/(cdd_number*cdd_target)
    //              -> ((cdd_total*days_total*cdd_target)/pop_adjust)*100
    boolean isCddDenomYes;
    if (request.getStepOneAnswers().containsKey("cdd_denom")) {
      isCddDenomYes = request.getStepOneAnswers().get("cdd_denom").equals("Yes");
    } else {
      isCddDenomYes = false;
    }

    boolean isCddNumberFixedVar = request.getStepOneAnswers().get("choice_cdd").equals("Yes");
    boolean isMdaDaysFixedVar = request.getStepOneAnswers().get("choice_days").equals("Yes");

    int cdd_target = Integer.parseInt((String) request.getStepOneAnswers().get("cdd_target"));

    int mda_days;
    if (request.getStepOneAnswers().get("mda_days").equals("")) {
      mda_days = 1;
    } else {
      mda_days = Integer.parseInt((String) request.getStepOneAnswers().get("mda_days"));
    }

    int cdd_numberVar;
    if (request.getStepOneAnswers().get("cdd_number").equals("")) {
      cdd_numberVar = 1;
    } else {
      cdd_numberVar = Integer.parseInt((String) request.getStepOneAnswers().get("cdd_number"));
    }

    int structure_day = Integer.parseInt(
        (String) request.getStepOneAnswers().get("structure_day"));
    int cdd_number = cdd_numberVar;
    data.forEach(el -> {
      double target_pop;
      double cdd_total;
      double days_total;
      double campPopCove;
      double totalStructure;
      double cddSuper;
      if (isCddDenomYes) {
        target_pop = (double) el.getColumnDataMap().get(TOTAL_TARGET_POPULATION).getValue();
        cdd_total = isCddNumberFixedVar ? cdd_number : (target_pop / mda_days) / cdd_target;
        days_total = isMdaDaysFixedVar ? mda_days : target_pop / (cdd_number * cdd_target);
        el.getColumnDataMap().put(CDDS_PLANNED,
            ColumnData.builder().isPercentage(false).key(columnKeyMap.get(CDDS_PLANNED))
                .dataType("double").value(cdd_total).build());
        el.getColumnDataMap().put(DAYS_PLANNED,
            ColumnData.builder().isPercentage(false).key(columnKeyMap.get(DAYS_PLANNED))
                .dataType("double").value(days_total).build());
        el.getColumnDataMap().put(ANTICIPATED_CAMPAIGN_POPULATION_COVERAGE_BASED_ON_CDDS,
            ColumnData.builder()
                .key(columnKeyMap.get(ANTICIPATED_CAMPAIGN_POPULATION_COVERAGE_BASED_ON_CDDS))
                .isPercentage(true).dataType("double")
                .value(
                    target_pop > 0 ? ((cdd_total * days_total * cdd_target) / target_pop) * 100
                        : 0)
                .build());
      } else {
        target_pop = (double) el.getColumnDataMap().get(TOTAL_POPULATION_WITH_GROWTH_RATE_APPLIED)
            .getValue();
        cdd_total = isCddNumberFixedVar ? cdd_number : (target_pop / mda_days) / cdd_target;
        days_total = isMdaDaysFixedVar ? mda_days : target_pop / (cdd_number * cdd_target);
        el.getColumnDataMap().put(CDDS_PLANNED,
            ColumnData.builder().isPercentage(false).key(columnKeyMap.get(CDDS_PLANNED))
                .dataType("double").value(cdd_total).build());
        el.getColumnDataMap().put(DAYS_PLANNED,
            ColumnData.builder().isPercentage(false).key(columnKeyMap.get(DAYS_PLANNED))
                .dataType("double").value(days_total).build());
        log.trace("cddResult {} - daysResult {} - cdd_target {} - val {}", cdd_total, days_total,
            cdd_target, target_pop);
        el.getColumnDataMap().put(ANTICIPATED_CAMPAIGN_POPULATION_COVERAGE_BASED_ON_CDDS,
            ColumnData.builder()
                .key(columnKeyMap.get(ANTICIPATED_CAMPAIGN_POPULATION_COVERAGE_BASED_ON_CDDS))
                .isPercentage(true).dataType("double")
                .value(
                    target_pop > 0 ? ((cdd_total * days_total * cdd_target) / target_pop) * 100
                        : 0)
                .build());
      }
      campPopCove = days_total * structure_day * cdd_total;
      if (request.isCountBasedOnImportedLocations()) {
        long structureCountValue = (long) el.getColumnDataMap()
            .get(NUMBER_OF_STRUCTURES_IN_THE_CAMPAIGN_LOCATION).getValue();
        totalStructure = (double) structureCountValue;
      } else {
        totalStructure = (double) el.getColumnDataMap()
            .get(NUMBER_OF_STRUCTURES_IN_THE_CAMPAIGN_LOCATION).getValue();
      }
      cddSuper = Double.parseDouble((String) request.getStepOneAnswers().get("cdd_super"));
      el.getColumnDataMap().put(ANTICIPATED_NUMBER_OF_STRUCTURES_THAT_CAN_BE_VISITED,
          ColumnData.builder().isPercentage(true)
              .key(columnKeyMap.get(ANTICIPATED_NUMBER_OF_STRUCTURES_THAT_CAN_BE_VISITED))
              .dataType("double").value(campPopCove).build());
      el.getColumnDataMap().put(
          ANTICIPATED_CAMPAIGN_COVERAGE_OF_STRUCTURES_BASED_ON_NUMBER_OF_STRUCTURES_IN_THE_LOCATION,
          ColumnData.builder()
              .key(columnKeyMap.get(
                  ANTICIPATED_CAMPAIGN_COVERAGE_OF_STRUCTURES_BASED_ON_NUMBER_OF_STRUCTURES_IN_THE_LOCATION))
              .isPercentage(true).dataType("double")
              .value(totalStructure > 0 ? campPopCove / totalStructure : 0).build());
      el.getColumnDataMap().put(CDD_SUPERVISORS_PLANNED,
          ColumnData.builder()
              .key(columnKeyMap.get(CDD_SUPERVISORS_PLANNED))
              .isPercentage(true).dataType("double").value(cdd_total / cddSuper)
              .build());
    });
  }

  private void calculateDrugs(List<LocationResourcePlanning> data,
      ResourcePlanningDashboardRequest request, CampaignDrug campaignDrug, CountryCampaign
      country,
      List<AgeGroup> targetedAgeGroups) {
    for (LocationResourcePlanning row : data) {
      double bufferVal;
      for (Drug drug : campaignDrug.getDrugs()) {
        double total = 0;
        double drugVal;
        bufferVal = Double.parseDouble((String) request.getStepTwoAnswers()
            .get(drug.getKey() + "_buffer_" + country.getKey()));
        for (AgeGroup ageGroup : targetedAgeGroups) {

          String firstValue = drug.getKey() + "_" + ageGroup.getKey() + "_" + country.getKey();
          drugVal = Double.parseDouble((String) request.getStepTwoAnswers().get(firstValue));

          total += drugVal * (double) row.getColumnDataMap().get(ageGroup.getName()).getValue();
        }
        row.getColumnDataMap().put(drug.getName(),
            ColumnData.builder()
                .key(columnKeyMap.get(DRUG).concat("-").concat(drug.getKey()))
                .isPercentage(true).dataType("double").value(total).build());

        row.getColumnDataMap().put("Buffer stock of " + drug.getName(),
            ColumnData.builder().isPercentage(true).dataType("double")
                .key(columnKeyMap.get(DRUG_BUFFER).concat("-").concat(drug.getKey()))
                .value(total * bufferVal / 100)
                .build());
      }
    }
  }

  public Page<ResourcePlanningHistoryResponse> getHistory(Pageable pageable) {
    return resourcePlanningHistoryRepository.getHistory(pageable);
  }

  public ResourcePlanningDashboardRequest getHistoryByIdentifier(UUID identifier) {
    ResourcePlanningHistory history = resourcePlanningHistoryRepository.findById(identifier)
        .orElseThrow(() -> new NotFoundException(Pair.of(
            Fields.identifier, identifier), ResourcePlanningHistory.class));
    int count = resourcePlanningHistoryRepository.countByBaseName(history.getBaseName());
    count++;
    history.getHistory().setName(history.getBaseName().concat("-".concat(String.valueOf(count))));
    return history.getHistory();
  }
}