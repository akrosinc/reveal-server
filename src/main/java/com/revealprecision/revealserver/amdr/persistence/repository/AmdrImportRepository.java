package com.revealprecision.revealserver.amdr.persistence.repository;

import com.revealprecision.revealserver.amdr.persistence.domain.AmdrImport;
import com.revealprecision.revealserver.amdr.persistence.domain.AmdrProcessingStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AmdrImportRepository extends JpaRepository<AmdrImport, UUID> {


  List<AmdrImport> findAllByStatusOrderByCreatedDatetime(AmdrProcessingStatus bulkEntryStatus);
  List<AmdrImport> findAllByStatusNotOrderByCreatedDatetime(AmdrProcessingStatus bulkEntryStatus);
}
