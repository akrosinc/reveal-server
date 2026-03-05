package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Instance;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.projection.InstanceProjection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InstanceRepository extends JpaRepository<Instance, UUID> {

  @Query("select ins from Instance ins  where ins.name like %:searchParam%")
  Page<Instance> searchInstance(String searchParam, Pageable pageable);

  @Query("select ins.name as name,insEtag.entityTag.identifier as entityTagIdentifier  from Instance ins  inner join InstanceEntityTag insEtag "
      + " on insEtag.instance = ins where insEtag.id in :entityTagtIdList")
  List<InstanceProjection> findInstancesNamesByEntityIds(List<UUID> entityTagtIdList);
}
