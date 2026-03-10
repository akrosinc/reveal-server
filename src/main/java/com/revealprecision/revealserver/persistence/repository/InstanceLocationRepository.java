package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Instance;
import com.revealprecision.revealserver.persistence.domain.InstanceLocation;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.id.InstanceLocationId;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InstanceLocationRepository extends
    JpaRepository<InstanceLocation, InstanceLocationId> {

  void deleteByInstance(Instance instance);

  @Query("SELECT DISTINCT l FROM InstanceLocation il JOIN il.location l WHERE il.instance.identifier = :instanceId")
  List<Location> getAreasByInstance(UUID instanceId);
}
