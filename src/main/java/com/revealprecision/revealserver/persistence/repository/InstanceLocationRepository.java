package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Instance;
import com.revealprecision.revealserver.persistence.domain.InstanceLocation;
import com.revealprecision.revealserver.persistence.domain.id.InstanceLocationId;
import com.revealprecision.revealserver.persistence.projection.IdentifierNameProjection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InstanceLocationRepository extends
    JpaRepository<InstanceLocation, InstanceLocationId> {

  void deleteByInstance(Instance instance);

  @Query("SELECT l.identifier AS identifier, l.name AS name FROM InstanceLocation il JOIN il.location l WHERE il.instance.identifier = :instanceId")
  List<IdentifierNameProjection> getAreasIdNamesByInstance(UUID instanceId);
}
