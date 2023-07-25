package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.aggregation.GeneratedHierarchyMetadata;
import com.revealprecision.revealserver.persistence.projection.EntityTagProjection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GeneratedHierarchyMetadataRepository extends JpaRepository<GeneratedHierarchyMetadata, Integer> {
  @Query(value = "SELECT DISTINCT ian.field_type as eventType, ian.tag from generated_hierarchy_metadata ian WHERE  CAST(ian.generated_hierarchy_id as varchar) =:hierarchyIdentifier",nativeQuery = true)
  List<EntityTagProjection> getUniqueDataTagsAssociatedWithData(String hierarchyIdentifier);
}
