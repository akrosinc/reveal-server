package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.ResourcePlanningMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourcePlanningMetadataRepository extends JpaRepository<ResourcePlanningMetadata, Integer> {

}
