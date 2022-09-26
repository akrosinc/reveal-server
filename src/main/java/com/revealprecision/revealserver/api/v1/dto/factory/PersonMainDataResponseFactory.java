package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.PersonCoreFields;
import com.revealprecision.revealserver.api.v1.dto.response.PersonMainData;
import com.revealprecision.revealserver.api.v1.dto.response.EntityMetadataResponse;
import com.revealprecision.revealserver.persistence.es.PersonElastic;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PersonMainDataResponseFactory {

  public static PersonMainData fromPersonElasticSummary(PersonElastic personElastic) {
    return PersonMainData.builder()
        .coreFields(PersonCoreFields.builder()
//            .identifier(personElastic.getIdentifier())
//            .firstName(personElastic.getNameText())
//            .lastName(personElastic.getNameFamily())
            .build())
        .build();
  }

  public static PersonMainData fromPersonElastic(PersonElastic personElastic) {
//    List<EntityMetadataResponse> personMetadata = personElastic.getMetadata()
//        .stream()
//        .map(pm -> new EntityMetadataResponse(pm.getValue(), pm.getType()))
//        .collect( Collectors.toList());
//    return PersonMainData.builder()
//        .coreFields(PersonCoreFields.builder()
//          .identifier(personElastic.getIdentifier())
//          .firstName(personElastic.getNameText())
//          .lastName(personElastic.getNameFamily())
//          .birthDate(personElastic.getBirthDate())
//          .birthDateApprox(personElastic.isBirthDateApprox())
//          .gender(personElastic.getGender())
//          .build())
//        .metadata(personMetadata)
//        .build();
    return null;
  }
}
