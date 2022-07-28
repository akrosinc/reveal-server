package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.MetadataImport;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetadataImportRepository extends JpaRepository<MetadataImport, UUID> {

}
