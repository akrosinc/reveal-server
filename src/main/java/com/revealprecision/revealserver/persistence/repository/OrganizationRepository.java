package com.revealprecision.revealserver.persistence.repository;

import com.cosium.spring.data.jpa.entity.graph.domain.EntityGraph;
import com.cosium.spring.data.jpa.entity.graph.repository.EntityGraphJpaRepository;
import com.revealprecision.revealserver.persistence.domain.Organization;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends EntityGraphJpaRepository<Organization, UUID> {

  Optional<Organization> findById(UUID id, EntityGraph graph);

  @Query(value =
      "SELECT * FROM organization o WHERE (CAST(:name as text) IS NULL OR o.name = CAST(:name as text)) "
          + "AND (CAST(:type as text) IS NULL OR o.type = CAST(:type as text))"
          + "AND o.organization_parent_id IS NULL AND entity_status='ACTIVE'", nativeQuery = true)
  Page<Organization> getAllByCriteriaWithRoot(@Param("name") String name,
      @Param("type") String type, Pageable pageable);

  @Query(value =
      "SELECT * FROM organization o WHERE (CAST(:name as text) IS NULL OR o.name = CAST(:name as text)) "
          + "AND (CAST(:type as text) IS NULL OR o.type = CAST(:type as text)) AND entity_status='ACTIVE'", nativeQuery = true)
  Page<Organization> getAllByCriteriaWithoutRoot(@Param("name") String name,
      @Param("type") String type, Pageable pageable);

  @Query(value =
      "SELECT count(*) FROM organization o WHERE (CAST(:name as text) IS NULL OR o.name = CAST(:name as text)) "
          + "AND (CAST(:type as text) IS NULL OR o.type = CAST(:type as text))"
          + "AND o.organization_parent_id IS NULL AND entity_status='ACTIVE'", nativeQuery = true)
  long getCountByCriteriaWithRoot(@Param("name") String name,
      @Param("type") String type);

  @Query(value =
      "SELECT count(*) FROM organization o WHERE (CAST(:name as text) IS NULL OR o.name = CAST(:name as text)) "
          + "AND (CAST(:type as text) IS NULL OR o.type = CAST(:type as text)) AND entity_status='ACTIVE'", nativeQuery = true)
  long getCountByCriteriaWithoutRoot(@Param("name") String name,
      @Param("type") String type);

  @Query(value = "SELECT * FROM organization o WHERE o.identifier IN :identifiers", nativeQuery = true)
  Set<Organization> findByIdentifiers(@Param("identifiers") Collection<UUID> identifiers);
}
