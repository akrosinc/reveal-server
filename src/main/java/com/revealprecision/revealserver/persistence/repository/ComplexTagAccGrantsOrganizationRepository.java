package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.ComplexTagAccGrantsOrganization;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplexTagAccGrantsOrganizationRepository extends JpaRepository<ComplexTagAccGrantsOrganization, UUID> {

  List<ComplexTagAccGrantsOrganization> findAllByComplexTag_IdAndOrganizationIdIn(Integer id, List<UUID> orgIds);
}
