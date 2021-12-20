package com.revealprecision.revealserver.persistence.repository;

import com.cosium.spring.data.jpa.entity.graph.domain.EntityGraph;
import com.cosium.spring.data.jpa.entity.graph.repository.EntityGraphJpaRepository;
import com.revealprecision.revealserver.persistence.domain.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
}
