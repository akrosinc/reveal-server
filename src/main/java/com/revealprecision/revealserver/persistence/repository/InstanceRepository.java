package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Instance;
import com.revealprecision.revealserver.persistence.projection.InstanceEntityTagIdProjection;
import com.revealprecision.revealserver.persistence.projection.InstanceListProjection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.Nullable;

public interface InstanceRepository extends JpaRepository<Instance, UUID> {

  @Query("select ins from Instance ins  where ins.name like %:searchParam%")
  Page<Instance> searchInstance(String searchParam, Pageable pageable);

  @Query(
      "select ins.name as name,insEtag.entityTag.identifier as entityTagIdentifier  from Instance ins  inner join InstanceEntityTag insEtag "
          + " on insEtag.instance = ins where insEtag.id.entityTagId in :entityTagtIdList")
  List<InstanceEntityTagIdProjection> findInstancesNamesByEntityIds(List<UUID> entityTagtIdList);

  @Query("SELECT " +
      "i.identifier AS identifier, " +
      "i.name AS instanceName, " +
      "p.title AS planTitle, " +
      "CAST(p.status AS string) AS planStatus, " +
      "lit.name AS interventionType, " +
      "i.createdDatetime AS createdDatetime, " +
      "p.effectivePeriodStart AS startDate, " +
      "p.effectivePeriodEnd AS endDate " +
      "FROM Instance i " +
      "LEFT JOIN i.plans p " +
      "LEFT JOIN p.interventionType lit")
  Page<InstanceListProjection> findAllInstances(Pageable pageable);


  @Query("SELECT " +
      "i.identifier AS identifier, " +
      "i.name AS instanceName, " +
      "p.title AS planTitle, " +
      "CAST(p.status AS string) AS planStatus, " +
      "lit.name AS interventionType, " +
      "i.createdDatetime AS createdDatetime, " +
      "p.effectivePeriodStart AS startDate, " +
      "p.effectivePeriodEnd AS endDate " +
      "FROM Instance i " +
      "LEFT JOIN i.plans p " +
      "LEFT JOIN p.interventionType lit " +
      "WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :searchParam, '%'))")
  Page<InstanceListProjection> findInstanceListBySearch(
      String searchParam,
      Pageable pageable
  );

  Optional<Instance> findByName(String name);

  @Query("SELECT count (i)" +
      "FROM Instance i " +
      "WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :searchParam, '%'))")
  long countInstanceListBySearch(String searchParam);

  @Query("SELECT count (i) FROM Instance i ")
  long countAllInstances();
}
