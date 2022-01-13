package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.GroupResponse;
import com.revealprecision.revealserver.api.v1.dto.response.GroupResponse.GroupResponseBuilder;
import com.revealprecision.revealserver.api.v1.dto.response.GroupResponse.Relationships;
import com.revealprecision.revealserver.api.v1.dto.response.PersonResponse;
import com.revealprecision.revealserver.api.v1.dto.response.UserResponse;
import com.revealprecision.revealserver.enums.GroupTypeEnum;
import com.revealprecision.revealserver.persistence.domain.Group;
import com.revealprecision.revealserver.persistence.domain.User;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GroupResponseFactory {


  public static GroupResponse fromEntity(Group group,boolean includeRelationships) {
    GroupResponseBuilder groupResponseBuilder = GroupResponse.builder()
        .identifier(group.getIdentifier())
        .name(group.getName())
        .type(GroupTypeEnum.valueOf(group.getType()))
        .locationIdentifier(group.getLocation() == null ?null:group.getLocation().getIdentifier());

    if (includeRelationships){
      var person = group.getPersons().stream().map(
          person1 -> PersonResponseFactory
              .getPersonResponseBuilder(person1).build()).collect(Collectors.toList());

      groupResponseBuilder.relationships(Relationships.builder()
              .person(person)
          .build());
    }

    return groupResponseBuilder.build();
  }


}
