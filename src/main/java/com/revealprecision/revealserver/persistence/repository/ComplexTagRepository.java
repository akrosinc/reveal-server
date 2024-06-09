package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.ComplexTag;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplexTagRepository extends JpaRepository<ComplexTag, Integer> {


  Set<ComplexTag> findComplexTagsByIdIn(Set<Integer> ids);

  @Query(value = "SELECT distinct ct.id from complex_tag ct, jsonb_array_elements(ct.tags) with ordinality symbols(item)\n"
      + "WHERE symbols.item->>'name' in :names",nativeQuery = true)
  Set<Integer> findComplexTagIdByTagNamesIn(Set<String> names);
}
