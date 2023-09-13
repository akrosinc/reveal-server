package com.revealprecision.revealserver.service;

import static com.revealprecision.revealserver.constants.EntityTagDataTypes.DATE;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.DOUBLE;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.INTEGER;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.STRING;
import static com.revealprecision.revealserver.service.SimulationHierarchyService.GENERATED;
import static com.revealprecision.revealserver.service.SimulationHierarchyService.SAVED;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.dto.csv.LocationCSVRecord;
import com.revealprecision.revealserver.api.v1.dto.factory.LocationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.PersonMainDataResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.DataFilterRequest;
import com.revealprecision.revealserver.api.v1.dto.request.EntityFilterRequest;
import com.revealprecision.revealserver.api.v1.dto.request.EntityTagRequest;
import com.revealprecision.revealserver.api.v1.dto.request.SearchValue;
import com.revealprecision.revealserver.api.v1.dto.response.AggregateHelper;
import com.revealprecision.revealserver.api.v1.dto.response.EntityMetadataResponse;
import com.revealprecision.revealserver.api.v1.dto.response.FeatureSetResponse;
import com.revealprecision.revealserver.api.v1.dto.response.FeatureSetResponseContainer;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.api.v1.dto.response.PersonMainData;
import com.revealprecision.revealserver.api.v1.dto.response.SimulationCountResponse;
import com.revealprecision.revealserver.api.v1.facade.factory.LocationCSVRecordFactory;
import com.revealprecision.revealserver.constants.EntityTagDataTypes;
import com.revealprecision.revealserver.enums.SignEntity;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.model.GenericHierarchy;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.SimulationRequest;
import com.revealprecision.revealserver.persistence.domain.aggregation.GeneratedHierarchy;
import com.revealprecision.revealserver.persistence.es.PersonElastic;
import com.revealprecision.revealserver.persistence.projection.LocationCoordinatesProjection;
import com.revealprecision.revealserver.persistence.repository.GeneratedHierarchyRepository;
import com.revealprecision.revealserver.persistence.repository.SimulationRequestRepository;
import com.revealprecision.revealserver.props.SimulationProperties;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.join.ScoreMode;
import org.csveed.bean.BeanInstructions;
import org.csveed.bean.BeanInstructionsImpl;
import org.csveed.row.RowWriter;
import org.csveed.row.RowWriterImpl;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
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
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

@Service
@Slf4j
@Profile("Elastic")
@RequiredArgsConstructor
public class EntityFilterEsService {

  private final LocationHierarchyService locationHierarchyService;
  private final RestHighLevelClient client;
  private final SimulationRequestRepository simulationRequestRepository;
  private final LocationService locationService;
  private final GeneratedHierarchyRepository generatedHierarchyRepository;

  private final SimulationProperties simulationProperties;

  @Value("${reveal.elastic.index-name}")
  String elasticIndex;

  private Optional<SimulationRequest> getSimulationRequestById(String simulationRequestId) {
    return simulationRequestRepository.findById(UUID.fromString(simulationRequestId));
  }

  public FeatureSetResponse getFullHierarchy(String hierarchyIdentifier)
      throws IOException, ParseException {
    DataFilterRequest request = new DataFilterRequest();
    request.setHierarchyIdentifier(hierarchyIdentifier);
    FeatureSetResponse featureSetResponse = new FeatureSetResponse();
    featureSetResponse.setFeatures(new ArrayList<>());
    featureSetResponse.setParents(new HashSet<>());
    featureSetResponse.setType("FeatureCollection");

    SearchHit lastResponse;
    do {
      try {

        FeatureSetResponseContainer featureSetResponse1 = filterEntites(request,
            10000, false, null);

        featureSetResponse.getFeatures()
            .addAll(featureSetResponse1.getFeatureSetResponse().getFeatures());
        featureSetResponse.getParents()
            .addAll(featureSetResponse1.getFeatureSetResponse().getParents());
        lastResponse = featureSetResponse1.getSearchHit();
        request.setLastHit(lastResponse);


      } catch (Exception ex) {
        log.error("Error getting page");
        throw ex;
      }
    } while (lastResponse != null);

    return featureSetResponse;
  }


  public SseEmitter getInactiveLocationsSseEmitter(String simulationRequestId) {
    Optional<SimulationRequest> simulationRequestById = getSimulationRequestById(
        simulationRequestId);

    if (simulationRequestById.isPresent()) {

      DataFilterRequest request = simulationRequestById.get().getRequest();

      request.setFilterGeographicLevelList(request.getInactiveGeographicLevelList());
      request.setEntityFilters(new ArrayList<>());
      request.setLocationIdentifier(null);

      SseEmitter emitter = new SseEmitter(180000L);
      ExecutorService sseMvcExecutor = Executors.newScheduledThreadPool(2);

      sseMvcExecutor.execute(() -> {
        SearchHit lastResponse;
        try {
          do {
            FeatureSetResponseContainer featureSetResponse1 = filterEntites(
                request, 2000, false, null);

            SseEventBuilder event = SseEmitter.event()
                .data(featureSetResponse1.getFeatureSetResponse())
                .id(String.valueOf(UUID.randomUUID())).name("parent");

            lastResponse = featureSetResponse1.getSearchHit();

            request.setLastHit(lastResponse);

            emitter.send(event);

          } while (lastResponse != null);

          emitter.send(SseEmitter.event().name("close").id("").data("{}"));

        } catch (Exception ex) {
          emitter.completeWithError(ex);
        }
        emitter.complete();

      });

      return emitter;

    } else {
      return null;
    }
  }

