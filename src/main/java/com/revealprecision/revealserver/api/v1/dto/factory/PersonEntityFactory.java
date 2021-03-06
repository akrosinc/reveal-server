package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.PersonRequest;
import com.revealprecision.revealserver.persistence.domain.Person;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PersonEntityFactory {

  public static Person fromRequestObj(PersonRequest personRequest) {
    LocalDate birthDate = personRequest.getBirthDate();
    UUID identifier = personRequest.getIdentifier();
    return Person.builder().identifier(identifier).nameFamily(personRequest.getName().getFamily())
        .nameGiven(personRequest.getName().getGiven())
        .namePrefix(personRequest.getName().getPrefix())
        .nameSuffix(personRequest.getName().getSuffix()).nameText(personRequest.getName().getText())
        .nameUse(personRequest.getName().getUse().name()).birthDate(birthDate!=null? Date.from(
            birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null)
        .gender(personRequest.getGender().name()).active(personRequest.isActive()).build();
  }

}
