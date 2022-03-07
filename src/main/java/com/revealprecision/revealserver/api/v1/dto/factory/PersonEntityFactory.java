package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.PersonRequest;
import com.revealprecision.revealserver.persistence.domain.Person;
import java.time.ZoneId;
import java.util.Date;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PersonEntityFactory {

  public static Person fromRequestObj(PersonRequest personRequest) {
    return Person.builder().nameFamily(personRequest.getName().getFamily())
        .nameGiven(personRequest.getName().getGiven())
        .namePrefix(personRequest.getName().getPrefix())
        .nameSuffix(personRequest.getName().getSuffix()).nameText(personRequest.getName().getText())
        .nameUse(personRequest.getName().getUse().name()).birthDate(Date.from(
            personRequest.getBirthDate().atStartOfDay(ZoneId.systemDefault()).toInstant()))
        .gender(personRequest.getGender().name()).active(personRequest.isActive()).build();
  }

}
