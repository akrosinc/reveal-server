package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.PersonName;
import com.revealprecision.revealserver.api.v1.dto.response.Group;
import com.revealprecision.revealserver.api.v1.dto.response.PersonResponse;
import com.revealprecision.revealserver.api.v1.dto.response.PersonResponse.PersonResponseBuilder;
import com.revealprecision.revealserver.enums.GenderEnum;
import com.revealprecision.revealserver.enums.LookupUtil;
import com.revealprecision.revealserver.enums.NameUseEnum;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.domain.PersonMetadata;
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
    PersonMetadata personMetadata = person.getPersonMetadata();
    Date birthDate = person.getBirthDate();
    return PersonResponse.builder()
        .identifier(person.getIdentifier())
        .name(
            PersonName.builder()
                .family(person.getNameFamily())
                .given(person.getNameGiven())
                .text(person.getNameText())
                .prefix(person.getNamePrefix())
                .use(LookupUtil.lookup(NameUseEnum.class, person.getNameUse()))
                .suffix(person.getNameSuffix())
                .build())
        .active(person.isActive())
        .meta(personMetadata != null ? personMetadata.getEntityValue() : null)
        .birthDate(
            birthDate != null ? birthDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                : null)
        .gender(LookupUtil.lookup(GenderEnum.class, person.getGender()));

  }


}
