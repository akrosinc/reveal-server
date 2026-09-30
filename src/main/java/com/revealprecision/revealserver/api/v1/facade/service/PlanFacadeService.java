package com.revealprecision.revealserver.api.v1.facade.service;

import com.revealprecision.revealserver.api.v1.facade.request.PlanRequestFacade;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.repository.PlanAssignmentRepository;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PlanFacadeService {

  private final PlanAssignmentRepository planAssignmentRepository;

  public Set<Plan> getPlans(PlanRequestFacade planRequestFacade) {
    List<UUID> organizations = planRequestFacade.getOrganizations();
    if (organizations == null || organizations.isEmpty()) {
      return Collections.emptySet();
    }
    Long serverVersion = planRequestFacade.getServerVersion() != null
        ? planRequestFacade.getServerVersion()
        : 0L;
    List<UUID> instances = planRequestFacade.getInstances();
    List<UUID> plans = planRequestFacade.getPlans();

    boolean hasInstances = instances != null && !instances.isEmpty();
    boolean hasPlans = plans != null && !plans.isEmpty();

    if (hasInstances && hasPlans) {
      return planAssignmentRepository.findPlansByOrganizationsAndServerVersionAndInstancesAndPlans(
          organizations, serverVersion, instances, plans);
    } else if (hasInstances) {
      return planAssignmentRepository.findPlansByOrganizationsAndServerVersionAndInstances(
          organizations, serverVersion, instances);
    } else if (hasPlans) {
      return planAssignmentRepository.findPlansByOrganizationsAndServerVersionAndPlans(
          organizations, serverVersion, plans);
    } else {
      return planAssignmentRepository.findPlansByOrganizationsAndServerVersion(
          organizations, serverVersion);
    }
  }
}