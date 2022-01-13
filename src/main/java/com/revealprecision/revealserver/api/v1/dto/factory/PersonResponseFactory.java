package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.PersonRequest.Gender;
import com.revealprecision.revealserver.api.v1.dto.request.PersonRequest.Name;
import com.revealprecision.revealserver.api.v1.dto.request.PersonRequest.Use;
import com.revealprecision.revealserver.api.v1.dto.response.Group;
import com.revealprecision.revealserver.api.v1.dto.response.GroupResponse;
import com.revealprecision.revealserver.api.v1.dto.response.PersonResponse;
import com.revealprecision.revealserver.api.v1.dto.response.PersonResponse.PersonResponseBuilder;
import com.revealprecision.revealserver.enums.GroupTypeEnum;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.domain.PersonGroup;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PersonResponseFactory {


  public static PersonResponse fromEntity(Person person) {

    PersonResponseBuilder builder = getPersonResponseBuilder(
        person);

    if (!person.getPersonGroups().isEmpty()) {
      var groups = person.getPersonGroups()
          .stream().map(personGroup ->
              Group.builder().identifier(personGroup
                  .getGroup().getIdentifier())
                  .name(personGroup.getGroup().getName())
                  .type(personGroup.getGroup().getType())
                  .build())
          .collect(Collectors.toList());
      builder.groups(groups);
    }

    return builder.build();
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
                .use(Use.valueOf(person.getNameUse()))
                .suffix(person.getNameSuffix())
                .build())
        .active(person.isActive())
        .birthDate(person.getBirthDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
        .gender(Gender.valueOf(person.getGender()));

  }


}
