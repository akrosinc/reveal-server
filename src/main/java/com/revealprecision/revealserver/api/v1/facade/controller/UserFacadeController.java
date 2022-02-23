package com.revealprecision.revealserver.api.v1.facade.controller;

import com.revealprecision.revealserver.api.v1.facade.dto.response.LoginResponseData;
import com.revealprecision.revealserver.api.v1.facade.dto.response.Team;
import com.revealprecision.revealserver.api.v1.facade.dto.response.TeamMember;
import com.revealprecision.revealserver.api.v1.facade.dto.response.UserFacadeResponse;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.service.UserService;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserFacadeController {

  private final UserService userService;

  @RequestMapping("/security/authenticate")
  public ResponseEntity<LoginResponseData> authenticate() {
    User user = userService.getCurrentUser();
    Optional<Organization> organizationOptional = user.getOrganizations().stream().findFirst();; //Assumption that practioner always assigned to one org.
    if(organizationOptional.isEmpty()){
      //handle case when user is not assgined
    }
    Organization organization = organizationOptional.get();

    UserFacadeResponse userFacadeResponse = UserFacadeResponse.builder()
        .baseEntityId(user.getIdentifier().toString())
        .username(user.getUsername()).firstName(user.getFirstName()).lastName(user.getLastName())
        .build();

    Team team = Team.builder().teamName(organization.getName()).display(organization.getName())
        .uuid(organization.getIdentifier().toString())
        .organizationIds(Arrays.asList(organization.getIdentifier())).build();

    TeamMember teamMember = TeamMember.builder().identifier(organization.getIdentifier().toString())
        .uuid(user.getIdentifier().toString()).team(team).build();
    //TODO: 1. solve for getting the assigned jurisdictions for the user(where jurisdiction are operational areas above structure)

    LoginResponseData loginResponseData = LoginResponseData.builder().user(userFacadeResponse)
        .team(teamMember)
        .build();
    return ResponseEntity.status(HttpStatus.OK).body(loginResponseData);
  }
}
