package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.PersonMetadata;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonMetadataRepository extends JpaRepository<PersonMetadata, UUID>,
    JpaSpecificationExecutor<PersonMetadata> {

}