package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.GroupResponse;
import com.revealprecision.revealserver.api.v1.dto.response.GroupResponse.GroupResponseBuilder;
import com.revealprecision.revealserver.api.v1.dto.response.GroupResponse.Relationships;
import com.revealprecision.revealserver.enums.GroupTypeEnum;
import com.revealprecision.revealserver.enums.SummaryEnum;
import com.revealprecision.revealserver.persistence.domain.Group;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GroupResponseFactory {

  public static GroupResponse fromEntity(Group group, SummaryEnum summary) {

    GroupResponseBuilder groupResponseBuilder = GroupResponse.builder()
        .identifier(group.getIdentifier()).name(group.getName())
        .type(GroupTypeEnum.getEnum(group.getType())).locationIdentifier(
            group.getLocation() == null ? null : group.getLocation().getIdentifier());

    if (summary.equals(SummaryEnum.FALSE)) {
      if (group.getPersons() != null) {
        groupResponseBuilder.relationships(
            Relationships.builder().person(group.getPersons().stream()
                .map(person1 -> PersonResponseFactory.getPersonResponseBuilder(person1).build())
                .collect(Collectors.toList())).build());
      }
    }

    return groupResponseBuilder.build();
  }

  public static Page<GroupResponse> getPageFromEntityList(Page<Group> groups, SummaryEnum summary) {

    return new PageImpl<>(groups.stream().map((group) -> fromEntity(group, summary))
        .collect(Collectors.toList()));
  }

}
