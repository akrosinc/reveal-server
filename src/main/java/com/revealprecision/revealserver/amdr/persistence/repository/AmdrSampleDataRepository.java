package com.revealprecision.revealserver.amdr.persistence.repository;

import com.revealprecision.revealserver.amdr.persistence.domain.AmdrProcessingStatus;
import com.revealprecision.revealserver.amdr.persistence.domain.AmdrSampleData;
import com.revealprecision.revealserver.amdr.persistence.projection.AmdrImportStatusProjection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AmdrSampleDataRepository extends JpaRepository<AmdrSampleData, UUID> {

  Page<AmdrSampleData> findAllByStatus(AmdrProcessingStatus bulkEntryStatus, Pageable pageable);

  @Query("SELECT a.status as status, count(a) as count FROM AmdrSampleData a WHERE a.importId =:importId group by a.status ")
  List<AmdrImportStatusProjection> countStatusByImportId(UUID importId);

  @Query("SELECT a.status as status, count(a) as count FROM AmdrSampleData a group by a.status ")
  List<AmdrImportStatusProjection> countStatus();

}
