package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.InstanceComplexTag;
import com.revealprecision.revealserver.persistence.domain.ComplexTag;
import com.revealprecision.revealserver.persistence.domain.id.InstanceComplexTagId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface InstanceComplexTagRepository extends
    JpaRepository<InstanceComplexTag, InstanceComplexTagId> {

    List<InstanceComplexTag> findByInstance_Identifier(UUID instanceId);

    void deleteByInstance_Identifier(UUID instanceId);

    @Query("SELECT ict.complexTag FROM InstanceComplexTag ict " +
        "WHERE ict.instance.identifier = :instanceId")
    List<ComplexTag> findComplexTagsByInstanceId(UUID instanceId);

    @Query("SELECT ict.complexTag.id FROM InstanceComplexTag ict " +
        "WHERE ict.instance.identifier = :instanceId")
    List<Integer> findComplexTagIdsByInstanceId(UUID instanceId);
}