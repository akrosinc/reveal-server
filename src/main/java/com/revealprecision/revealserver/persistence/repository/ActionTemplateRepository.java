package com.revealprecision.revealserver.persistence.repository;

import com.cosium.spring.data.jpa.entity.graph.repository.EntityGraphJpaRepository;
import com.revealprecision.revealserver.persistence.domain.ActionTemplate;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface ActionTemplateRepository extends EntityGraphJpaRepository<ActionTemplate, UUID> {

}
