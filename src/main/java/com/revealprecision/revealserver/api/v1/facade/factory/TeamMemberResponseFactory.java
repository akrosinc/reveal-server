package com.revealprecision.revealserver.api.v1.facade.factory;


import com.revealprecision.revealserver.api.v1.facade.models.TeamMember;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.User;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TeamMemberResponseFactory {

  public static TeamMember fromEntities(Organization organization, User user) {
    TeamMember teamMember = TeamMember.builder().identifier(organization.getIdentifier().toString())
        .uuid(user.getIdentifier().toString()).team(TeamResponseFactory.fromEntity(organization))
        .build();
    return teamMember;
  }
}
