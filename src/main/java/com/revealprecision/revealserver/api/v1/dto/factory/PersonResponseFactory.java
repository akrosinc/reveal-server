package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.GroupResponse;
import com.revealprecision.revealserver.api.v1.dto.response.PersonResponse;
import com.revealprecision.revealserver.enums.GroupTypeEnum;
import com.revealprecision.revealserver.persistence.domain.Group;
import com.revealprecision.revealserver.persistence.domain.Person;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PersonResponseFactory {


  public static PersonResponse fromEntity(Person person) {
    return PersonResponse.builder()
            .identifier(person.getIdentifier())
//            .name(person.getName())
//            .type(GroupTypeEnum.valueOf(person.getType()))
            .build();
  }


}
