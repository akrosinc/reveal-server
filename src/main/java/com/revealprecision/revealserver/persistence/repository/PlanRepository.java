package com.revealprecision.revealserver.persistence.repository;

import com.cosium.spring.data.jpa.entity.graph.domain.EntityGraph;
import com.cosium.spring.data.jpa.entity.graph.repository.EntityGraphJpaRepository;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Plan;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanRepository extends EntityGraphJpaRepository<Plan, UUID> {

  @Query(value = "select p from Plan p "
      + "where lower(p.title) like lower(concat('%', :param, '%')) "
      + "OR lower(p.status) like lower(concat('%', :param, '%')) "
      + "OR lower(p.interventionType.name) like lower(concat('%', :param, '%'))")
  Page<Plan> getAll(@Param("param") String param, Pageable pageable, EntityGraph entityGraph);

  @Query(value = "select count(p) from Plan p "
      + "where lower(p.title) like lower(concat('%', :param, '%')) "
      + "OR lower(p.status) like lower(concat('%', :param, '%')) "
      + "OR lower(p.interventionType.name) like lower(concat('%', :param, '%'))")
  long getAllCount(@Param("param") String param);

  @Query("select pl.location from Plan p inner join PlanLocations pl on pl.plan = p WHERE p.identifier = :planIdentifier")
  List<Location> findLocationsForPlan(UUID planIdentifier);

  @Query(value = "select p from Plan p where p.status = 'ACTIVE' and (p.interventionType.code like :interventionType or :interventionType = '')")
  Page<Plan> findPlansByInterventionType(String interventionType, Pageable pageable);

  Plan findPlanByIdentifier(UUID planIdentifier);

}