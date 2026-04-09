package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.ComplexTag;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.checkerframework.checker.units.qual.C;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplexTagRepository extends JpaRepository<ComplexTag, Integer> {


  Set<ComplexTag> findComplexTagsByIdIn(Set<Integer> ids);

  @Query(value = "SELECT distinct ct.id from complex_tag ct, jsonb_array_elements(ct.tags) with ordinality symbols(item)\n"
      + "WHERE symbols.item->>'name' in :names",nativeQuery = true)
  Set<Integer> findComplexTagIdByTagNamesIn(Set<String> names);

  @Query(value = "select ct from ComplexTag ct Inner join InstanceComplexTag insTag on insTag.complexTag.id =  ct.id"
      + " where  insTag.instance.identifier = :instanceIdentifier ")
  Set<ComplexTag> findByInstanceId(UUID instanceIdentifier);

  Page<ComplexTag> findTagsByHierarchyId(String hierarchyId, Pageable pageable);

  Page<ComplexTag> findTagsByHierarchyIdAndIsPublicEquals(String hierarchyId, boolean isPublic, Pageable pageable);

}
