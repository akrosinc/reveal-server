package com.revealprecision.revealserver.api.v1.facade.dto.factory;


import com.revealprecision.revealserver.api.v1.facade.dto.response.Team;
import com.revealprecision.revealserver.persistence.domain.Organization;
import java.util.Arrays;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TeamResponseFactory {

  public static Team fromEntity(Organization organization) {
    Team team = Team.builder().teamName(organization.getName()).display(organization.getName())
        .uuid(organization.getIdentifier().toString())
        .organizationIds(Arrays.asList(organization.getIdentifier())).build();
    return team;
  }
}
