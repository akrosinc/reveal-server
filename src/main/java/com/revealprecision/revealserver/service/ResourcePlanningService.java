package com.revealprecision.revealserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.dto.models.ColumnData;
import com.revealprecision.revealserver.api.v1.dto.request.ResourcePlanningDashboardRequest;
import com.revealprecision.revealserver.api.v1.dto.request.ResourcePlanningRequest;
import com.revealprecision.revealserver.api.v1.dto.response.FieldType;
import com.revealprecision.revealserver.api.v1.dto.response.FormulaResponse;
import com.revealprecision.revealserver.api.v1.dto.response.SecondStepQuestionsResponse;
import com.revealprecision.revealserver.enums.InputTypeEnum;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.AgeGroup;
import com.revealprecision.revealserver.persistence.domain.CampaignDrug;
import com.revealprecision.revealserver.persistence.domain.CampaignDrug.Fields;
import com.revealprecision.revealserver.persistence.domain.CountryCampaign;
import com.revealprecision.revealserver.persistence.domain.Drug;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.es.LocationElastic;
import com.revealprecision.revealserver.persistence.repository.CampaignDrugRepository;
import com.revealprecision.revealserver.persistence.repository.CountryCampaignRepository;
import com.revealprecision.revealserver.service.models.LocationResourcePlanning;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResourcePlanningService {

  private final CountryCampaignRepository countryCampaignRepository;
  private final CampaignDrugRepository campaignDrugRepository;
  private final LocationHierarchyService locationHierarchyService;
  private final LocationRelationshipService locationRelationshipService;
  private final RestHighLevelClient client;

  private static String suffix = "_buffer";
  private static String bufferQuestion = "What percent of buffer stock of '%s' is planned?";
  private static String drugDosageQuestion = "What is the average number of '%s' tablets required to treat 1 person in '%s' age group? (select 0 if not eligible)";
  private static String popPercent = "What percent is '%s' age group out of the total population?";
  private static String coveragePercent = "What percent of this population do you expect to reach during the campaign?";

  public List<CountryCampaign> getCountries() {
    return countryCampaignRepository.findAll();
  }

  public List<CampaignDrug> getCampaigns() {
    return campaignDrugRepository.findAll();
  }

  public CampaignDrug getCampaignByIdentifier(UUID identifier) {
    return campaignDrugRepository.findById(identifier).orElseThrow(() -> new NotFoundException(Pair.of(
        Fields.identifier, identifier), CampaignDrug.class));
  }

  public List<SecondStepQuestionsResponse> getSecondStepQuestions(ResourcePlanningRequest request) {
    List<SecondStepQuestionsResponse> response = new ArrayList<>();

    List<CountryCampaign> countryCampaigns = countryCampaignRepository.getAllByIdentifiers(request.getCountryIdentifiers());
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

  public List<LocationResourcePlanning> getDashboardData(ResourcePlanningDashboardRequest request) throws IOException {
    CampaignDrug campaign = getCampaignByIdentifier(request.getCampaign());
    LocationHierarchy locationHierarchy = locationHierarchyService.findByIdentifier(request.getLocationHierarchy());
    if(!locationHierarchy.getNodeOrder().contains(request.getLowestGeography())) {
      throw new ConflictException("Geography level does not exist in Location hierarch.");
    }
    List<LocationResourcePlanning> response = getDataFromElastic(request);

    return response;
  }

  private List<LocationResourcePlanning> getDataFromElastic(ResourcePlanningDashboardRequest request) throws IOException {
    List<LocationElastic> foundLocations = new ArrayList<>();
    List<LocationResourcePlanning> response = new ArrayList<>();
    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

    boolQuery.must(QueryBuilders.existsQuery("ancestry.".concat(request.getLocationHierarchy().toString())));
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
      Object total_population = loc.getMetadata().stream().filter(el-> el.getTag().equals("total_population")).findFirst().orElse(null).getValueNumber();
      Object total_structure = loc.getMetadata().stream().filter(el-> el.getTag().equals("total_structure")).findFirst().orElse(null).getValueNumber();
      response.add(new LocationResourcePlanning(loc, total_population, total_structure));
    }
    calculateAdjustedPopulation(response, request);
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
}