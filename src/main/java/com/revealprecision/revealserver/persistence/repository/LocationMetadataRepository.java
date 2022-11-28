package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.metadata.LocationMetadata;
import java.util.Optional;
import java.util.UUID;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationMetadataRepository extends JpaRepository<LocationMetadata, UUID> {

  Optional<LocationMetadata> findLocationMetadataByLocation_Identifier(UUID locationIdentifier);

  @Query(value =
      "SELECT count(*)  FROM \n"
          + "location_relationship lr \n"
          + "inner join (\n"
          + "SELECT l.location_identifier FROM\n"
          + "location_metadata l,jsonb_array_elements(entity_value -> 'metadataObjs') with ordinality arr(item_object, position) \n"
          + "         where item_object->>'type' = :tagType and item_object->'current'->'value'->>'valueString' = :value\n"
          + ") t on t.location_identifier = lr.location_identifier\n"
          + "WHERE lr.parent_identifier = :parentLocationIdentifier", nativeQuery = true)
  int countLocationMetadataByTagAndStringValueForParentLocation(@Param("tagType") String tagType,
      @Param("value") String value,
      @Param("parentLocationIdentifier") UUID parentLocationIdentifier);


  @Query(value = "REFRESH MATERIALIZED VIEW CONCURRENTLY location_metadata_double_aggregate", nativeQuery = true)
  @Transactional
  @Modifying
  void refreshLocationMetadataDoubleAggregateMaterializedView();
}
