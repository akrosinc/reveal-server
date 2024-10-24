package com.revealprecision.revealserver.api.v1.facade.controller;

import com.revealprecision.revealserver.api.v1.facade.models.HdssCompoundHouseholdIndividualObj;
import com.revealprecision.revealserver.api.v1.facade.models.HdssCompoundHouseholdIndividualPushObj;
import com.revealprecision.revealserver.api.v1.facade.models.HdssCompoundObj;
import com.revealprecision.revealserver.api.v1.facade.models.HdssCompoundObj.HdssCompound;
import com.revealprecision.revealserver.api.v1.facade.models.HdssCompoundObj.HdssCompoundHousehold;
import com.revealprecision.revealserver.api.v1.facade.models.HdssCompoundObj.HdssHouseholdIndividual;
import com.revealprecision.revealserver.api.v1.facade.models.HdssCompoundObj.HdssHouseholdStructure;
import com.revealprecision.revealserver.api.v1.facade.models.HdssCompoundObj.HdssIndividual;
import com.revealprecision.revealserver.api.v1.facade.request.HdssSearchRequest;
import com.revealprecision.revealserver.api.v1.facade.request.HdssSyncRequest;
import com.revealprecision.revealserver.persistence.domain.Fields;
import com.revealprecision.revealserver.persistence.domain.HdssCompounds;
import com.revealprecision.revealserver.persistence.projection.HdssCompoundHouseholdIndividualProjection;
import com.revealprecision.revealserver.persistence.repository.HdssCompoundsRepository;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  @ResponseStatus(HttpStatus.OK)
  @PostMapping(value = "/sync", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Transactional
  public ResponseEntity<HdssCompoundObj> taskSync(@RequestBody HdssSyncRequest hdssSyncRequest) {

    long serverVersion = hdssSyncRequest.getServerVersion();

    List<HdssCompoundHouseholdIndividualProjection> individuals = compoundsRepository.getAllCompoundsForUserAssignmentAndServerVersionAndBatchSize(
        hdssSyncRequest.getUserId(), serverVersion, hdssSyncRequest.getBatchSize());

    if (!individuals.isEmpty()) {
      Optional<Long> maxServerVersion = individuals.stream()
          .map(HdssCompoundHouseholdIndividualProjection::getServerVersion)
          .reduce(Long::max);

      return ResponseEntity.ok(HdssCompoundObj.builder()
          .allCompounds(individuals.stream()
              .map(individual -> HdssCompound
                  .builder()
                  .serverVersion(individual.getServerVersion())
                  .compoundId(individual.getCompoundId())
                  .build()).collect(Collectors.toSet()))
          .compoundHouseHolds(
              individuals.stream()
                  .map(individual -> HdssCompoundHousehold.builder()
                      .compoundId(individual.getCompoundId())
                      .serverVersion(individual.getServerVersion())
                      .householdId(individual.getHouseholdId())
                      .build())
                  .collect(Collectors.toSet()))
          .allHouseholdIndividual(
              individuals.stream()
                  .map(individual -> HdssHouseholdIndividual
                      .builder()
                      .serverVersion(individual.getServerVersion())
                      .individualId(individual.getIndividualId())
                      .householdId(individual.getHouseholdId())
                      .build()
                  ).collect(Collectors.toSet()))
          .allHouseholdStructure(
              individuals.stream()
                  .map(individual -> HdssHouseholdStructure
                      .builder()
                      .serverVersion(individual.getServerVersion())
                      .structureId(individual.getStructureId())
                      .householdId(individual.getHouseholdId())
                      .build())
                  .collect(Collectors.toSet()))
          .allIndividuals(individuals.stream()
              .map(individual -> HdssIndividual
                  .builder()
                  .identifier(individual.getId())
                  .individualId(individual.getIndividualId())
                  .serverVersion(individual.getServerVersion())
                  .dob(individual.getDob().toString())
                  .gender(individual.getGender())
                  .build()).collect(Collectors.toSet()))
          .serverVersion(maxServerVersion.isPresent() ? maxServerVersion.get() : 0)
          .build());
    } else {
      return ResponseEntity.ok(HdssCompoundObj.builder().isEmpty(true).build());
    }
  }

  @PostMapping(value = "/search", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)

  public ResponseEntity<List<HdssCompoundHouseholdIndividualObj>> search(
      @RequestBody HdssSearchRequest hdssSearchRequest) throws ParseException {
    List<HdssCompoundHouseholdIndividualProjection> individualProjections = null;

    String searchDeterminer = "";

    String searchDate = null;

    if (hdssSearchRequest.getGender() != null) {
      searchDeterminer = "G";
    }
    if (hdssSearchRequest.getDob() != null) {

      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

      // Parse the string into LocalDate
      LocalDate dob = LocalDate.parse(hdssSearchRequest.getDob(), formatter);

      // Extract year, month, and day
      int year = dob.getYear();
      int month = dob.getMonthValue();
      int day = dob.getDayOfMonth();

      // Construct the JSON array format as a string
      searchDate = String.format("[%d, %d, %d]", year, month, day);

      // Construct the JSON array format as a string

      searchDeterminer = searchDeterminer.concat("D");
    }
    if (hdssSearchRequest.getSearchString() != null) {
      searchDeterminer = searchDeterminer.concat("S");
    }

    switch (searchDeterminer) {

      case "G":
        individualProjections = compoundsRepository.searchWithGender(
            hdssSearchRequest.getGender());
        break;
      case "GD":
        individualProjections = compoundsRepository.searchWithGenderAndDob(
            hdssSearchRequest.getGender(), searchDate);
        break;
      case "GDS":
        individualProjections = compoundsRepository.searchWithStringGenderAndDob(
            hdssSearchRequest.getSearchString(), hdssSearchRequest.getGender(),
            hdssSearchRequest.getDob());
        break;
      case "D":
        individualProjections = compoundsRepository.searchWithDob(
            searchDate);
        break;
      case "DS":
        individualProjections = compoundsRepository.searchWithStringAndDob(
            hdssSearchRequest.getSearchString(), searchDate);
        break;
      case "S":
        individualProjections = compoundsRepository.searchWithString(
            hdssSearchRequest.getSearchString());
        break;
      case "GS":
        individualProjections = compoundsRepository.searchWithStringAndGender(
            hdssSearchRequest.getSearchString(), hdssSearchRequest.getGender());
        break;

    }
    if (individualProjections!=null) {
      return ResponseEntity.ok(individualProjections.stream()
          .map(individualProjection ->
              HdssCompoundHouseholdIndividualObj.builder()
                  .compoundId(individualProjection.getCompoundId())
                  .householdId(individualProjection.getHouseholdId())
                  .individualId(individualProjection.getIndividualId())
                  .gender(individualProjection.getGender())
                  .dob(individualProjection.getDob().toString())
                  .id(individualProjection.getId())
                  .build())
          .collect(Collectors.toList()));
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @ResponseStatus(HttpStatus.OK)
  @PostMapping(value = "/add", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Transactional
  public ResponseEntity<HdssCompoundObj> taskSync(
      @RequestBody List<HdssCompoundHouseholdIndividualPushObj> hdssCompoundHouseholdIndividualObjs) {

    List<HdssCompounds> collect = hdssCompoundHouseholdIndividualObjs.stream()
        .map(hdssCompoundHouseholdIndividualPushObj ->

            {
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
              return HdssCompounds.builder()
                  .id(UUID.fromString(hdssCompoundHouseholdIndividualPushObj.getIdentifier()))
                  .compoundId(hdssCompoundHouseholdIndividualPushObj.getCompoundId())
                  .serverVersion(hdssCompoundHouseholdIndividualPushObj.getServerVersion())
                  .individualId(hdssCompoundHouseholdIndividualPushObj.getIndividualId())
                  .householdId(hdssCompoundHouseholdIndividualPushObj.getHouseholdId())
                  .structureId(hdssCompoundHouseholdIndividualPushObj.getStructureId())
                  .fields(Fields.builder()
                      .gender(hdssCompoundHouseholdIndividualPushObj.getGender())
                      .dob(parse)
                      .build())
                  .build();
            }

        ).collect(Collectors.toList());

    compoundsRepository.saveAll(collect);

    return ResponseEntity.ok().build();
  }

}
