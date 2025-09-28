package com.revealprecision.revealserver.api.v1.facade.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.facade.models.HdssCompoundHouseholdIndividualObj;
import com.revealprecision.revealserver.api.v1.facade.models.HdssCompoundHouseholdIndividualPushObj;
import com.revealprecision.revealserver.api.v1.facade.models.HdssCompoundObj;
import com.revealprecision.revealserver.api.v1.facade.models.HdssCompoundObj.HdssCompound;
import com.revealprecision.revealserver.api.v1.facade.models.HdssCompoundObj.HdssCompoundHousehold;
import com.revealprecision.revealserver.api.v1.facade.models.HdssCompoundObj.HdssHousehold;
import com.revealprecision.revealserver.api.v1.facade.models.HdssCompoundObj.HdssHouseholdIndividual;
import com.revealprecision.revealserver.api.v1.facade.models.HdssCompoundObj.HdssHouseholdStructure;
import com.revealprecision.revealserver.api.v1.facade.models.HdssCompoundObj.HdssIndividual;
import com.revealprecision.revealserver.api.v1.facade.request.HdssSearchRequest;
import com.revealprecision.revealserver.api.v1.facade.request.HdssSyncRequest;
import com.revealprecision.revealserver.persistence.domain.Fields;
import com.revealprecision.revealserver.persistence.domain.HdssCompounds;
import com.revealprecision.revealserver.persistence.projection.HdssCompoundHouseholdIndividualProjection;
import com.revealprecision.revealserver.persistence.repository.HdssCompoundsRepository;
import com.revealprecision.revealserver.service.HdssSearchService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.csveed.row.RowInstructionsImpl;
import org.csveed.row.RowWriter;
import org.csveed.row.RowWriterImpl;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/hdss")
@Slf4j
@RequiredArgsConstructor
public class HdssFacadeController {

  public static final String TOTAL_RECORDS = "total_records";

  private final HdssCompoundsRepository compoundsRepository;

  private final HdssSearchService hdssSearchService;

