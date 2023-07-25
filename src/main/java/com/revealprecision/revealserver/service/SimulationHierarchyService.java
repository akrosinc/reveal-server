package com.revealprecision.revealserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.dto.request.SaveHierarchyLocationRequest;
import com.revealprecision.revealserver.api.v1.dto.request.SaveHierarchyRequest;
import com.revealprecision.revealserver.api.v1.dto.response.SaveHierarchyResponse;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.GeneratedHierarchyEvent;
import com.revealprecision.revealserver.messaging.message.GeneratedHierarchyMetadataEvent;
import com.revealprecision.revealserver.persistence.domain.aggregation.GeneratedHierarchy;
import com.revealprecision.revealserver.persistence.domain.aggregation.GeneratedHierarchyMetadata;
import com.revealprecision.revealserver.persistence.domain.aggregation.GeneratedLocationRelationship;
import com.revealprecision.revealserver.persistence.repository.GeneratedHierarchyMetadataRepository;
import com.revealprecision.revealserver.persistence.repository.GeneratedHierarchyRepository;
import com.revealprecision.revealserver.persistence.repository.GeneratedLocationRelationshipRepository;
import com.revealprecision.revealserver.props.KafkaProperties;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Profile("Elastic")
@RequiredArgsConstructor
@Transactional
public class SimulationHierarchyService {

  private final GeneratedHierarchyRepository generatedHierarchyRepository;

  private final GeneratedLocationRelationshipRepository generatedLocationRelationshipRepository;

  private final ObjectMapper objectMapper;

  private final GeneratedHierarchyMetadataRepository generatedHierarchyMetadataRepository;

  private final PublisherService publisherService;

  private final KafkaProperties kafkaProperties;

  public SaveHierarchyResponse saveSimulationHierarchy(SaveHierarchyRequest saveHierarchyRequest) {

    Optional<GeneratedHierarchy> generatedHierarchyByName = generatedHierarchyRepository.findGeneratedHierarchyByName(
        saveHierarchyRequest.getName());

    GeneratedHierarchy generatedHierarchy;

    if (generatedHierarchyByName.isPresent()) {
      generatedHierarchy = generatedHierarchyByName.get();

    } else {
      generatedHierarchy = new GeneratedHierarchy();
      generatedHierarchy.setName(saveHierarchyRequest.getName());
    }
    generatedHierarchy.setNodeOrder(saveHierarchyRequest.getSelectedHierarchy().getNodeOrder());
    generatedHierarchy.setEntityQuery(
        objectMapper.valueToTree(saveHierarchyRequest.getSubmitSimulationRequestData()));
    GeneratedHierarchy generatedHierarchySaved = generatedHierarchyRepository.save(
        generatedHierarchy);

    List<String> locationsRequested = saveHierarchyRequest.getMapdata().stream()
        .map(SaveHierarchyLocationRequest::getIdentifier).collect(
            Collectors.toList());

    deleteExistingLocationRelationshipsForIncomingGeneratedHierarchy(generatedHierarchySaved, locationsRequested);

    List<GeneratedLocationRelationship> generatedLocationRelationships = saveHierarchyRequest.getMapdata()
        .stream()
        .map(saveHierarchyLocationRequest ->
            GeneratedLocationRelationship.builder()
                .generatedHierarchy(generatedHierarchySaved)
                .locationIdentifier(saveHierarchyLocationRequest.getIdentifier())
                .parentIdentifier(saveHierarchyLocationRequest.getProperties().getParent())
                .ancestry(saveHierarchyLocationRequest.getProperties().getAncestry())
                .build()
        ).collect(Collectors.toList());

    List<GeneratedLocationRelationshipEvent> generatedLocationRelationshipEvents = saveHierarchyRequest.getMapdata()
        .stream()
        .map(saveHierarchyLocationRequest ->
            GeneratedLocationRelationshipEvent.builder()
                .generatedHierarchy(generatedHierarchySaved)
                .locationIdentifier(saveHierarchyLocationRequest.getIdentifier())
                .parentIdentifier(saveHierarchyLocationRequest.getProperties().getParent())
                .ancestry(saveHierarchyLocationRequest.getProperties().getAncestry())
                .geographicLevelNumber(saveHierarchyLocationRequest.getProperties().getGeographicLevelNumber())
                .build()
        ).collect(Collectors.toList());

    List<GeneratedLocationRelationship> generatedLocationRelationshipsSaved = generatedLocationRelationshipRepository.saveAll(
        generatedLocationRelationships);

    Map<String, GeneratedLocationRelationship> relationshipsByLocationMap = generatedLocationRelationshipsSaved.stream()
        .collect(Collectors.toMap(GeneratedLocationRelationship::getLocationIdentifier, a -> a,
            (a, b) -> b));

    Map<String, GeneratedLocationRelationshipEvent> relationshipsByLocationEventsMap = generatedLocationRelationshipEvents.stream()
        .collect(Collectors.toMap(GeneratedLocationRelationshipEvent::getLocationIdentifier, a -> a,
            (a, b) -> b));

    List<GeneratedHierarchyMetadata> generatedHierarchyMetadata = saveMetadata(saveHierarchyRequest,
        generatedHierarchySaved);

    submitToMessaging(generatedHierarchyMetadata,relationshipsByLocationEventsMap);

    return SaveHierarchyResponse.builder().name(generatedHierarchySaved.getName())
        .nodeOrder(generatedHierarchySaved.getNodeOrder())
        .identifier(generatedHierarchy.getId()).build();
  }


