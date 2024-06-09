package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.ComplexTagAccGrantsUser;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplexTagAccGrantsUserRepository extends JpaRepository<ComplexTagAccGrantsUser, UUID> {

    List<ComplexTagAccGrantsUser> findAllByComplexTag_IdAndAndUserSidIn(Integer complexTagId,List<UUID> ids);

}
