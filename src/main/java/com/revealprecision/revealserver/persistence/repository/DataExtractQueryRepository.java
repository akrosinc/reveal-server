package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.DataExtractQuery;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataExtractQueryRepository extends JpaRepository<DataExtractQuery, UUID> {

  List<DataExtractQuery> findByPlanIdentifier(UUID planIdentifier);

  List<DataExtractQuery> findByPlanIdentifierAndQueryLabel(UUID planIdentifier, String qeuryLabel);
}
