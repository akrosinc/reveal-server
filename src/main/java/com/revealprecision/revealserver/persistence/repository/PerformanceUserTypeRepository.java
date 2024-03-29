package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.PerformanceUserType;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PerformanceUserTypeRepository extends JpaRepository<PerformanceUserType, UUID> {

  PerformanceUserType findPerformanceUserTypeByPlanIdentifierAndUserString(UUID planIdentifier, String user);

}
