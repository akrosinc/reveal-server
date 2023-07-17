package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.aggregation.GeneratedHierarchyMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeneratedHierarchyMetadataRepository extends JpaRepository<GeneratedHierarchyMetadata, Integer> {

}
