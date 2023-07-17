package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.aggregation.GeneratedLocationRelationship;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

@Repository
public interface GeneratedLocationRelationshipRepository extends
    JpaRepository<GeneratedLocationRelationship, Integer> {

  List<GeneratedLocationRelationship> findGeneratedLocationRelationshipByGeneratedHierarchy_IdAndLocationIdentifierIn(
      int generatedHierarchyIdentifier, List<String> identifier);

  @Modifying
  void deleteGeneratedLocationRelationshipByGeneratedHierarchy_IdAndLocationIdentifierIn(
      int generatedHierarchyIdentifier, List<String> identifier);

}
