package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.LocationBulkException;
import com.revealprecision.revealserver.persistence.projection.LocationBulkProjection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationBulkExceptionRepository extends
    JpaRepository<LocationBulkException, UUID> {

  @Query(value =
      "SELECT lbe.name as name,lbe.message as message, null as entityStatus " +
          "FROM location_bulk_exception lbe " +
          "WHERE  lbe.location_bulk_identifier = :identifier " , nativeQuery = true)
  Page<LocationBulkProjection> getFailedLocationsFromBulk(@Param("identifier") UUID identifier,
      Pageable pageable);
}
