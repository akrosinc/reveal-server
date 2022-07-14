package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.CoreField;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoreFieldRepository extends JpaRepository<CoreField, UUID> {

}
