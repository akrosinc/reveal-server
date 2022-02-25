package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.PersonRequest.Name;
import com.revealprecision.revealserver.api.v1.dto.response.Group;
import com.revealprecision.revealserver.api.v1.dto.response.PersonResponse;
import com.revealprecision.revealserver.api.v1.dto.response.PersonResponse.PersonResponseBuilder;
import com.revealprecision.revealserver.enums.GenderEnum;
import com.revealprecision.revealserver.enums.NameUseEnum;
import com.revealprecision.revealserver.persistence.domain.Person;
import java.time.ZoneId;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PersonResponseFactory {


  public static PersonResponse fromEntity(Person person) {

    PersonResponseBuilder builder = getPersonResponseBuilder(
        person);

    if (!person.getGroups().isEmpty()) {
      var groups = person.getGroups()
          .stream().map(group ->
              Group.builder().identifier(group
                      .getIdentifier())
                  .name(group.getName())
                  .type(group.getType())
                  .build())
          .collect(Collectors.toSet());
      builder.groups(groups);
    }

    return builder.build();
  }

  public static PersonResponse fromCount(Long count) {
    return PersonResponse.builder().count(count).build();
  }

  public static PersonResponseBuilder getPersonResponseBuilder(Person person) {
    return PersonResponse.builder()
        .identifier(person.getIdentifier())
        .name(
            Name.builder()
                .family(person.getNameFamily())
                .given(person.getNameGiven())
                .text(person.getNameText())
                .prefix(person.getNamePrefix())
                .use(NameUseEnum.getEnum(person.getNameUse()))
                .suffix(person.getNameSuffix())
                .build())
        .active(person.isActive())
        .meta(person.getPersonMetadata().getEntityValue())
        .birthDate(person.getBirthDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
        .gender(GenderEnum.getEnum(person.getGender()));

  }


}
