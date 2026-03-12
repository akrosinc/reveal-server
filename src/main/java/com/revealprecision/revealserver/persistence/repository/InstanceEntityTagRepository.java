package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Instance;
import com.revealprecision.revealserver.persistence.domain.InstanceEntityTag;
import com.revealprecision.revealserver.persistence.domain.id.InstanceEntityTagId;
import com.revealprecision.revealserver.persistence.projection.IdentifierNameProjection;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InstanceEntityTagRepository extends JpaRepository<InstanceEntityTag, InstanceEntityTagId> {
  void deleteByInstance(Instance instance);

  @Query("SELECT iet.entityTag.identifier AS identifier, iet.entityTag.tag AS name FROM  InstanceEntityTag iet WHERE iet.instance.identifier = :instanceIdentifier")
  List<IdentifierNameProjection> getDatasetsIdNamesByInstance(UUID instanceIdentifier);
}
