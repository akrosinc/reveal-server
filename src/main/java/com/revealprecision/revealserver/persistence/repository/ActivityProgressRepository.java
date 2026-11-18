package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.ActivityProgress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityProgressRepository extends JpaRepository<ActivityProgress, UUID> {

  Optional<ActivityProgress> findByPlanIdentifierAndLocationIdentifier(UUID planIdentifier,
      UUID locationIdentifier);

  @Query(value = "select nextval('activity_progress_server_version_seq')", nativeQuery = true)
  Long nextServerVersion();

  /**
   * Progress rows visible to a set of organizations: for every SELECTED plan_assignment of the
   * organizations (restricted to the requested, ACTIVE plans) return the rows of that plan for the
   * selected location and every descendant of it. Descendants come from the ancestry array on
   * location_relationship (GIN indexed) and only relationships of the plan's own hierarchy count.
   * Same scope rules as the assignment driven task sync.
   */
  @Query(value = "WITH scope AS ( "
      + "  SELECT pl.plan_identifier AS plan_identifier, pl.location_identifier AS location_identifier, "
      + "    p.hierarchy_identifier AS hierarchy_identifier "
      + "  FROM plan_assignment pa "
      + "  JOIN plan_locations pl ON pl.identifier = pa.plan_locations_identifier "
      + "  JOIN plan p ON p.identifier = pl.plan_identifier "
      + "  WHERE pa.organization_identifier IN (:organizationIds) "
      + "    AND pa.selected = true AND pa.entity_status = 'ACTIVE' "
      + "    AND pl.entity_status = 'ACTIVE' "
      + "    AND p.status = 'ACTIVE' AND p.entity_status = 'ACTIVE' "
      + "    AND pl.plan_identifier IN (:planIds) "
      + "), scope_locations AS ( "
      + "  SELECT s.plan_identifier, s.location_identifier FROM scope s "
      + "  UNION "
      + "  SELECT s.plan_identifier, lr.location_identifier FROM scope s "
      + "  JOIN location_relationship lr ON lr.ancestry @> ARRAY[s.location_identifier] "
      + "    AND lr.location_hierarchy_identifier = s.hierarchy_identifier "
      + "  WHERE lr.entity_status = 'ACTIVE' "
      + ") "
      + "SELECT ap.* FROM activity_progress ap "
      + "JOIN scope_locations sl ON sl.plan_identifier = ap.plan_identifier "
      + "  AND sl.location_identifier = ap.location_identifier "
      + "WHERE ap.server_version >= :serverVersion "
      + "ORDER BY ap.server_version", nativeQuery = true)
  List<ActivityProgress> getForAssignedOrganizations(
      @Param("organizationIds") List<UUID> organizationIds,
      @Param("planIds") List<UUID> planIds,
      @Param("serverVersion") Long serverVersion);
}
