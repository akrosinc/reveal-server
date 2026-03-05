package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Instance;
import com.revealprecision.revealserver.persistence.domain.InstanceEntityTag;
import com.revealprecision.revealserver.persistence.domain.id.InstanceEntityTagId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstanceEntityTagRepository extends JpaRepository<InstanceEntityTag, InstanceEntityTagId> {
  void deleteByInstance(Instance instance);
}
