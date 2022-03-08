package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Location;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {

  @Query(value = "select l from Location l where l.geographicLevel.identifier = :identifier")
  List<Location> findByGeographicLevelIdentifier(@Param("identifier") UUID identifier);

  @Query(value =
      "SELECT l FROM Location l WHERE (lower(l.name) like lower(concat('%', :param, '%'))) AND l.entityStatus='ACTIVE'")
  Page<Location> findAlLByCriteria(@Param("param") String param, Pageable pageable);

  @Query(value = "SELECT COUNT(l) FROM Location l WHERE (lower(l.name) like lower(concat('%', :param, '%'))) AND l.entityStatus='ACTIVE'")
  long findAllCountByCriteria(@Param("param") String param);

  @Query(value = "select l from Location l where l.identifier in :identifiers")
  List<Location> getAllByIdentifiers(@Param("identifiers") List<UUID> identifiers);

  @Query(value = "select l from Location  l where  l.name in :names")
  List<Location> getAllByNames(@Param("names") List<String> names);
}
