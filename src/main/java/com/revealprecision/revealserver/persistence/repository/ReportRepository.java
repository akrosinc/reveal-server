package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Report;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

  Optional<Report> findByPlan_IdentifierAndLocation_Identifier(UUID plan, UUID location);

  @Query(value = "SELECT count(*) from report r\n"
      + "left join location l on l.identifier = r.location_id\n"
      + "left join plan p on p.identifier = r.plan_id\n"
      + "inner join location_relationships lr on l.identifier = lr.location_identifier\n"
      + "\n"
      + "WHERE r.plan_id = :planId and lr.geographic_level_name = 'structure' and lr.location_parent_identifier = :locationParentIdentifier\n"
      + "and r.report_indicators->>'businessStatus' = :businessStatus and  CAST(r.report_indicators->>'aggregateLevel' as boolean)=false",nativeQuery = true)
  Integer getCountOfStructuresByPlanLocationParentBusinessStatus(UUID planId, UUID locationParentIdentifier, String businessStatus);

  @Query(value = "SELECT count(*) from report r\n"
      + "left join location l on l.identifier = r.location_id\n"
      + "left join plan p on p.identifier = r.plan_id\n"
      + "inner join location_relationships lr on l.identifier = lr.location_identifier\n"
      + "WHERE r.plan_id = :planId and lr.geographic_level_name = 'structure' and lr.location_parent_identifier = :locationParentIdentifier\n"
      + "and CAST(r.report_indicators->>'eligible' as boolean) = true  and  CAST(r.report_indicators->>'aggregateLevel' as boolean)=false",nativeQuery = true)
  Integer getCountOfEligibleStructuresByPlanLocationParent(UUID planId, UUID locationParentIdentifier);

}
