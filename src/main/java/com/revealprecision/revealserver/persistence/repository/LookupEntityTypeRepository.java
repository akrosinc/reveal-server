package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.LookupEntityType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LookupEntityTypeRepository extends JpaRepository<LookupEntityType, UUID> {

  Optional<LookupEntityType> findLookupEntityTypeByTableName(String tableName);

  Optional<LookupEntityType> findLookupEntityTypeByCode(String code);
}