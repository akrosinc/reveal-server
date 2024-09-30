package com.revealprecision.revealserver.api.v1.facade.controller;

import com.revealprecision.revealserver.api.v1.controller.TestController.HdssCompoundHouseholdProjection;
import com.revealprecision.revealserver.api.v1.controller.TestController.HdssCompoundProjection;
import com.revealprecision.revealserver.api.v1.controller.TestController.HdssHouseholdIndividualProjection;
import com.revealprecision.revealserver.api.v1.controller.TestController.HdssHouseholdStructureProjection;
import com.revealprecision.revealserver.api.v1.controller.TestController.HdssIndividualProjection;
import com.revealprecision.revealserver.api.v1.facade.models.HdssCompoundObj;
import com.revealprecision.revealserver.api.v1.facade.models.HdssCompoundObj.HdssCompound;
import com.revealprecision.revealserver.api.v1.facade.models.HdssCompoundObj.HdssCompoundHousehold;
import com.revealprecision.revealserver.api.v1.facade.models.HdssCompoundObj.HdssHouseholdIndividual;
import com.revealprecision.revealserver.api.v1.facade.models.HdssCompoundObj.HdssHouseholdStructure;
import com.revealprecision.revealserver.api.v1.facade.models.HdssCompoundObj.HdssIndividual;
import com.revealprecision.revealserver.api.v1.facade.request.HdssSyncRequest;
import com.revealprecision.revealserver.persistence.repository.HdssCompoundsRepository;
import java.util.List;
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

    List<HdssCompoundProjection> allCompounds = compoundsRepository.getAllCompoundsForUserAssignment(hdssSyncRequest.getUserId());

    List<String> compounds = allCompounds.stream().map(HdssCompoundProjection::getCompoundId)
        .collect(Collectors.toList());

    List<HdssCompoundHouseholdProjection> compoundHouseHolds = compoundsRepository.getCompoundHouseHoldsByCompoundIdIn(compounds);

    List<HdssHouseholdIndividualProjection> allHouseholdIndividual = compoundsRepository.getAllHouseholdIndividualByCompoundIdIn(compounds);

    List<HdssHouseholdStructureProjection> hdssHouseholdStructureProjections = compoundsRepository.getAllHouseholdStructuresByCompoundIdIn(compounds);

    List<HdssIndividualProjection> allIndividuals = compoundsRepository.getAllIndividualsByCompoundIdIn(compounds);

    return ResponseEntity.ok(HdssCompoundObj.builder()
        .allCompounds(allCompounds.stream().map(compound -> HdssCompound.builder()
            .compoundId(compound.getCompoundId()).build()).collect(Collectors.toList()))
        .compoundHouseHolds(
            compoundHouseHolds.stream().map(compoundHousehold -> HdssCompoundHousehold.builder()
                .compoundId(compoundHousehold.getCompoundId())
                .householdId(compoundHousehold.getHouseholdId())
                .build()).collect(Collectors.toList()))
        .allHouseholdIndividual(
            allHouseholdIndividual.stream().map(household -> HdssHouseholdIndividual
                    .builder()
                    .householdId(household.getHouseholdId())
                    .individualId(household.getIndividualId())
                    .build())
                .collect(Collectors.toList()))
        .allHouseholdStructure(
            hdssHouseholdStructureProjections.stream().map(hdssHouseholdStructureProjection ->
                HdssHouseholdStructure
                    .builder()
                    .structureId(hdssHouseholdStructureProjection.getStructureId())
                    .householdId(hdssHouseholdStructureProjection.getHouseholdId())
                    .build()).collect(Collectors.toList()))
        .allIndividuals(allIndividuals.stream().map(hdssIndividualProjection -> HdssIndividual
            .builder()
            .identifier(hdssIndividualProjection.getId())
            .individualId(hdssIndividualProjection.getIndividualId())
            .dob(hdssIndividualProjection.getDob().toString())
            .gender(hdssIndividualProjection.getGender())
            .build()).collect(Collectors.toList()))
        .build());

  }

}