  public void deleteExistingLocationRelationshipsForIncomingGeneratedHierarchy(
      GeneratedHierarchy generatedHierarchySaved,
      List<String> locationsRequested) {
    generatedLocationRelationshipRepository.deleteGeneratedLocationRelationshipByGeneratedHierarchy_IdAndLocationIdentifierIn(
        generatedHierarchySaved.getId(), locationsRequested);
  }

  private void submitToMessaging(List<GeneratedHierarchyMetadata> generatedHierarchyMetadataList,
      Map<String, GeneratedLocationRelationshipEvent> relationshipsByLocationEventsMap) {

    generatedHierarchyMetadataList.forEach(generatedHierarchyMetadata ->
        publisherService.send(
            kafkaProperties.getTopicMap().get(KafkaConstants.LOCATION_METADATA_UPDATE),
            GeneratedHierarchyMetadataEvent.builder()
                .id(generatedHierarchyMetadata.getId())
                .generatedHierarchy(GeneratedHierarchyEvent.builder()
                    .id(String.valueOf(generatedHierarchyMetadata.getGeneratedHierarchy().getId()))
                    .entityQuery(
                        generatedHierarchyMetadata.getGeneratedHierarchy().getEntityQuery())
                    .nodeOrder(generatedHierarchyMetadata.getGeneratedHierarchy().getNodeOrder())
                    .name(generatedHierarchyMetadata.getGeneratedHierarchy().getName())
                    .build())
                .fieldType(generatedHierarchyMetadata.getFieldType())
                .value(generatedHierarchyMetadata.getValue())
                .locationIdentifier(generatedHierarchyMetadata.getLocationIdentifier())
                .tag(generatedHierarchyMetadata.getTag())
                .ancestry(relationshipsByLocationEventsMap.get(generatedHierarchyMetadata.getLocationIdentifier()).getAncestry())
                .geographicLevelNumber(relationshipsByLocationEventsMap.get(generatedHierarchyMetadata.getLocationIdentifier()).getGeographicLevelNumber())
                .parent(relationshipsByLocationEventsMap.get(generatedHierarchyMetadata.getLocationIdentifier()).getParentIdentifier())
                .build()
        )
    );

  }


  private List<GeneratedHierarchyMetadata> saveMetadata(SaveHierarchyRequest saveHierarchyRequest,
      GeneratedHierarchy generatedHierarchySaved) {
    List<GeneratedHierarchyMetadata> generatedHierarchyMetadata = saveHierarchyRequest.getMapdata()
        .stream()
        .flatMap(saveHierarchyLocationRequest ->
            saveHierarchyLocationRequest.getProperties().getMetadata().stream().map(metadata ->
                GeneratedHierarchyMetadata.builder()
                    .tag(metadata.getType())
                    .value(metadata.getValue() instanceof String ? Double.parseDouble(
                        (String) metadata.getValue())
                        : metadata.getValue() instanceof Integer
                            ? ((Integer) metadata.getValue()).doubleValue()
                            : (Double) metadata.getValue())
                    .fieldType(metadata.getFieldType())
                    .generatedHierarchy(generatedHierarchySaved)
                    .locationIdentifier(saveHierarchyLocationRequest.getIdentifier())
                    .build()

            )
        ).collect(Collectors.toList());

    return generatedHierarchyMetadataRepository.saveAll(generatedHierarchyMetadata);
  }

  public List<GeneratedHierarchy> generatedHierarchies(){

    return generatedHierarchyRepository.findAll();
  }
}

@Setter @Getter
@Builder
class GeneratedLocationRelationshipEvent {

  private int id;

  private String locationIdentifier;

  private String parentIdentifier;

  private List<String> ancestry;

  private GeneratedHierarchy generatedHierarchy;

  private Integer geographicLevelNumber;

}
