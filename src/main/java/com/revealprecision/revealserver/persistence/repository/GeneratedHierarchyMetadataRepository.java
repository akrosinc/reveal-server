package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.aggregation.GeneratedHierarchyMetadata;
import com.revealprecision.revealserver.persistence.projection.EntityTagProjection;
import com.revealprecision.revealserver.persistence.projection.EntityTagWithGeoLevelAndEntityTypeProjection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GeneratedHierarchyMetadataRepository extends JpaRepository<GeneratedHierarchyMetadata, Integer> {
  @Query(value = "SELECT DISTINCT ian.field_type as eventType, ian.tag from generated_hierarchy_metadata ian WHERE  CAST(ian.generated_hierarchy_id as varchar) =:hierarchyIdentifier",nativeQuery = true)
  List<EntityTagProjection> getUniqueDataTagsAssociatedWithData(String hierarchyIdentifier);

  @Query(value = "  SELECT DISTINCT ian.field_type                 as eventType\n"
      + "                , ian.tag\n"
      + "                , gl.name                        as geoName\n"
      + "                , cast(gl.identifier as varchar) as geoIdentifier\n"
      + "  from generated_hierarchy_metadata ian\n"
      + "           left join location l on cast(l.identifier as varchar) = ian.location_identifier\n"
      + "           left join geographic_level gl on gl.identifier = l.geographic_level_identifier\n"
      + "\n"
      + "  WHERE CAST(ian.generated_hierarchy_id as varchar) = :hierarchyIdentifier",nativeQuery = true)
  List<EntityTagWithGeoLevelAndEntityTypeProjection> getUniqueDataTagsAndLevelsListAssociatedWithData(String hierarchyIdentifier);

}
