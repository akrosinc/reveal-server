package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.aggregation.GeneratedLocationRelationship;
import com.revealprecision.revealserver.persistence.projection.LocationMainDataProjection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GeneratedLocationRelationshipRepository extends
    JpaRepository<GeneratedLocationRelationship, Integer> {

  List<GeneratedLocationRelationship> findGeneratedLocationRelationshipByGeneratedHierarchy_IdAndLocationIdentifierIn(
      int generatedHierarchyIdentifier, List<String> identifier);

  @Modifying
  void deleteGeneratedLocationRelationshipByGeneratedHierarchy_IdAndLocationIdentifierIn(
      int generatedHierarchyIdentifier, List<String> identifier);

  @Query(value = "SELECT CAST(l.identifier as varchar) as identifier, l.name as name from generated_location_relationship glr\n"
      + "left join location l on glr.location_identifier = CAST(l.identifier as varchar)\n"
      + "left join geographic_level gl on gl.identifier = l.geographic_level_identifier\n"
      + "where CAST(glr.generated_hierarchy_id as varchar) = :generatedHierarchyId and gl.name = :level",nativeQuery = true)
  List<LocationMainDataProjection> getLocationIdsByGeneratedHierarchyIdAndGeographicLevelName(String generatedHierarchyId, String level);
}
