package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.ComplexTag;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplexTagRepository extends JpaRepository<ComplexTag, Integer> {

  Set<ComplexTag> findComplexTagsByTagNameIn(Set<String> tags);
}
