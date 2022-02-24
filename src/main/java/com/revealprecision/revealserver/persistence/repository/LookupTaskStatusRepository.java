package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.LookupTaskStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface LookupTaskStatusRepository extends JpaRepository<LookupTaskStatus, UUID>,
    JpaSpecificationExecutor<LookupTaskStatus> {

  Optional<LookupTaskStatus> findByCode(String code);

}