  public InputStreamResource getFullHierarchyCSV(String hierarchyIdentifier, char delimiter)
      throws IOException, ParseException {
    StringWriter stringWriter = new StringWriter();

    BeanInstructions instructions = new BeanInstructionsImpl(LocationCSVRecord.class);
    instructions.setSeparator(delimiter);
    instructions.setUseHeader(false);

    DataFilterRequest request = new DataFilterRequest();
    request.setHierarchyIdentifier(hierarchyIdentifier);
    FeatureSetResponse featureSetResponse = new FeatureSetResponse();
    featureSetResponse.setFeatures(new ArrayList<>());
    featureSetResponse.setParents(new HashSet<>());
    featureSetResponse.setType("FeatureCollection");

    SearchHit lastResponse;
    do {
      try {

        log.info("calling filterEntites");
        FeatureSetResponseContainer featureSetResponse1 = filterEntites(request,
            10000, true, List.of("geometry"));
        log.info("called filterEntites");

        featureSetResponse.getFeatures()
            .addAll(featureSetResponse1.getFeatureSetResponse().getFeatures());
        featureSetResponse.getParents()
            .addAll(featureSetResponse1.getFeatureSetResponse().getParents());
        lastResponse = featureSetResponse1.getSearchHit();
        request.setLastHit(lastResponse);

      } catch (Exception ex) {
        log.error("Error getting page");
        throw ex;
      }
    } while (lastResponse != null);

    Map<String, Map<String, Object>> featureMap = featureSetResponse.getFeatures().stream().map(
            featureSet -> new SimpleEntry<>(featureSet.getIdentifier().toString(),
                featureSet.getProperties().getMetadata().stream().collect(
                    Collectors.toMap(EntityMetadataResponse::getType, EntityMetadataResponse::getValue,
                        (a, b) -> b))))
        .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue, (a, b) -> b));

    Set<String> tags = featureSetResponse.getFeatures().stream().flatMap(
        feature -> feature.getProperties().getMetadata().stream()
            .map(EntityMetadataResponse::getType)).collect(Collectors.toSet());

    List<LocationCSVRecord> locationList = featureSetResponse.getFeatures().stream().map(
        feature -> LocationCSVRecordFactory.getLocationCSVRecordFromLocationResponse(feature,
            tags.stream().map(tag -> new SimpleEntry<>(tag,
                    featureMap.get(feature.getIdentifier().toString()).getOrDefault(tag, "")))
                .collect(Collectors.toSet()))).collect(Collectors.toList());

    RowWriter rowWriter = new RowWriterImpl(stringWriter);

    List<String> headerArr = new ArrayList<>();
    headerArr.add("Identifier");
    headerArr.add("Name");
    headerArr.add("GeographicLevel");
    headerArr.addAll(tags);
    rowWriter.writeHeader(headerArr.toArray(new String[0]));

    locationList.stream().forEach(location -> {
      List<String> strArr = new ArrayList<>();
      strArr.add(location.getIdentifier());
      strArr.add(location.getName());
      strArr.add(location.getGeographicLevel());

      List<String> collect = location.getMeta().stream().map(SimpleEntry::getValue).map(val -> {
        if (val instanceof Double) {
          return ((Double) val).toString();
        } else {
          return (String) val;
        }
      }).collect(Collectors.toList());

      strArr.addAll(collect);

      rowWriter.writeRow(strArr.toArray(new String[0]));
    });

    stringWriter.close();
    InputStream targetStream = new ByteArrayInputStream(stringWriter.toString().getBytes());
    return new InputStreamResource(targetStream);
  }

  public SseEmitter getSseEmitter(String simulationRequestId) {
    Optional<SimulationRequest> simulationRequestById = getSimulationRequestById(
        simulationRequestId);

    if (simulationRequestById.isPresent()) {
      DataFilterRequest request = simulationRequestById.get().getRequest();
      GenericHierarchy locationHierarchy = new GenericHierarchy();
      if (request.getHierarchyType().equals(SAVED)) {
        LocationHierarchy locationHierarchyDB = locationHierarchyService.findByIdentifier(
            UUID.fromString(request.getHierarchyIdentifier()));

        locationHierarchy.setIdentifier(locationHierarchyDB.getIdentifier().toString());
        locationHierarchy.setName(locationHierarchyDB.getName());
        locationHierarchy.setNodeOrder(locationHierarchyDB.getNodeOrder());
        locationHierarchy.setType(SAVED);

      } else {

        Optional<GeneratedHierarchy> generatedHierarchyOptional = generatedHierarchyRepository.findById(
            Integer.valueOf(request.getHierarchyIdentifier()));

        if (generatedHierarchyOptional.isPresent()) {
          locationHierarchy.setIdentifier(String.valueOf(generatedHierarchyOptional.get().getId()));
          locationHierarchy.setName(generatedHierarchyOptional.get().getName());
          locationHierarchy.setType(GENERATED);
          locationHierarchy.setNodeOrder(generatedHierarchyOptional.get().getNodeOrder());
        }

      }

      SseEmitter emitter = new SseEmitter(180000L);
      ExecutorService sseMvcExecutor = Executors.newScheduledThreadPool(2);
      Set<String> parents = new HashSet<>();

      List<AggregateHelper> aggregateHelpers = new ArrayList<>();

      GenericHierarchy finalLocationHierarchy = locationHierarchy;
      sseMvcExecutor.execute(() -> {
        SearchHit lastResponse;

        try {
          do {
            log.trace("fetching in try loop");
            FeatureSetResponseContainer featureSetResponse1 = filterEntites(
                request, simulationProperties.getFetchLocationPageSize(), false, null);

            log.trace("got results");
            parents.addAll(featureSetResponse1.getFeatureSetResponse().getParents());

            aggregateHelpers.addAll(getAggregateHelpers(featureSetResponse1));
            log.trace("got getAggregateHelpers");

            SseEventBuilder event = SseEmitter.event()
                .data(buildSseEventObject(featureSetResponse1))
                .id(String.valueOf(UUID.randomUUID())).name("message");
            log.trace("build event");
            lastResponse = featureSetResponse1.getSearchHit();

            request.setLastHit(lastResponse);

            emitter.send(event);
            log.trace("should have returned to browser");
          } while (lastResponse != null);
          log.trace("done with children processing parent");

          processParentData(request, emitter, parents, aggregateHelpers, finalLocationHierarchy);

        } catch (Exception ex) {
          emitter.completeWithError(ex);
        }
        emitter.complete();

      });

      return emitter;
    } else {
      return null;
    }
  }
