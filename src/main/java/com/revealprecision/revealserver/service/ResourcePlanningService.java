package com.revealprecision.revealserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.dto.models.ColumnData;
import com.revealprecision.revealserver.api.v1.dto.request.ResourcePlanningDashboardRequest;
import com.revealprecision.revealserver.api.v1.dto.request.ResourcePlanningRequest;
import com.revealprecision.revealserver.api.v1.dto.response.FieldType;
import com.revealprecision.revealserver.api.v1.dto.response.FormulaResponse;
import com.revealprecision.revealserver.api.v1.dto.response.ResourcePlanningHistoryResponse;
import com.revealprecision.revealserver.api.v1.dto.response.SecondStepQuestionsResponse;
import com.revealprecision.revealserver.enums.InputTypeEnum;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.AgeGroup;
import com.revealprecision.revealserver.persistence.domain.CampaignDrug;
import com.revealprecision.revealserver.persistence.domain.CampaignDrug.Fields;
import com.revealprecision.revealserver.persistence.domain.CountryCampaign;
import com.revealprecision.revealserver.persistence.domain.Drug;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.ResourcePlanningHistory;
import com.revealprecision.revealserver.persistence.es.EntityMetadataElastic;
import com.revealprecision.revealserver.persistence.es.LocationElastic;
import com.revealprecision.revealserver.persistence.projection.LocationStructureCount;
import com.revealprecision.revealserver.persistence.repository.CampaignDrugRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRelationshipRepository;
import com.revealprecision.revealserver.persistence.repository.ResourcePlanningHistoryRepository;
import com.revealprecision.revealserver.service.models.LocationResourcePlanning;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResourcePlanningService {

  private final CampaignDrugRepository campaignDrugRepository;
  private final LocationHierarchyService locationHierarchyService;
  private final LocationRelationshipRepository locationRelationshipRepository;
  private final ResourcePlanningHistoryRepository resourcePlanningHistoryRepository;
  private final RestHighLevelClient client;
  private final EntityTagService entityTagService;
  private final CountryCampaign countryCampaign;

  private static String suffix = "_buffer";
  private static String bufferQuestion = "What percent of buffer stock of '%s' is planned?";
  private static String drugDosageQuestion = "What is the average number of '%s' tablets required to treat 1 person in '%s' age group? (select 0 if not eligible)";
  private static String popPercent = "What percent is '%s' age group out of the total population?";
  private static String coveragePercent = "What percent of this population do you expect to reach during the campaign?";

  public List<CountryCampaign> getCountries() {
    System.out.println("dsad");
    return List.of(countryCampaign);
  }

  public List<CampaignDrug> getCampaigns() {
    return campaignDrugRepository.findAll();
  }

  public CampaignDrug getCampaignByIdentifier(UUID identifier) {
    return campaignDrugRepository.findById(identifier).orElseThrow(() -> new NotFoundException(Pair.of(
        Fields.identifier, identifier), CampaignDrug.class));
  }

  public CountryCampaign getCountryCampaignByIdentifier(UUID identifier) {
    if(identifier.equals(countryCampaign.getIdentifier())) {
      return countryCampaign;
    }else {
      throw new NotFoundException("Country with id: " + identifier + " not found");
    }
  }

  public List<SecondStepQuestionsResponse> getSecondStepQuestions(ResourcePlanningRequest request) {
    List<SecondStepQuestionsResponse> response = new ArrayList<>();

    List<CountryCampaign> countryCampaigns = List.of(countryCampaign);
    List<CampaignDrug> campaignDrugs = campaignDrugRepository.getAllByIdentifiers(request.getCampaignIdentifiers());
    if(countryCampaigns.size() != request.getCountryIdentifiers().size() || campaignDrugs.size() != request.getCampaignIdentifiers().size()) {
      throw new ConflictException("Did not find all countries and campaigns");
    }else {
      for(CountryCampaign con : countryCampaigns) {
        SecondStepQuestionsResponse questions = new SecondStepQuestionsResponse();
        questions.setCountry(con.getName());

        int index = IntStream.range(0, con.getGroups().size())
            .filter(i -> con.getGroups().get(i).getKey().equals(request.getAgeGroupKey()))
            .findFirst()
            .orElse(-1);

        if(index != -1) {
          List<AgeGroup> targetedAgeGroups = con.getGroups().subList(index, con.getGroups().size());
          targetedAgeGroups.forEach(el -> {
            questions.getQuestions().add(new FormulaResponse(String.format(popPercent, el.getName()), el.getKey().concat("_percent").concat("_" + con.getKey()), new FieldType(
                InputTypeEnum.DECIMAL, null, 0, 100), null));
            questions.getQuestions().add(new FormulaResponse(coveragePercent, el.getKey().concat("_coverage").concat("_" + con.getKey()), new FieldType(
                InputTypeEnum.INTEGER, null, 0, 100), null));

            for(CampaignDrug campaign : campaignDrugs) {
              campaign.getDrugs().forEach(drug->{
                questions.getQuestions().add(new FormulaResponse(String.format(drugDosageQuestion, drug.getName(), el.getName()), drug.getKey() + "_" + el.getKey() + "_" + con.getKey(), new FieldType(
                    InputTypeEnum.DROPDOWN, getPossibleValues(drug), null, null), null));
              });
            }
          });
        }

        for(CampaignDrug campaign : campaignDrugs) {

          //buffer questions
          campaignDrugs.forEach(el -> {
            el.getDrugs().forEach(drug -> {
              questions.getQuestions().add(new FormulaResponse(String.format(bufferQuestion, drug.getName()), drug.getKey().concat(suffix).concat("_" + con.getKey()), new FieldType(
                  InputTypeEnum.INTEGER, null, 0, 100), null));
            });
          });
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
    if(drug.isFull()) {
      int counter = (int)drug.getMin();
      while(counter < (int)drug.getMax()) {
        counter++;
        response.add(counter);
      }
    }else if(drug.isHalf()) {
      double counter = (double)drug.getMin();
      while(counter < (double)drug.getMax()) {
        counter += 0.5;
        response.add(counter);
      }
    }
    return response;
  }

  public List<LocationResourcePlanning> getDashboardData(ResourcePlanningDashboardRequest request, boolean saveData) throws IOException {
    CampaignDrug campaign = getCampaignByIdentifier(request.getCampaign());
    LocationHierarchy locationHierarchy = locationHierarchyService.findByIdentifier(request.getLocationHierarchy());
    CountryCampaign countryCampaign = getCountryCampaignByIdentifier(request.getCountry());
    String minAgeGroup = (String) request.getStepTwoAnswers().get("ageGroup");
    List<AgeGroup> targetedAgeGroups;
    if(!locationHierarchy.getNodeOrder().contains(request.getLowestGeography())) {
      throw new ConflictException("Geography level does not exist in Location hierarch.");
    }
    List<LocationResourcePlanning> response = getDataFromElastic(request);

    int index = IntStream.range(0, countryCampaign.getGroups().size())
        .filter(i -> countryCampaign.getGroups().get(i).getKey().equals(minAgeGroup))
        .findFirst()
        .orElse(-1);

    if(index != -1) {
      targetedAgeGroups = countryCampaign.getGroups().subList(index, countryCampaign.getGroups().size());
    }else {
      throw new ConflictException("Age group does not exist");
    }

    calculateAdjustedPopulation(response, request);
    calculateAgeGroupColumns(response, targetedAgeGroups, request, countryCampaign.getKey());
    calculateTotals(response, request);
    calculateDrugs(response, request, campaign, countryCampaign, targetedAgeGroups);

    if(saveData) {
      if(request.getName().isBlank()) {
        throw new ConflictException("Name cannot be blank");
      }
      ResourcePlanningHistory resourcePlanningHistory = new ResourcePlanningHistory(request,
          request.getName());
      resourcePlanningHistoryRepository.save(resourcePlanningHistory);
    }
    return response;
  }

  private List<LocationResourcePlanning> getDataFromElastic(ResourcePlanningDashboardRequest request) throws IOException {
    List<LocationElastic> foundLocations = new ArrayList<>();
    EntityTag popTag = entityTagService.getEntityTagByIdentifier(request.getPopulationTag());
    String structureCountTag = null;
    List<LocationStructureCount> structureCounts = new ArrayList<>();
    Map<String, Long> mapStructureCount = new HashMap<>();
    if(!request.isCountBasedOnImportedLocations()) {
      structureCountTag = entityTagService.getEntityTagByIdentifier(request.getStructureCountTag()).getTag();
    }else {
      structureCounts = locationRelationshipRepository.getNumberOfStructures(request.getLocationHierarchy(), request.getLowestGeography());
      mapStructureCount = structureCounts.stream().collect(Collectors.toMap(el-> el.getIdentifier(),
          LocationStructureCount::getStructureCount));
    }

    List<LocationResourcePlanning> response = new ArrayList<>();
    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

    if(!request.getLowestGeography().equals("country")) {
      boolQuery.must(QueryBuilders.existsQuery("ancestry.".concat(request.getLocationHierarchy().toString())));
    }
    boolQuery.must(QueryBuilders.termQuery("level", request.getLowestGeography()));

    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    sourceBuilder.size(10000);
    sourceBuilder.fetchSource(new String[]{"id", "name", "metadata.tag", "metadata.valueNumber"}, null);

    sourceBuilder.query(boolQuery);
    SearchRequest searchRequest = new SearchRequest("location");
    searchRequest.source(sourceBuilder);
    SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
    ObjectMapper mapper = new ObjectMapper();
    for (SearchHit hit : searchResponse.getHits().getHits()) {
      foundLocations.add(mapper.readValue(hit.getSourceAsString(), LocationElastic.class));
    }

    for(LocationElastic loc : foundLocations) {
      Optional<EntityMetadataElastic> entityMetadataElastic = loc.getMetadata().stream()
          .filter(el -> el.getTag().equals(popTag.getTag())).findFirst();
      Object total_population = null;
      if(entityMetadataElastic.isPresent()) {
        total_population = entityMetadataElastic.get().getValueNumber();
      }

      Object total_structure = null;
      if(!request.isCountBasedOnImportedLocations()) {
        String finalStructureCountTag = structureCountTag;
        entityMetadataElastic = loc.getMetadata().stream()
            .filter(el -> el.getTag().equals(
                finalStructureCountTag)).findFirst();
        if (entityMetadataElastic.isPresent()) {
          total_structure = entityMetadataElastic.get().getValueNumber();
        }
      }else {
        total_structure = mapStructureCount.get(loc.getId());
      }

      response.add(new LocationResourcePlanning(loc, total_population, total_structure));
    }

    return response;
  }

  private void calculateAdjustedPopulation(List<LocationResourcePlanning> data, ResourcePlanningDashboardRequest request) {
    int mdaYear =Integer.parseInt((String)request.getStepOneAnswers().get("mda_year"));
    int popYear = Integer.parseInt((String)request.getStepOneAnswers().get("pop_year"));
    double popGrowth = Double.parseDouble((String)request.getStepOneAnswers().get("pop_growth"));

    data.forEach(el -> {
      double val = (double)el.getColumnDataMap().get("Official population").getValue() + ((mdaYear - popYear)*popGrowth*100);
      el.getColumnDataMap().put("Total population with growth rate applied", ColumnData.builder().isPercentage(false).dataType("double").value(val).build());
    });
  }

  private void calculateAgeGroupColumns(List<LocationResourcePlanning> data, List<AgeGroup> targetedAgeGroups, ResourcePlanningDashboardRequest request, String countryKey) {
    double total;
    for(LocationResourcePlanning el : data){
      total = 0;
      double pop_adjust = (double) el.getColumnDataMap().get("Total population with growth rate applied").getValue();
      for(AgeGroup ageGroup : targetedAgeGroups){
        double ageGroupPercent = Double.parseDouble((String) request.getStepTwoAnswers().get(ageGroup.getKey().concat("_percent_").concat(countryKey)));
        int ageGroupCoverage = Integer.parseInt((String) request.getStepTwoAnswers().get(ageGroup.getKey().concat("_coverage_").concat(countryKey)));
        double value = ageGroupPercent * pop_adjust * ageGroupCoverage;
        total += value;
        el.getColumnDataMap().put(ageGroup.getName(), ColumnData.builder().isPercentage(false).dataType("double").value(value).build());
      }
      el.getColumnDataMap().put("Total target population", ColumnData.builder().isPercentage(false).dataType("double").value(total).build());
    }
  }

  private void calculateTotals(List<LocationResourcePlanning> data, ResourcePlanningDashboardRequest request) {
    //cdd_denom yes -> cdd_total = (target_pop / mda_days)/cdd_target
    //              -> days_total = target_pop/(cdd_number*cdd_target)
    //              -> ((cdd_total*days_total*cdd_target)/target_pop)*100
    //          no  -> cdd_total = (pop_adjust / mda_days)/cdd_target
    //              -> days_total = pop_adjust/(cdd_number*cdd_target)
    //              -> ((cdd_total*days_total*cdd_target)/pop_adjust)*100
    boolean flag;
    if(request.getStepOneAnswers().containsKey("cdd_denom")) {
      if(request.getStepOneAnswers().get("cdd_denom").equals("Yes")) {
        flag = true;
      }else {
        flag = false;
      }
    }else {
      flag = false;
    }
    int mda_days;
    if(request.getStepOneAnswers().get("mda_days").equals("")) {
      mda_days = 1;
    } else {
      mda_days = Integer.parseInt((String) request.getStepOneAnswers().get("mda_days"));
    }

    int cdd_target = Integer.parseInt((String) request.getStepOneAnswers().get("cdd_target"));
    int cdd_number = Integer.parseInt((String) request.getStepOneAnswers().get("cdd_number"));
    int structure_day = Integer.parseInt((String) request.getStepOneAnswers().get("structure_day"));
    data.forEach(el -> {
      double val = 0;
      double cddResult = 0;
      double daysResult = 0;
      double campPopCove = 0;
      double totalStructure = 0;
      double cddSuper = 0;
      if (flag) {
        val = (double) el.getColumnDataMap().get("Total target population").getValue();
        cddResult = (val/mda_days)/cdd_target;
        daysResult = val/(cdd_number*cdd_target);
        el.getColumnDataMap().put("CDDs planned", ColumnData.builder().isPercentage(false).dataType("double").value(cddResult).build());
        el.getColumnDataMap().put("Days planned", ColumnData.builder().isPercentage(false).dataType("double").value(daysResult).build());
        el.getColumnDataMap().put("Anticipated campaign population coverage based on CDDs", ColumnData.builder().isPercentage(true).dataType("double").value(((cddResult*daysResult*cdd_target)/val)*100).build());
      }else {
        val = (double) el.getColumnDataMap().get("Total population with growth rate applied").getValue();
        cddResult = (val/mda_days)/cdd_target;
        daysResult = val/(cdd_number*cdd_target);
        el.getColumnDataMap().put("CDDs planned", ColumnData.builder().isPercentage(false).dataType("double").value(cddResult).build());
        el.getColumnDataMap().put("Days planned", ColumnData.builder().isPercentage(false).dataType("double").value(daysResult).build());
        el.getColumnDataMap().put("Anticipated campaign population coverage based on CDDs", ColumnData.builder().isPercentage(true).dataType("double").value(((cddResult*daysResult*cdd_target)/val)*100).build());
      }
      campPopCove = daysResult*structure_day*cddResult;
      if(request.isCountBasedOnImportedLocations()) {
        long structureCountValue = (long) el.getColumnDataMap().get("Number of structures in the campaign location").getValue();
        totalStructure = (double) structureCountValue;
      }else {
        totalStructure = (double) el.getColumnDataMap().get("Number of structures in the campaign location").getValue();
      }
      cddSuper = Double.parseDouble((String) request.getStepOneAnswers().get("cdd_super"));
      el.getColumnDataMap().put("Anticipated number of structures that can be visited", ColumnData.builder().isPercentage(true).dataType("double").value(campPopCove).build());
      el.getColumnDataMap().put("Anticipated campaign coverage of structures based on number of structures in the location", ColumnData.builder().isPercentage(true).dataType("double").value(campPopCove/totalStructure).build());
      el.getColumnDataMap().put("CDD Supervisors planned", ColumnData.builder().isPercentage(true).dataType("double").value(cddResult/cddSuper).build());
    });
  }

  private void calculateDrugs(List<LocationResourcePlanning> data, ResourcePlanningDashboardRequest request, CampaignDrug campaignDrug, CountryCampaign country, List<AgeGroup> targetedAgeGroups) {
    for(LocationResourcePlanning row : data) {
      double bufferVal;
      for(Drug drug : campaignDrug.getDrugs()) {
        double total = 0;
        double drugVal;
        bufferVal = Double.parseDouble((String) request.getStepTwoAnswers().get(drug.getKey() + "_buffer_" + country.getKey()));
        for(AgeGroup ageGroup : targetedAgeGroups) {

          String firstValue = drug.getKey() + "_" + ageGroup.getKey() + "_" + country.getKey();
          drugVal = Double.parseDouble((String) request.getStepTwoAnswers().get(firstValue));

          total += drugVal*(double) row.getColumnDataMap().get(ageGroup.getName()).getValue();
        }
        row.getColumnDataMap().put(drug.getName(), ColumnData.builder().isPercentage(true).dataType("double").value(total).build());
        row.getColumnDataMap().put("Buffer stock of " + drug.getName(), ColumnData.builder().isPercentage(true).dataType("double").value(total * bufferVal).build());
      }
    }
  }

  public Page<ResourcePlanningHistoryResponse> getHistory(Pageable pageable) {
    return resourcePlanningHistoryRepository.getHistory(pageable);
  }

  public  ResourcePlanningDashboardRequest getHistoryByIdentifier(UUID identifier) {
    ResourcePlanningHistory history = resourcePlanningHistoryRepository.findById(identifier).orElseThrow(() -> new NotFoundException(Pair.of(
        Fields.identifier, identifier), ResourcePlanningHistory.class));
    return history.getHistory();
  }
}