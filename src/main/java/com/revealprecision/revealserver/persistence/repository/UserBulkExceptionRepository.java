package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.UserBulkException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserBulkExceptionRepository extends JpaRepository<UserBulkException, UUID> {
}
