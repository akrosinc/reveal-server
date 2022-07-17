package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.LookupEntityTagResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.DataFilterRequest;
import com.revealprecision.revealserver.api.v1.dto.request.EntityTagRequest;
import com.revealprecision.revealserver.api.v1.dto.response.EntityTagResponse;
import com.revealprecision.revealserver.api.v1.dto.response.FeatureSetResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LookupEntityTypeResponse;
import com.revealprecision.revealserver.api.v1.dto.response.PersonMainData;
import com.revealprecision.revealserver.persistence.repository.LocationElasticRepository;
import com.revealprecision.revealserver.service.EntityFilterService;
import com.revealprecision.revealserver.service.EntityTagService;
import com.revealprecision.revealserver.service.LookupEntityTypeService;
import io.swagger.v3.oas.annotations.Operation;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/entityTag")
public class EntityTagController {

  private final EntityTagService entityTagService;
  private final EntityFilterService entityFilterService;
  private final LookupEntityTypeService lookupEntityTypeService;
  private final LocationElasticRepository locationElasticRepository;
  private final RestHighLevelClient client;

  @Operation(summary = "Create Tag", description = "Create Tag", tags = {"Entity Tags"})
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<EntityTagResponse> createTag(
      @Valid @RequestBody EntityTagRequest entityTagRequest) {

    return ResponseEntity.status(HttpStatus.CREATED).body(
        EntityTagResponseFactory.fromEntity(entityTagService.createEntityTag(entityTagRequest)));
  }


  @Operation(summary = "Get All Entity Tags", description = "Get All Entity Tags", tags = {
      "Entity Tags"})
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Page<EntityTagResponse>> getAll(Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(EntityTagResponseFactory.fromEntityPage(entityTagService.getAllEntityTags(pageable)));
  }

  @GetMapping("/{entityTypeIdentifier}")
  public ResponseEntity<List<EntityTagResponse>> getTagsByEntityType(@PathVariable UUID entityTypeIdentifier) {
    return ResponseEntity.status(HttpStatus.OK).body(entityTagService.getTagsAndCoreFields(entityTypeIdentifier));
  }

  @GetMapping("/entityType")
  public ResponseEntity<List<LookupEntityTypeResponse>> getEntityTypes(){
    return ResponseEntity.status(HttpStatus.OK).body(lookupEntityTypeService.getAllLookUpEntityTypes().stream().map(
        LookupEntityTagResponseFactory::fromEntity).collect(Collectors.toList()));
  }

  @PostMapping("/filter")
  public ResponseEntity<FeatureSetResponse> filterEntities(@Valid @RequestBody DataFilterRequest request)
      throws IOException, ParseException {
    return ResponseEntity.ok().body(entityFilterService.filterEntites(request));
  }

  @GetMapping("/person/{personIdentifier}")
  public ResponseEntity<PersonMainData> getPersonDetails(@PathVariable UUID personIdentifier)
      throws IOException {
    return ResponseEntity.ok().body(entityFilterService.getPersonsDetails(personIdentifier));
  }

  @GetMapping("download/{entityTypeIdentifier}")
  public ResponseEntity<?> downloadEntityTags(@PathVariable UUID entityTypeIdentifier) {

  }
}
