package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.enums.BulkStatusEnum;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationBulk;
import com.revealprecision.revealserver.persistence.projection.LocationBulkProjection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationBulkRepository extends JpaRepository<LocationBulk, UUID> {

  @Query(value =
      "SELECT lbe.name as name,lbe.message as message, null as entityStatus " +
          "FROM location_bulk_exception lbe " +
          "WHERE  lbe.location_bulk_identifier = :identifier " +
          "UNION " +
          "SELECT l.name as name, null as message, l.entity_status as entityStatus " +
          "FROM location l WHERE l.location_bulk_identifier = :identifier",
      countQuery =
          "SELECT COUNT(*) FROM (SELECT lbe.name as name, lbe.message as message,null as entityStatus "
              +
              "FROM location_bulk_exception lbe " +
              "WHERE  lbe.location_bulk_identifier = :identifier " +
              "UNION " +
              "SELECT l.name as name,null as message, l.entity_status as entityStatus " +
              "FROM location l WHERE l.location_bulk_identifier = :identifier) bulk", nativeQuery = true)
  Page<LocationBulkProjection> findBulkById(@Param("identifier") UUID identifier,
      Pageable pageable);

  @Query(value =
          "SELECT l.name as name, null as message, l.entity_status as entityStatus " +
          "FROM location l WHERE l.location_bulk_identifier = :identifier", nativeQuery = true)
  Page<LocationBulkProjection> getSuccessfulLocationsFromBulk(@Param("identifier") UUID identifier,
      Pageable pageable);

  @Query(value = "select l from Location l where l.locationBulk.identifier = :identifier")
  List<Location> getAllCreatedInBulk(@Param("identifier") UUID identifier);

  Integer countByStatusNot(BulkStatusEnum bulkStatusEnum);
}
