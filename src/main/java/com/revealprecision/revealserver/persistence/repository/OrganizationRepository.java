package com.revealprecision.revealserver.persistence.repository;

import com.cosium.spring.data.jpa.entity.graph.domain.EntityGraph;
import com.cosium.spring.data.jpa.entity.graph.repository.EntityGraphJpaRepository;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.projection.OrganizationProjection;
import java.util.Collection;
import java.util.List;
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
      "SELECT o FROM Organization o WHERE (lower(o.name) like lower(concat('%', :param, '%')) "
          + "OR lower(o.type) like lower(concat('%', :param, '%'))) "
          + "AND o.parent IS NULL AND o.entityStatus='ACTIVE'")
  Page<Organization> getAllByCriteriaWithRoot(@Param("param") String param, Pageable pageable);

  @Query(value =
      "SELECT o FROM Organization o WHERE (lower(o.name) like lower(concat('%', :param, '%')) "
          + "OR lower(o.type) like lower(concat('%', :param, '%'))) AND o.entityStatus='ACTIVE'")
  Page<Organization> getAllByCriteriaWithoutRoot(@Param("param") String param, Pageable pageable);

  @Query(value =
      "SELECT count(o) FROM Organization o WHERE (lower(o.name) like lower(concat('%', :param, '%')) "
          + "OR lower(o.type) like lower(concat('%', :param, '%'))) "
          + "AND o.parent IS NULL AND o.entityStatus='ACTIVE'")
  long getCountByCriteriaWithRoot(@Param("param") String param);

  @Query(value =
      "SELECT count(o) FROM Organization o WHERE (lower(o.name) like lower(concat('%', :param, '%')) "
          + "OR lower(o.type) like lower(concat('%', :param, '%'))) AND o.entityStatus='ACTIVE'")
  long getCountByCriteriaWithoutRoot(@Param("param") String param);

  @Query(value = "SELECT * FROM organization o WHERE o.identifier IN :identifiers", nativeQuery = true)
  Set<Organization> findByIdentifiers(@Param("identifiers") Collection<UUID> identifiers);

  @Query(value = "WITH RECURSIVE ancestors(id, parent_id, name, type, active, lvl) AS ( "
      + "    SELECT org.identifier, org.organization_parent_id, org.name, org.type, org.active,1 AS lvl "
      + "    FROM organization org "
      + "    WHERE (lower(org.name) like lower(concat('%',:param, '%')) OR lower(org.type) like lower(concat('%', :param, '%'))) "
      + "    AND org.entity_status = 'ACTIVE'"
      + "    UNION ALL "
      + "    SELECT parent.identifier, parent.organization_parent_id, parent.name, parent.type, parent.active, child.lvl + 1 AS lvl "
      + "    FROM organization parent "
      + "             JOIN ancestors child "
      + "                  ON parent.identifier = child.parent_id "
      + ") "
      + "select distinct cast(a.id as varchar) as identifier, a.name as name, cast(a.parent_id as varchar) as parentId, a.type as type, a.active as active, MIN(a.lvl) as lvl from ancestors a group by a.id, a.name, a.type, a.active, a.parent_id", nativeQuery = true)
  List<OrganizationProjection> searchTreeOrganiztions(@Param("param") String param);

  @Query(value = "SELECT o from Organization o where o.identifier in :identifiers")
  List<Organization> getAllByIdentifiers(@Param("identifiers") List<UUID> identifiers,
      EntityGraph graph);
}
