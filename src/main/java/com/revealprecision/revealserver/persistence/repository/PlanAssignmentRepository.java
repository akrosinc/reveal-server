package com.revealprecision.revealserver.persistence.repository;

import com.cosium.spring.data.jpa.entity.graph.repository.EntityGraphJpaRepository;
import com.revealprecision.revealserver.persistence.domain.PlanAssignment;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanAssignmentRepository extends EntityGraphJpaRepository<PlanAssignment, UUID> {
  List<PlanAssignment> findPlanAssignmentByOrganization_Identifier(UUID organizationIdentifier);
  List<PlanAssignment> findPlanAssignmentByPlanLocations_Plan_Identifier(UUID planIdentifier);
}