//  private GZIPOutputStream compressObject() throws IOException {
//    FeatureSetResponse featureSetResponse = new FeatureSetResponse();
//    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
//    GZIPOutputStream zipStream = new GZIPOutputStream(byteStream);
//    zipStream.write(featureSetResponse);
//  }

  private FeatureSetResponse buildSseEventObject(FeatureSetResponseContainer featureSetResponse1) {
    return FeatureSetResponse.builder()
        .features(featureSetResponse1.getFeatureSetResponse().getFeatures())
        .type(featureSetResponse1.getFeatureSetResponse().getType())
        .identifier(featureSetResponse1.getFeatureSetResponse().getIdentifier()).build();
  }

  private List<AggregateHelper> getAggregateHelpers(
      FeatureSetResponseContainer featureSetResponse1) {
    return featureSetResponse1.getFeatureSetResponse().getFeatures().stream().map(
            feature -> new AggregateHelper(feature.getIdentifier().toString(),
                feature.getAncestry() == null ? new ArrayList<>() : feature.getAncestry(),
                feature.getProperties().getMetadata(),
                feature.getProperties().getGeographicLevel(), null))
        .collect(Collectors.toList());
  }

  private void processParentData(DataFilterRequest request, SseEmitter emitter, Set<String> parents,
      List<AggregateHelper> aggregateHelpers, GenericHierarchy finalLocationHierarchy)
      throws IOException {
    int parentBatch = simulationProperties.getFetchParentPageSize();

    List<Set<String>> parentBatches = splitArray(parents, parentBatch);

    int count = 0;
    for (Set<String> batch : parentBatches) {
      FeatureSetResponseContainer featureSetResponseContainer = retrieveParentLocations(
          batch, request.getHierarchyIdentifier(), request);
      emitter.send(SseEmitter.event().name("parent").id(UUID.randomUUID().toString())
          .data(featureSetResponseContainer.getFeatureSetResponse().getFeatures()));
      count++;
      log.trace("processed parent batched: {}", count);
    }

    List<IndividualAggregateHelperType> individualAggregateHelperTypeList = processAggregateHelpers(
        aggregateHelpers, finalLocationHierarchy);
    log.trace("processed parent batched - processAggregateHelpers");

    Map<String, Optional<IndividualAggregateHelperType>> lowestLevelPerTag = getTagsAndLowestLevelsTheyPresentIn(
        individualAggregateHelperTypeList);
    log.trace("processed parent batched - getTagsAndLowestLevelsTheyPresentIn");

    Map<String, List<EntityMetadataResponse>> collect4 = getMetadataOfLowestLevels(
        aggregateHelpers,
        lowestLevelPerTag);
    log.trace("processed parent batched - getMetadataOfLowestLevels");

    Map<String, Double> summationOfTagsOfLowestLevels = getSummationOfTagsOfLowestLevels(
        collect4);
    log.trace("processed parent batched - getSummationOfTagsOfLowestLevels");

    emitter.send(SseEmitter.event().name("stats").id("").data(summationOfTagsOfLowestLevels));
    log.trace("processed parent batched - sent stats");

    List<AggregateHelper> incorporateLowestLevelTagsIntoParents = incorporateLowestLevelTagsIntoParents(
        aggregateHelpers,
        lowestLevelPerTag);
    log.trace("processed parent batched - incorporateLowestLevelTagsIntoParents");

    Map<String, List<List<EntityMetadataResponse>>> distributeTagDataOfLowestLevelsToParents = distributeTagDataOfLowestLevelsToParents(
        incorporateLowestLevelTagsIntoParents);
    log.trace("processed parent batched - distributeTagDataOfLowestLevelsToParents");

    Map<String, List<EntityMetadataResponse>> processDistributedData = processDistributedData(
        distributeTagDataOfLowestLevelsToParents);
    log.trace("processed parent batched - processDistributedData");

    Map<String, Map<String, Object>> summationByLocationByTag = getSummationByLocationByTag(
        processDistributedData);
    log.trace("processed parent batched - getSummationByLocationByTag");

    emitter.send(SseEmitter.event().name("aggregations").id("").data(summationByLocationByTag));
    log.trace("processed parent batched - sent aggregations to browser");

    Map<String, String> collect = getTagDefinitions(processDistributedData);
    log.trace("processed parent batched - getTagDefinitions");

    emitter.send(SseEmitter.event().name("aggregationDefinitions").id("").data(collect));
    log.trace("processed parent batched - send aggregation definitions");

    emitter.send(SseEmitter.event().name("close").id("").data("close"));
    log.trace("processed parent batched - send close");
  }

  // List<NodeNumber in Level,  TagName> - list of tags with the levels they are present in
  private List<IndividualAggregateHelperType> processAggregateHelpers(
      List<AggregateHelper> aggregateHelpers,
      GenericHierarchy finalLocationHierarchy) {
    return aggregateHelpers.stream()
        .peek(aggregateHelper -> aggregateHelper.setFilteredNodeOrder(
            finalLocationHierarchy.getNodeOrder()))
        .flatMap(
            aggregateHelper -> aggregateHelper.getEntityMetadataResponses()
                .stream().map(
                    entityMetadataResponse -> new IndividualAggregateHelper(
                        aggregateHelper.getFilteredNodeOrder()
                            .indexOf(aggregateHelper.getGeographicLevel()),
                        entityMetadataResponse))).map(
            individualAggregateHelper -> new IndividualAggregateHelperType(
                individualAggregateHelper.getIndex(),
                individualAggregateHelper.getEntityMetadataResponse().getType()))
        .collect(Collectors.toList());
  }

  private Map<String, String> getTagDefinitions(Map<String, List<EntityMetadataResponse>> a) {
    return a.entrySet().stream()
        .flatMap(entry -> entry.getValue().stream()
            .filter(
                entityMetadataResponse -> entityMetadataResponse.getType().endsWith("-sum")
                    || entityMetadataResponse.getType().endsWith("-count"))
        ).collect(Collectors.toMap(EntityMetadataResponse::getType,
            EntityMetadataResponse::getFieldType, (c, d) -> d));
  }

  //Aggregation of the lowest level tags rolled up to parents in a map per tag in a map
  //          INPUT -> a
  //
  // Map: LocationParent1, metadataList=[metadata=[tagABC-sum=2], metadata=[tagABC-sum=3]]
  // Map: LocationParent2, metadataList=[metadata=[tagABC-sum=1], metadata=[tagABC-sum=1]]
  // Map: LocationGrandParent,
  //          metadataList=[metadata=[tagDEF-sum=5],
  //                        metadata=[tagDEF-sum=4],
  //                        metadata=[tagABC-sum=1],
  //                        metadata=[tagABC-sum=1],
  //                        metadata=[tagABC-sum=2],
  //                        metadata=[tagABC-sum=3]]
  //
  //          OUTPUT -> b
  //
  //Map:  LocationGrandParent, (Map: tagDEF-sum, val=9
  //                            Map: tagABC-sum, val=7)
  //Map:  LocationParent1, (Map: tagABC-sum, val=5)
  //Map:  LocationParent2, (Map: tagABC-sum, val=2)
  private Map<String, Map<String, Object>> getSummationByLocationByTag(
      Map<String, List<EntityMetadataResponse>> a) {
    return a.entrySet().stream().filter(entry -> !entry.getKey().equals(""))
        .map(entry -> new SimpleEntry<>(entry.getKey(),
            entry.getValue()
                .stream().filter(
                    entityMetadataResponse -> entityMetadataResponse.getType().endsWith("-sum")
                        || entityMetadataResponse.getType().endsWith("-count")
                        || entityMetadataResponse.getFieldType().equals("generated"))

                .collect(
                    Collectors.groupingBy(EntityMetadataResponse::getType,
                        Collectors.reducing(0, EntityMetadataResponse::getValue,
                            (c, d) -> (c instanceof Integer ? (Integer) c : (double) c) + (
                                d instanceof Integer ? (Integer) d : (double) d))))))
        .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
  }

  //          INPUT -> collect8
  //
  //  Map: LocationParent1-LocationGrandParent metadataList=[metadata=[tagABC-sum=2], metadata=[tagABC-sum=3]]
  //  Map: LocationParent2-LocationGrandParent metadataList=[metadata=[tagABC-sum=1], metadata=[tagABC-sum=1]]
  //  Map: LocationGrandParent metadataList=[metadata=[tagDEF-sum=5], metadata=[tagDEF-sum=4]]
  //
  //          OUTPUT -> a
  //
  // Map: LocationParent1, metadataList=[metadata=[tagABC-sum=2], metadata=[tagABC-sum=3]]
  // Map: LocationParent2, metadataList=[metadata=[tagABC-sum=1], metadata=[tagABC-sum=1]]
  // Map: LocationGrandParent, metadataList=[metadata=[tagDEF-sum=5], metadata=[tagDEF-sum=4], metadata=[tagABC-sum=1], metadata=[tagABC-sum=1], metadata=[tagABC-sum=2], metadata=[tagABC-sum=3]]
  private Map<String, List<EntityMetadataResponse>> processDistributedData(
      Map<String, List<List<EntityMetadataResponse>>> distributeTagDataOfLowestLevelsToParents) {
    return distributeTagDataOfLowestLevelsToParents.entrySet().stream()
        .map(entry -> Stream.of(entry.getKey().split(","))
            .map(ancestor -> Pair.of(ancestor,
                entry.getValue().stream().flatMap(Collection::stream)
                    .collect(Collectors.toList())))
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, (c, b) -> {
              c.addAll(b);
              return b;
            }))).flatMap(map -> map.entrySet().stream())
        //Pair [LocationParent1, metadataList=[metadata=[tagABC-sum=2], metadata=[tagABC-sum=3]]
        //Pair [LocationParent2, metadataList=[metadata=[tagABC-sum=1], metadata=[tagABC-sum=1]]
        //Pair [LocationGrandParent, metadataList=[metadata=[tagDEF-sum=5], metadata=[tagDEF-sum=4]]
        //Pair [LocationGrandParent, metadataList=[metadata=[tagABC-sum=1], metadata=[tagABC-sum=1]]
        //Pair [LocationGrandParent, metadataList=[metadata=[tagABC-sum=2], metadata=[tagABC-sum=3]]
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (c, b) -> {
          c.addAll(b);
          return c;
        }));
  }

  //         INPUT -> listsOfLowestLocationsWithTheirTags
  //         ======
  // LocationGrandParent - metadata=[] ancestry=LocationGrandParent
  //
  //    ,LocationParent1 metadata=[] ancestry=LocationParent1,LocationGrandParent
  //    ,LocationParent2 metadata=[tagDEF-sum=5] ancestry=LocationParent2,LocationGrandParent
  //    ,LocationParent3 metadata=[tagDEF-sum=4] ancestry=LocationParent3,LocationGrandParent
  //
  //        ,Location1  metadata=[tagABC-sum=2] ancestry=Location1,LocationParent1,LocationGrandParent
  //        ,Location2  metadata=[tagABC-sum=3] ancestry=Location1,LocationParent1,LocationGrandParent
  //        ,Location3  metadata=[tagABC-sum=1] ancestry=Location1,LocationParent2,LocationGrandParent
  //        ,Location4  metadata=[tagABC-sum=1] ancestry=Location1,LocationParent2,LocationGrandParent
  //
  //          OUTPUT -> collect8
  //
  //  Map: LocationParent1-LocationGrandParent metadataList=[metadata=[tagABC-sum=2], metadata=[tagABC-sum=3]]
  //  Map: LocationParent2-LocationGrandParent metadataList=[metadata=[tagABC-sum=1], metadata=[tagABC-sum=1]]
  //  Map: LocationGrandParent metadataList=[metadata=[tagDEF-sum=5], metadata=[tagDEF-sum=4]]
  private Map<String, List<List<EntityMetadataResponse>>> distributeTagDataOfLowestLevelsToParents(
      List<AggregateHelper> incorporateLowestLevelTagsIntoParents) {
    return incorporateLowestLevelTagsIntoParents.stream()
        .map(lowestLocationWithTheirTags -> Pair.of(String.join(",",
                lowestLocationWithTheirTags.getAncestry().size() > 1
                    ? lowestLocationWithTheirTags.getAncestry()
                    .subList(1, lowestLocationWithTheirTags.getAncestry().size())
                    : new ArrayList<>()),
            lowestLocationWithTheirTags.getEntityMetadataResponses()))
        // LocationGrandParent - metadata=[] ancestry=LocationGrandParent -> Pair["" ,   metadata=[] ]
        //
        //    ,LocationParent1 metadata=[] ancestry=LocationParent1,LocationGrandParent -> Pair["LocationGrandParent" ,   metadata=[] ]
        //    ,LocationParent2 metadata=[tagDEF-sum=5] ancestry=LocationParent2,LocationGrandParent -> Pair["LocationGrandParent" ,   metadata=[tagDEF-sum=5] ]
        //    ,LocationParent3 metadata=[tagDEF-sum=4] ancestry=LocationParent3,LocationGrandParent -> Pair["LocationGrandParent" ,   metadata=[tagDEF-sum=4] ]
        //
        //        ,Location1  metadata=[tagABC-sum=2] ancestry=Location1,LocationParent1,LocationGrandParent -> Pair["LocationParent1-LocationGrandParent" ,   metadata=[tagABC-sum=2] ]
        //        ,Location2  metadata=[tagABC-sum=3] ancestry=Location1,LocationParent1,LocationGrandParent -> Pair["LocationParent1-LocationGrandParent" ,   metadata=[tagABC-sum=3] ]
        //        ,Location3  metadata=[tagABC-sum=1] ancestry=Location1,LocationParent2,LocationGrandParent -> Pair["LocationParent2-LocationGrandParent" ,   metadata=[tagABC-sum=1] ]
        //        ,Location4  metadata=[tagABC-sum=1] ancestry=Location1,LocationParent2,LocationGrandParent -> Pair["LocationParent2-LocationGrandParent" ,   metadata=[tagABC-sum=1] ]

        .collect(
            Collectors.groupingBy(Pair::getFirst,
                Collectors.mapping(Pair::getSecond, Collectors.toList())));
  }

  //        INPUT -> aggregateHelpers
  //        =====
  // LocationGrandParent - metadata=[tagABC-sum=7, tagDEF-sum=9] ancestry=LocationGrandParent
  //
  //    ,LocationParent1 metadata=[tagABC-sum=5] ancestry=LocationParent1,LocationGrandParent
  //    ,LocationParent2 metadata=[tagABC-sum=2, tagDEF-sum=5] ancestry=LocationParent2,LocationGrandParent
  //    ,LocationParent3 metadata=[tagABC-sum=2, tagDEF-sum=4] ancestry=LocationParent3,LocationGrandParent
  //
  //        ,Location1  metadata=[tagABC-sum=2] ancestry=Location1,LocationParent1,LocationGrandParent
  //        ,Location2  metadata=[tagABC-sum=3] ancestry=Location1,LocationParent1,LocationGrandParent
  //        ,Location3  metadata=[tagABC-sum=1] ancestry=Location1,LocationParent2,LocationGrandParent
  //        ,Location4  metadata=[tagABC-sum=1] ancestry=Location1,LocationParent2,LocationGrandParent
  //
  //         OUTPUT -> listsOfLowestLocationsWithTheirTags
  //         ======
  // LocationGrandParent - metadata=[] ancestry=LocationGrandParent
  //
  //    ,LocationParent1 metadata=[] ancestry=LocationParent1,LocationGrandParent
  //    ,LocationParent2 metadata=[tagDEF-sum=5] ancestry=LocationParent2,LocationGrandParent
  //    ,LocationParent3 metadata=[tagDEF-sum=4] ancestry=LocationParent3,LocationGrandParent
  //
  //        ,Location1  metadata=[tagABC-sum=2] ancestry=Location1,LocationParent1,LocationGrandParent
  //        ,Location2  metadata=[tagABC-sum=3] ancestry=Location1,LocationParent1,LocationGrandParent
  //        ,Location3  metadata=[tagABC-sum=1] ancestry=Location1,LocationParent2,LocationGrandParent
  //        ,Location4  metadata=[tagABC-sum=1] ancestry=Location1,LocationParent2,LocationGrandParent

  private List<AggregateHelper> incorporateLowestLevelTagsIntoParents(
      List<AggregateHelper> aggregateHelpers,
      Map<String, Optional<IndividualAggregateHelperType>> lowestLevelPerTag) {
    return aggregateHelpers.stream()
        .map(aggregateHelper -> {
          AggregateHelper helper = new AggregateHelper();
          helper.setAncestry(aggregateHelper.getAncestry());
          helper.setFilteredNodeOrder(aggregateHelper.getFilteredNodeOrder());
          helper.setEntityMetadataResponses(
              aggregateHelper.getEntityMetadataResponses().stream()
                  .filter(entityMetadataResponse -> {
                    if (lowestLevelPerTag.containsKey(entityMetadataResponse.getType())) {
                      Optional<IndividualAggregateHelperType> individualAggregateHelperType = lowestLevelPerTag.get(
                          entityMetadataResponse.getType());

                      return individualAggregateHelperType.map(
                              aggregateHelperType -> aggregateHelperType.getIndex().equals(
                                  aggregateHelper.getFilteredNodeOrder()
                                      .indexOf(aggregateHelper.getGeographicLevel())))
                          .orElse(false);
                    }
                    return false;
                  })

                  .collect(Collectors.toList()));
          helper.setIdentifier(aggregateHelper.getIdentifier());
          helper.setGeographicLevel(aggregateHelper.getGeographicLevel());
          return helper;
        }).collect(Collectors.toList());
  }

  //stats - summation of tag items of the lowest levels - The summation of the lowest levels
  // of the result set represents the stats of the entire result set
  private Map<String, Double> getSummationOfTagsOfLowestLevels(
      Map<String, List<EntityMetadataResponse>> collect4) {
    return collect4.entrySet().stream().map(
            entry -> new SimpleEntry<>(entry.getKey(), entry.getValue().stream()
                .mapToDouble(entityreposnse -> (double) entityreposnse.getValue()).sum()))
        .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
  }

  // Map<Tag, IndividualAggregateHelperType(NodeNumber in Level,  TagName) - finding the lowest level a tag is present in
  private Map<String, Optional<IndividualAggregateHelperType>> getTagsAndLowestLevelsTheyPresentIn(
      List<IndividualAggregateHelperType> individualAggregateHelperTypeList) {
    return individualAggregateHelperTypeList.stream()
        .collect(Collectors.groupingBy(IndividualAggregateHelperType::getType,
            Collectors.reducing((a, b) -> {
              if (b.getIndex() > a.getIndex()) {
                return b;
              } else {
                return a;
              }
            })));
  }

  // Map<Tag, List<EntityMetadataResponse> - Each metadata which belonged to the lowest levels per tag
  private Map<String, List<EntityMetadataResponse>> getMetadataOfLowestLevels(
      List<AggregateHelper> aggregateHelpers,
      Map<String, Optional<IndividualAggregateHelperType>> lowestLevelPerTag) {
    return aggregateHelpers.stream().flatMap(
        aggregateHelper -> aggregateHelper.getEntityMetadataResponses().stream()
            .filter(entityMetadataResponse -> {
              if (lowestLevelPerTag.containsKey(entityMetadataResponse.getType())) {

                Optional<IndividualAggregateHelperType> individualAggregateHelperType = lowestLevelPerTag.get(
                    entityMetadataResponse.getType());
                return individualAggregateHelperType.map(
                    aggregateHelperType -> aggregateHelperType.getIndex().equals(
                        aggregateHelper.getFilteredNodeOrder()
                            .indexOf(aggregateHelper.getGeographicLevel()))).orElse(false);
              } else {
                return false;
              }
            })).collect(Collectors.groupingBy(EntityMetadataResponse::getType));
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

    List<String> nodeOrder = null;

    if (request.getHierarchyType().equals(SAVED)) {
      nodeOrder = locationHierarchyService.findByIdentifier(
              UUID.fromString(request.getHierarchyIdentifier()))
          .getNodeOrder();
    } else {

      Optional<GeneratedHierarchy> generatedHierarchyOptional = generatedHierarchyRepository.findById(
          Integer.valueOf(request.getHierarchyIdentifier()));

      if (generatedHierarchyOptional.isPresent()) {
        nodeOrder = generatedHierarchyOptional.get().getNodeOrder();
      }

    }

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

    ExistsQueryBuilder existsQueryBuilder = QueryBuilders.existsQuery(
        "hierarchyDetailsElastic." + request.getHierarchyIdentifier());
    NestedQueryBuilder nestedQueryBuilder = QueryBuilders.nestedQuery(
        "hierarchyDetailsElastic", existsQueryBuilder, ScoreMode.None);

    Map<String, Long> collect = geographicLevelList.stream().map(geographicLevel -> {
      try {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        boolQuery.must(
            QueryBuilders.matchPhraseQuery("level", geographicLevel));

        boolQuery.must(nestedQueryBuilder);

        List<EntityFilterRequest> locationFilters = new ArrayList<>(request.getEntityFilters());

        for (EntityFilterRequest req : locationFilters) {
          if (req.getRange() == null && req.getValues() == null) {
            boolQuery.must(mustStatement(req, request.getHierarchyIdentifier()));
          } else if (req.getSearchValue() == null && req.getValues() == null) {
            boolQuery.must(rangeStatement(req, request.getHierarchyIdentifier()));
          } else if (req.getSearchValue() == null && req.getRange() == null) {
            boolQuery.must(shouldStatement(req, request.getHierarchyIdentifier()));
          } else {
            throw new ConflictException("Request object bad formatted.");
          }
        }

        if (request.getHierarchyIdentifier() != null && request.getLocationIdentifier() != null) {
          boolQuery.must(QueryBuilders.nestedQuery("hierarchyDetailsElastic",
              QueryBuilders.matchQuery("hierarchyDetailsElastic".concat(".")
                      .concat(request.getHierarchyIdentifier()).concat(".")
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
          boolQuery.must(nestedQueryBuilder);

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

    simulationCountResponse.setSearchRequestId(simulationRequestSaved.getIdentifier().toString());

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

    ExistsQueryBuilder existsQueryBuilder = QueryBuilders.existsQuery(
        "hierarchyDetailsElastic." + request.getHierarchyIdentifier());
    NestedQueryBuilder nestedQueryBuilder = QueryBuilders.nestedQuery(
        "hierarchyDetailsElastic", existsQueryBuilder, ScoreMode.None);

    if (request.getFilterGeographicLevelList() != null) {
      BoolQueryBuilder geoBoolQuery = QueryBuilders.boolQuery();
      request.getFilterGeographicLevelList().stream().forEach(geoList ->
          geoBoolQuery.should().add(QueryBuilders.matchPhraseQuery("level", geoList)));

      boolQuery.must(geoBoolQuery);

    }

    boolQuery.must(nestedQueryBuilder);

    if (request.getEntityFilters() != null) {
      locationFilters.addAll(request.getEntityFilters());
    }

    for (EntityFilterRequest req : locationFilters) {
      if (req.getRange() == null && req.getValues() == null) {
        boolQuery.must(mustStatement(req, request.getHierarchyIdentifier()));
      } else if (req.getSearchValue() == null && req.getValues() == null) {
        boolQuery.must(rangeStatement(req, request.getHierarchyIdentifier()));
      } else if (req.getSearchValue() == null && req.getRange() == null) {
        boolQuery.must(shouldStatement(req, request.getHierarchyIdentifier()));
      } else {
        throw new ConflictException("Request object bad formatted.");
      }
    }

    if (request.getHierarchyIdentifier() != null && request.getLocationIdentifier() != null) {
      boolQuery.must(QueryBuilders.nestedQuery("hierarchyDetailsElastic",
          QueryBuilders.matchQuery("hierarchyDetailsElastic".concat(".")
                  .concat(request.getHierarchyIdentifier()).concat(".").concat("ancestry"),
              request.getLocationIdentifier().toString()
          ), ScoreMode.None));
    }

    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    sourceBuilder.size(batchSize);

    sourceBuilder.query(boolQuery);

    String collect = request.getResultTags().stream()
        .map(entityFilterRequest -> "n.add('" + entityFilterRequest.getTag() + "');")
        .collect(Collectors.joining(""));
    Script inline = new Script(ScriptType.INLINE, "painless",
        "List a = params['_source']['metadata']; List n = new ArrayList(); " + collect
            + "  return a.stream().filter(val->n.contains(val.tag) && val.hierarchyIdentifier.equals('"
            + request.getHierarchyIdentifier() + "')).collect(Collectors.toList());",
        new HashMap<>());

    sourceBuilder.scriptField("meta", inline);

    String[] val = new String[0];

    if (excludeFields) {
      sourceBuilder.fetchSource(new ArrayList<String>().toArray(val), exclusionList.toArray(val));
    } else {
      sourceBuilder.fetchSource(new ArrayList<String>().toArray(val),
          new ArrayList<String>().toArray(val));
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
                request.getHierarchyIdentifier());
            locToAdd.getProperties().setSimulationSearchResult(true);
            locToAdd.getProperties()
                .setLevelColor(
                    getGeoLevelColor(locToAdd.getProperties().getGeographicLevelNodeNumber()));

            return locToAdd;
          } catch (JsonProcessingException e) {
            e.printStackTrace();
          }
          return null;
        }).collect(Collectors.toList());
    log.trace("processed search results");

    response.setParents(parentLocations);

    locationResponses = getLocationCenters(locationResponses);
    response.setFeatures(locationResponses);

    if (searchResponse.getHits().getHits().length > 0) {
      lastHit =
          searchResponse.getHits().getHits()[searchResponse.getHits().getHits().length - 1];
    }

    response.setType("FeatureCollection");
    log.trace("returning results");
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

  private String getGeoLevelColor(Integer geolevel) {

    int end = 145 + geolevel * 35;

    return "#".concat(Integer.toHexString(end)).concat(Integer.toHexString(end))
        .concat(Integer.toHexString(end));
  }

  public FeatureSetResponseContainer retrieveParentLocations(Set<String> parentIds,
      String hierarchyId, DataFilterRequest request)
      throws IOException {

//    List<LocationResponse> responses = new ArrayList<>();
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    sourceBuilder.query(QueryBuilders.termsQuery("_id", parentIds));
    SearchRequest searchRequest = new SearchRequest(elasticIndex);

    sourceBuilder.size(10000);

    String collect = request.getResultTags().stream()
        .map(entityFilterRequest -> "n.add('" + entityFilterRequest.getTag() + "');")
        .collect(Collectors.joining(""));
    Script inline = new Script(ScriptType.INLINE, "painless",
        "List a = params['_source']['metadata']; List n = new ArrayList(); " + collect
            + "  return a.stream().filter(val->n.contains(val.tag) && val.hierarchyIdentifier.equals('"
            + request.getHierarchyIdentifier() + "')).collect(Collectors.toList());",
        new HashMap<>());

    sourceBuilder.scriptField("meta", inline);

    String[] val = new String[0];
    sourceBuilder.fetchSource(new ArrayList<String>().toArray(val), new ArrayList<String>().toArray(val));

    searchRequest.source(sourceBuilder);

    log.trace("parent query {}",searchRequest);

    SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

//    ObjectMapper mapper = new ObjectMapper();
    List<LocationResponse> responses = Arrays.stream(searchResponse.getHits().getHits())
        .filter(Objects::nonNull).map(hit -> {
          LocationResponse locationResponse = null;
          try {
            locationResponse = LocationResponseFactory.fromSearchHit(hit, null,
                request.getHierarchyIdentifier());
            locationResponse.getProperties()
                .setLevelColor(
                    getGeoLevelColor(
                        locationResponse.getProperties().getGeographicLevelNodeNumber()));
          } catch (JsonProcessingException e) {
            e.printStackTrace();
          }

//      LocationResponse locationResponse = LocationResponseFactory.fromElasticModel(parent,
//          parent.getHierarchyDetailsElastic() != null ? parent.getHierarchyDetailsElastic()
//              .get(hierarchyId) : null, parent.getMetadata().stream()
//              .filter(meta -> meta.getHierarchyIdentifier().equals(hierarchyId)).map(meta ->
//                  EntityMetadataResponse.builder().type(meta.getTag()).value(meta.getValueNumber())
//                      .fieldType(meta.getFieldType())
//                      .build())
//              .collect(Collectors.toList()));

//      responses.add(locationResponse);
          return locationResponse;
        }).filter(Objects::nonNull).collect(Collectors.toList());
//      LocationElastic parent = mapper.readValue(hit.getSourceAsString(), LocationElastic.class);

//    }
    FeatureSetResponse response = new FeatureSetResponse();
    response.setType("FeatureCollection");
    response.setFeatures(responses);

    return new FeatureSetResponseContainer(response, null);
  }


  public NestedQueryBuilder nestedPersonQuery(List<EntityFilterRequest> requests,
      String hierarchyId)
      throws ParseException {
    BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
    for (EntityFilterRequest req : requests) {
      if (req.getRange() == null && req.getValues() == null) {
        boolQueryBuilder.must(mustStatement(req, hierarchyId));
      } else if (req.getSearchValue() == null && req.getValues() == null) {
        boolQueryBuilder.must(rangeStatement(req, hierarchyId));
      } else if (req.getSearchValue() == null && req.getRange() == null) {
        boolQueryBuilder.must(shouldStatement(req, hierarchyId));
      } else {
        throw new ConflictException("Request object bad formatted.");
      }
    }
    return QueryBuilders.nestedQuery("person", boolQueryBuilder, ScoreMode.None).innerHit(
        new InnerHitBuilder());
  }

  private AbstractQueryBuilder<?> mustStatement(EntityFilterRequest request, String hierarchyId)
      throws ParseException {

    BoolQueryBuilder andStatement = QueryBuilders.boolQuery();

    String searchField;
//    if (request.getFieldType().equals("ImportedData")) {
    if (request.getValueType().equals("date")) {
      prepareDate(request);
    }
    searchField = "metadata.";

    andStatement.must(
        QueryBuilders.matchQuery(searchField.concat("tag"), request.getTag()));

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

    andStatement.must(
        QueryBuilders.matchQuery(searchField.concat("hierarchyIdentifier"), hierarchyId));

    return QueryBuilders.nestedQuery("metadata", andStatement,
        ScoreMode.None);
//    }
//    else if (request.getFieldType().equals("core")) {
//      CoreField coreField = coreFieldService.getCoreFieldByIdentifier(request.getFieldIdentifier());
//      if (coreField.getValueType().equals(EntityTagDataTypes.DATE)) {
//        prepareDate(request);
//      }
//      searchField = LookupEntityTypeCodeEnum.LOCATION_CODE.getLookupEntityType().concat(".")
//          .concat(coreField.getField());
//      if (request.getSearchValue().getSign() == SignEntity.EQ) {
//        andStatement.must(
//            QueryBuilders.matchQuery(searchField, request.getSearchValue().getValue()));
//      } else {
//        andStatement.must(rangeQuery(request.getSearchValue().getSign(), searchField,
//            request.getSearchValue().getValue()));
//      }
//    } else {
//      throw new ConflictException("Unexpected field type: " + request.getFieldType());
//    }
//    return andStatement;
  }

  private AbstractQueryBuilder<?> shouldStatement(EntityFilterRequest request, String hierarchyId)
      throws ParseException {

    BoolQueryBuilder shouldStatement = QueryBuilders.boolQuery();
    String searchField;
//    if (request.getFieldType().equals("ImportedData")) {
    if (request.getValueType().equals(EntityTagDataTypes.DATE)) {
      prepareDate(request);
    }
    searchField = "metadata.";

    shouldStatement.must(
        QueryBuilders.matchQuery(searchField.concat("tag"), request.getTag()));
    BoolQueryBuilder orStatement = QueryBuilders.boolQuery();
    for (SearchValue value : request.getValues()) {
      if (request.getValueType().equals(STRING)) {
        orStatement.should(
            QueryBuilders.matchQuery(searchField.concat("value"), value.getValue()));
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
//    } else if (request.getFieldType().equals("core")) {
//      CoreField coreField = coreFieldService.getCoreFieldByIdentifier(request.getFieldIdentifier());
//      if (coreField.getValueType().equals(DATE)) {
//        prepareDate(request);
//      }
//      searchField = LookupEntityTypeCodeEnum.LOCATION_CODE.getLookupEntityType().concat(".")
//          .concat(coreField.getField());
//      for (SearchValue value : request.getValues()) {
//        shouldStatement.should(QueryBuilders.matchQuery(searchField, value.getValue()));
//      }
//      return shouldStatement;
//    } else {
//      throw new ConflictException("Unexpected field type: " + request.getFieldType());
//    }
  }

  private AbstractQueryBuilder<?> rangeStatement(EntityFilterRequest request, String hierarchyId)
      throws ParseException {
    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

    String searchField;
    if (request.getRange().getMaxValue() != null && request.getRange().getMinValue() != null) {
//      if (request.getFieldType().equals("tag")) {
      if (request.getValueType().equals(DATE)) {
        prepareDate(request);
      }

      searchField = "metadata.";

      boolQuery.must(
          QueryBuilders.matchQuery(searchField.concat("tag"), request.getTag()));
      String searchFieldName = "value";
      if (request.getValueType().equals(INTEGER) || request.getValueType().equals(DOUBLE)) {
        searchFieldName = "valueNumber";
      }
      boolQuery.must(QueryBuilders.rangeQuery(searchField.concat(searchFieldName))
          .lte(request.getRange().getMaxValue())
          .gte(request.getRange().getMinValue()));
      return QueryBuilders.nestedQuery("metadata", boolQuery,
          ScoreMode.None);
//      } else if (request.getFieldType().equals("core")) {
//        CoreField coreField = coreFieldService.getCoreFieldByIdentifier(
//            request.getFieldIdentifier());
//        if (coreField.getValueType().equals(DATE)) {
//          prepareDate(request);
//        }
//        searchField = LookupEntityTypeCodeEnum.LOCATION_CODE.getLookupEntityType().concat(".")
//            .concat(coreField.getField());
//        boolQuery.must(QueryBuilders.rangeQuery(searchField)
//            .lte(request.getRange().getMaxValue())
//            .gte(request.getRange().getMinValue()));
//      } else {
//        throw new ConflictException("Unexpected field type: " + request.getFieldType());
//      }
    } else {
      throw new ConflictException("Must set min and max value for range.");
    }

//    return boolQuery;
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

  public static List<Set<String>> splitArray(Set<String> inputArray, int chunkSize) {
    return IntStream.iterate(0, i -> i + chunkSize)
        .limit((int) Math.ceil((double) inputArray.size() / chunkSize)).mapToObj(j -> new HashSet<>(
            Arrays.asList(Arrays.copyOfRange(inputArray.toArray(new String[0]), j,
                Math.min(inputArray.size(), j + chunkSize))))).collect(Collectors.toList());
  }
}

@Setter
@Getter
@AllArgsConstructor
class LocationMetadatHolder {

  private UUID identifier;
  private List<String> ancestors;
  private List<EntityMetadataResponse> entityMetadataResponses;
  private Map<String, Object> aggregates;
}

@Setter
@Getter
@AllArgsConstructor
class IndividualAggregateHelper {


  private Integer index;
  private EntityMetadataResponse entityMetadataResponse;

}

@Setter
@Getter
@AllArgsConstructor
class IndividualAggregateHelperType {

  private Integer index;
  private String type;

}