  @ResponseStatus(HttpStatus.OK)
  @PostMapping(value = "/sync", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Transactional
  public ResponseEntity<HdssCompoundObj> taskSync(@RequestBody HdssSyncRequest hdssSyncRequest) {

    long serverVersion = hdssSyncRequest.getServerVersion();

    int count = compoundsRepository.getTotalCountOfCompoundsForUserAssignmentAndServerVersionAndBatchSize(
        hdssSyncRequest.getUserId());

    List<HdssCompoundHouseholdIndividualProjection> individuals = compoundsRepository
        .getAllCompoundsForUserAssignmentAndServerVersionAndBatchSize(
            hdssSyncRequest.getUserId(), serverVersion, hdssSyncRequest.getBatchSize());

    if (!individuals.isEmpty()) {
      Optional<Long> maxServerVersion = individuals.stream()
          .map(HdssCompoundHouseholdIndividualProjection::getServerVersion).reduce(Long::max);

      HdssCompoundObj compounds = HdssCompoundObj.builder()
          .allCompounds(individuals.stream()
              .filter(individual->individual.getStructureId()!=null)
              .map(individual -> HdssCompound.builder().serverVersion(individual.getServerVersion())
                  .compoundId(individual.getCompoundId()).build()).collect(Collectors.toSet()))
          .compoundHouseHolds(individuals.stream()
              .filter(individual->individual.getCompoundId()!=null)
              .map(individual -> HdssCompoundHousehold.builder().compoundId(individual.getCompoundId())
                  .serverVersion(individual.getServerVersion())
                  .householdId(individual.getHouseholdId()).build()).collect(Collectors.toSet()))
          .allHouseholdIndividual(individuals.stream()
              .filter(individual->individual.getHouseholdId()!=null )
              .map(individual -> HdssHouseholdIndividual.builder()
                  .serverVersion(individual.getServerVersion())
                  .individualId(individual.getIndividualId())
                  .householdId(individual.getHouseholdId()).build()).collect(Collectors.toSet()))
          .allHouseholdStructure(individuals.stream()
              .filter(individual->individual.getStructureId()!=null)
              .map(individual -> HdssHouseholdStructure.builder()
                  .serverVersion(individual.getServerVersion())
                  .structureId(individual.getStructureId()).householdId(individual.getHouseholdId())
                  .build())
              .collect(Collectors.toSet()))
          .allIndividuals(individuals.stream().map(
              individual -> HdssIndividual.builder().identifier(individual.getId())
                  .individualId(individual.getIndividualId()).name(individual.getName())
                  .serverVersion(individual.getServerVersion()).dob(individual.getDob())
                  .gender(individual.getGender())
                  .cluster(individual.getCluster())
                  .floatingLocationGeographicLevel(individual.getFloatingLocationGeographicLevel())
                  .floatingLocationId(individual.getFloatingLocationId())
                  .floatingLocationName(individual.getFloatingLocationName()).build())
              .collect(Collectors.toSet()))
          .allHouseholds(individuals.stream().map(individual -> HdssHousehold
              .builder()
              .householdId(individual.getHouseholdId())
              .floatingHouseholdLocationName(individual.getFloatingHouseholdLocationName())
              .serverVersion(individual.getServerVersion())
              .build()).collect(Collectors.toSet()))
          .allHouseholdIndividualToDelete(individuals.stream()
              .filter(individual->individual.getStructureId()==null && individual.getHouseholdId()==null)
              .map(HdssCompoundHouseholdIndividualProjection::getIndividualId).collect(Collectors.toSet()))
          .allCompoundHouseholdToDelete(individuals.stream()
              .filter(individual->individual.getCompoundId()==null && individual.getStructureId()==null)
              .map(HdssCompoundHouseholdIndividualProjection::getHouseholdId).collect(Collectors.toSet()))
          .serverVersion(maxServerVersion.isPresent() ? maxServerVersion.get() : 0)
          .totalRecords(count).isEmpty(false).build();

      return ResponseEntity.ok(compounds);
    } else {
      return ResponseEntity.ok(HdssCompoundObj.builder().totalRecords(count).isEmpty(true).build());
    }
  }


  @PostMapping(value = "/search", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<HdssCompoundHouseholdIndividualObj>> search(
      @RequestBody HdssSearchRequest hdssSearchRequest) throws ParseException {
    List<HdssCompoundHouseholdIndividualObj> hdssCompoundHouseholdIndividualObjs = hdssSearchService.searchHdssCompounds(
        hdssSearchRequest);
    if (hdssCompoundHouseholdIndividualObjs != null) {
      return ResponseEntity.ok(hdssCompoundHouseholdIndividualObjs);
    } else {
      return ResponseEntity.ok().build();
    }
  }



  @ResponseStatus(HttpStatus.OK)
  @PostMapping(value = "/addOrUpdate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Transactional
  public ResponseEntity<HdssCompoundObj> taskSync(
      @RequestBody List<HdssCompoundHouseholdIndividualPushObj> hdssCompoundHouseholdIndividualObjs) {

    List<String> individualIds = hdssCompoundHouseholdIndividualObjs.stream().map(
        HdssCompoundHouseholdIndividualPushObj::getIndividualId).collect(
        Collectors.toList());

    List<HdssCompounds> allByIndividualIdIn = compoundsRepository.findAllByIndividualIdIn(
        individualIds);

    Map<String, HdssCompounds> existing = allByIndividualIdIn.stream()
        .collect(
            Collectors.toMap(HdssCompounds::getIndividualId, i -> i,
                (a, b) -> a));

    List<HdssCompounds> collect = hdssCompoundHouseholdIndividualObjs.stream()
        .map(hdssCompoundHouseholdIndividualPushObj ->

            {

              HdssCompounds item;
              if (existing.containsKey(
                  hdssCompoundHouseholdIndividualPushObj.getIndividualId())) {
                 item = existing.get(
                    hdssCompoundHouseholdIndividualPushObj.getIndividualId());
                if (hdssCompoundHouseholdIndividualPushObj.getCompoundId()!=null){
                  item.setCompoundId(hdssCompoundHouseholdIndividualPushObj.getCompoundId());
                }
                if (hdssCompoundHouseholdIndividualPushObj.getHouseholdId()!=null){
                  item.setHouseholdId(
                      hdssCompoundHouseholdIndividualPushObj.getHouseholdId());
                }
                if (hdssCompoundHouseholdIndividualPushObj.getStructureId()!=null){
                  item.setStructureId(hdssCompoundHouseholdIndividualPushObj.getStructureId());
                }

                long nextServerVersion = compoundsRepository.getNextServerVersion();

                item.setServerVersion(nextServerVersion);
                if (hdssCompoundHouseholdIndividualPushObj.getFloatingLocationName()!=null){
                  item.setFloatingLocationId(hdssCompoundHouseholdIndividualPushObj.getFloatingLocationId());
                  item.setFloatingLocationName(hdssCompoundHouseholdIndividualPushObj.getFloatingLocationName());
                  item.setFloatingLocationGeographicLevel(hdssCompoundHouseholdIndividualPushObj.getFloatingLocationGeographicLevel());
                  item.setCompoundId(null);
                  item.setHouseholdId(null);
                  item.setStructureId(null);
                }

                if (hdssCompoundHouseholdIndividualPushObj.getFloatingHouseholdLocationName()!=null){
                  item.setFloatingHouseholdLocationName(hdssCompoundHouseholdIndividualPushObj.getFloatingHouseholdLocationName());
                  item.setCompoundId(null);
                  item.setStructureId(null);
                }
              } else {
                LocalDate parse = null;
                try {
                  parse = LocalDate.parse(hdssCompoundHouseholdIndividualPushObj.getDob(),
                      DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                } catch (DateTimeParseException e) {
                  try {
                    parse = LocalDate.parse(hdssCompoundHouseholdIndividualPushObj.getDob(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                  } catch (DateTimeParseException pe) {
                    log.error("cannot parse data for Hdss data {}",
                        hdssCompoundHouseholdIndividualPushObj);
                  }
                }

                item = HdssCompounds.builder()
                    .id(UUID.fromString(hdssCompoundHouseholdIndividualPushObj.getIdentifier()))
                    .compoundId(hdssCompoundHouseholdIndividualPushObj.getCompoundId())
                    .serverVersion(hdssCompoundHouseholdIndividualPushObj.getServerVersion())
                    .individualId(hdssCompoundHouseholdIndividualPushObj.getIndividualId())
                    .householdId(hdssCompoundHouseholdIndividualPushObj.getHouseholdId())
                    .structureId(hdssCompoundHouseholdIndividualPushObj.getStructureId())
                    .fields(
                        Fields.builder().gender(hdssCompoundHouseholdIndividualPushObj.getGender())
                            .dob(parse==null?LocalDate.now().toString():parse.toString()).build()).build();

                if (hdssCompoundHouseholdIndividualPushObj.getFloatingLocationName()!=null){
                  item.setFloatingLocationId(hdssCompoundHouseholdIndividualPushObj.getFloatingLocationId());
                  item.setFloatingLocationName(hdssCompoundHouseholdIndividualPushObj.getFloatingLocationName());
                  item.setFloatingLocationGeographicLevel(hdssCompoundHouseholdIndividualPushObj.getFloatingLocationGeographicLevel());
                  item.setCompoundId(null);
                  item.setHouseholdId(null);
                  item.setStructureId(null);
                }
                if (hdssCompoundHouseholdIndividualPushObj.getFloatingHouseholdLocationName()!=null){
                  item.setFloatingHouseholdLocationName(hdssCompoundHouseholdIndividualPushObj.getFloatingHouseholdLocationName());
                  item.setCompoundId(null);
                  item.setStructureId(null);
                }

              }

              return item;
            }

        ).collect(Collectors.toList());

    compoundsRepository.saveAll(collect);

    return ResponseEntity.ok().build();
  }

  @PostMapping("/file")
  public ResponseEntity<Resource> data2(@RequestBody HdssSyncRequest hdssSyncRequest) throws IOException {

    log.info("hdssSyncRequest json: > {} <",new ObjectMapper().writeValueAsString(hdssSyncRequest));
    log.info("hdssSyncRequest: > {} <",hdssSyncRequest);
    log.info("hdssSyncRequest.getUserId(): > {} <",hdssSyncRequest.getUserId());
    List<HdssCompoundHouseholdIndividualProjection> individuals = compoundsRepository
        .getAllCompoundsForUserAssignmentAndServerVersion(
            hdssSyncRequest.getUserId(),0);

    StringWriter stringWriter = new StringWriter();

    RowWriter rowWriter = new RowWriterImpl(stringWriter, new RowInstructionsImpl()
        .setUseHeader(false));

    HdssCompoundObj compounds = HdssCompoundObj.builder()
        .allCompounds(individuals.stream()
            .filter(individual->individual.getStructureId()!=null)
            .map(individual -> HdssCompound.builder().serverVersion(individual.getServerVersion())
                .compoundId(individual.getCompoundId()).build()).collect(Collectors.toSet()))
        .compoundHouseHolds(individuals.stream()
            .filter(individual->individual.getCompoundId()!=null)
            .map(individual -> HdssCompoundHousehold.builder().compoundId(individual.getCompoundId())
                .serverVersion(individual.getServerVersion())
                .householdId(individual.getHouseholdId()).build()).collect(Collectors.toSet()))
        .allHouseholdIndividual(individuals.stream()
            .filter(individual->individual.getHouseholdId()!=null )
            .map(individual -> HdssHouseholdIndividual.builder()
                .serverVersion(individual.getServerVersion())
                .individualId(individual.getIndividualId())
                .householdId(individual.getHouseholdId()).build()).collect(Collectors.toSet()))
        .allHouseholdStructure(individuals.stream()
            .filter(individual->individual.getStructureId()!=null)
            .map(individual -> HdssHouseholdStructure.builder()
                .serverVersion(individual.getServerVersion())
                .structureId(individual.getStructureId()).householdId(individual.getHouseholdId())
                .build())
            .collect(Collectors.toSet()))
        .allIndividuals(individuals.stream().map(
                individual -> HdssIndividual.builder().identifier(individual.getId())
                    .individualId(individual.getIndividualId()).name(individual.getName())
                    .serverVersion(individual.getServerVersion()).dob(individual.getDob())
                    .gender(individual.getGender())
                    .cluster(individual.getCluster())
                    .floatingLocationGeographicLevel(individual.getFloatingLocationGeographicLevel())
                    .floatingLocationId(individual.getFloatingLocationId())
                    .floatingLocationName(individual.getFloatingLocationName()).build())
            .collect(Collectors.toSet()))
        .allHouseholds(individuals.stream().map(individual -> HdssHousehold
            .builder()
            .householdId(individual.getHouseholdId())
            .floatingHouseholdLocationName(individual.getFloatingHouseholdLocationName())
            .serverVersion(individual.getServerVersion())
            .build()).collect(Collectors.toSet()))
        .allHouseholdIndividualToDelete(individuals.stream()
            .filter(individual->individual.getStructureId()==null && individual.getHouseholdId()==null)
            .map(HdssCompoundHouseholdIndividualProjection::getIndividualId).collect(Collectors.toSet()))
        .allCompoundHouseholdToDelete(individuals.stream()
            .filter(individual->individual.getCompoundId()==null && individual.getStructureId()==null)
            .map(HdssCompoundHouseholdIndividualProjection::getHouseholdId).collect(Collectors.toSet())).build();

    Consumer<List<String>> writeRowWithType = (row) -> rowWriter.writeRow(row.toArray(new String[0]));

    for (HdssCompound compound : compounds.getAllCompounds()) {
      writeRowWithType.accept(Arrays.asList(
          "allCompounds",
          safe(compound.getCompoundId()),
          safe(String.valueOf(compound.getServerVersion()))
      ));
    }

// compoundHouseHolds
    for (HdssCompoundHousehold chh : compounds.getCompoundHouseHolds()) {
      writeRowWithType.accept(Arrays.asList(
          "compoundHouseHolds",
          safe(chh.getCompoundId()),
          safe(chh.getHouseholdId()),
          safe(String.valueOf(chh.getServerVersion()))
      ));
    }

// allHouseholdIndividual
    for (HdssHouseholdIndividual hi : compounds.getAllHouseholdIndividual()) {
      writeRowWithType.accept(Arrays.asList(
          "allHouseholdIndividual",
          safe(hi.getIndividualId()),
          safe(hi.getHouseholdId()),
          safe(String.valueOf(hi.getServerVersion()))
      ));
    }

// allHouseholdStructure
    for (HdssHouseholdStructure hs : compounds.getAllHouseholdStructure()) {
      writeRowWithType.accept(Arrays.asList(
          "allHouseholdStructure",
          safe(hs.getStructureId()),
          safe(hs.getHouseholdId()),
          safe(String.valueOf(hs.getServerVersion()))
      ));
    }

// allIndividuals
    for (HdssIndividual i : compounds.getAllIndividuals()) {
      writeRowWithType.accept(Arrays.asList(
          "allIndividuals",
          safe(i.getIdentifier()),
          safe(i.getIndividualId()),
          safe(i.getName()),
          safe(i.getDob()),
          safe(i.getGender()),
          safe(i.getCluster()),
          safe(i.getFloatingLocationId()),
          safe(i.getFloatingLocationName()),
          safe(i.getFloatingLocationGeographicLevel()),
          safe(String.valueOf(i.getServerVersion()))
      ));
    }

// allHouseholds
    for (HdssHousehold h : compounds.getAllHouseholds()) {
      writeRowWithType.accept(Arrays.asList(
          "allHouseholds",
          safe(h.getHouseholdId()),
          safe(h.getFloatingHouseholdLocationName()),
          safe(String.valueOf(h.getServerVersion()))
      ));
    }

// allHouseholdIndividualToDelete
    for (String id : compounds.getAllHouseholdIndividualToDelete()) {
      writeRowWithType.accept(Arrays.asList("allHouseholdIndividualToDelete", safe(id)));
    }

// allCompoundHouseholdToDelete
    for (String id : compounds.getAllCompoundHouseholdToDelete()) {
      writeRowWithType.accept(Arrays.asList("allCompoundHouseholdToDelete", safe(id)));
    }

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("allCompounds", compounds.getAllCompounds().size());
    metadata.put("compoundHouseHolds", compounds.getCompoundHouseHolds().size());
    metadata.put("allHouseholdIndividual", compounds.getAllHouseholdIndividual().size());
    metadata.put("allHouseholdStructure", compounds.getAllHouseholdStructure().size());
    metadata.put("allIndividuals", compounds.getAllIndividuals().size());
    metadata.put("allHouseholds", compounds.getAllHouseholds().size());
    metadata.put("allHouseholdIndividualToDelete", compounds.getAllHouseholdIndividualToDelete().size());
    metadata.put("allCompoundHouseholdToDelete", compounds.getAllCompoundHouseholdToDelete().size());

    String metadataJson = new ObjectMapper().writeValueAsString(metadata);

    stringWriter.close();
    InputStream targetStream = new ByteArrayInputStream(stringWriter.toString().getBytes());

    return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header("Content-disposition", "attachment;filename=" + "user.csv")
        .header("X-File-Metadata",metadataJson)
        .body(new InputStreamResource(targetStream));
  }
  private String safe(String value) {
    return value != null ? value : "";
  }
}
