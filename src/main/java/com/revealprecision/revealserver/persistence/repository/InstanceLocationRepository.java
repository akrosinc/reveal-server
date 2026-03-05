package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Instance;
import com.revealprecision.revealserver.persistence.domain.InstanceLocation;
import com.revealprecision.revealserver.persistence.domain.id.InstanceLocationId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstanceLocationRepository extends
    JpaRepository<InstanceLocation, InstanceLocationId> {

  void deleteByInstance(Instance instance);
}
