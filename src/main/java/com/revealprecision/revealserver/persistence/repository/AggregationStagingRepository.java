package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.AggregationStaging;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AggregationStagingRepository extends JpaRepository<AggregationStaging, Integer> {

}
