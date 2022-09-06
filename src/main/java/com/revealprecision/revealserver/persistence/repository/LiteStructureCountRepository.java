package com.revealprecision.revealserver.persistence.repository;

import com.cosium.spring.data.jpa.entity.graph.repository.EntityGraphJpaRepository;
import com.revealprecision.revealserver.persistence.domain.LiteStructureCount;
import java.util.UUID;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LiteStructureCountRepository extends EntityGraphJpaRepository<LiteStructureCount, UUID> {

  @Query(value = "REFRESH MATERIALIZED VIEW CONCURRENTLY lite_structure_count ", nativeQuery = true)
  @Modifying
  @Transactional
  void refreshLiteStructureCount();
}
