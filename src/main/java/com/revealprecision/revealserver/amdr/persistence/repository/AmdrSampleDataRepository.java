package com.revealprecision.revealserver.amdr.persistence.repository;

import com.revealprecision.revealserver.amdr.persistence.domain.AmdrSampleData;
import com.revealprecision.revealserver.amdr.persistence.domain.AmdrProcessingStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AmdrSampleDataRepository extends JpaRepository<AmdrSampleData, UUID> {

  Page<AmdrSampleData> findAllByStatus(AmdrProcessingStatus bulkEntryStatus, Pageable pageable);

  int countByImportId(UUID importId);

}
