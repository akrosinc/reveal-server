package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.IdentifierNameResponse;
import com.revealprecision.revealserver.api.v1.dto.response.InstanceContextResponse;
import com.revealprecision.revealserver.api.v1.dto.response.InstanceContextResponse.GroupContextInfo;
import com.revealprecision.revealserver.api.v1.dto.response.InstanceContextResponse.InstancePlanContextResponse;
import com.revealprecision.revealserver.dto.KeycloakRole;
import com.revealprecision.revealserver.persistence.domain.Instance;
import com.revealprecision.revealserver.persistence.domain.InstanceUser;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.Plan;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InstanceContextResponseFactory {

  public static InstanceContextResponse buildInstanceContextResponse(
      InstanceUser instanceUser,
      KeycloakRole instanceRole,
      List<GroupContextInfo> groups, Plan instancePlan) {

    Instance instance = instanceUser.getInstance();

    IdentifierNameResponse selectedInstance = IdentifierNameResponse.builder()
        .identifier(instance.getIdentifier())
        .name(instance.getName())
        .build();

    InstancePlanContextResponse selectedInstancePlan = InstancePlanContextResponse.builder()
        .identifier(instancePlan.getIdentifier())
        .name(instancePlan.getName())
        .planStatus(instancePlan.getStatus().name())
        .interventionType(instancePlan.getInterventionType().getName())
        .planTargetType(instancePlan.getPlanTargetType().getGeographicLevel().getName())
        .build();

    return InstanceContextResponse.builder()
        .selectedInstance(selectedInstance)
        .instancePlan(selectedInstancePlan)
        .role(instanceRole)
        .groups(groups)
        .build();
  }

  public static InstanceContextResponse.GroupContextInfo toGroupContextInfo(
      Organization org, List<KeycloakRole> orgRoles) {

    return InstanceContextResponse.GroupContextInfo.builder()
        .identifier(org.getIdentifier())
        .name(org.getName())
        .type(org.getType().name())
        .roles(orgRoles)
        .build();
  }
}