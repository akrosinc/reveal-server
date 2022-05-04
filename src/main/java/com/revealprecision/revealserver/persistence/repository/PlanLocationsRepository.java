package com.revealprecision.revealserver.persistence.repository;

import com.cosium.spring.data.jpa.entity.graph.repository.EntityGraphJpaRepository;
import com.revealprecision.revealserver.persistence.domain.PlanLocations;
import java.util.List;
import java.util.UUID;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanLocationsRepository extends EntityGraphJpaRepository<PlanLocations, UUID> {

  List<PlanLocations> findByPlan_Identifier(UUID planIdentifier);

  List<PlanLocations> findByLocation_Identifier(UUID locationIdentifier);

  List<PlanLocations> findByLocation_IdentifierIn(List<UUID> locationIdentifiers);

  PlanLocations findByPlan_IdentifierAndLocation_Identifier(UUID planIdentifier,
      UUID locationIdentifier);

  List<PlanLocations> findByPlan_IdentifierAndLocation_IdentifierIn(UUID planIdentifier,
      List<UUID> locationIdentifier);

  Long countByPlan_Identifier(UUID planIdentifier);

  @Transactional
  @Modifying
  @Query(value = "delete from PlanLocations pl where pl.plan.identifier = :planIdentifier and pl.location.identifier in :locations")
  void deletePlanLocationsByPlanAndLocation(@Param("planIdentifier") UUID identifier, @Param("locations") List<UUID> locations);

}