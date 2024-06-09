package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.EntityTag;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EntityTagRepository extends JpaRepository<EntityTag, UUID> {

  @Query(value = "select et from EntityTag et where lower(et.tag ) like concat('%', lower(:search) , '%') ")
  Page<EntityTag> findOrSearchEntityTags(Pageable pageable, String search);

  @Query(value = "select et from EntityTag et left join EntityTagOwnership eo "
      + "on eo.entityTag.identifier = et.identifier "
      + "where lower(et.tag ) like concat('%', lower(:search) , '%') and eo.userSid = :userSid")
  Page<EntityTag> findOrSearchEntityTagsByOwners(Pageable pageable, String search, UUID userSid);

  Optional<EntityTag> getFirstByTag(String tag);

  @Query(value = "select et from EntityTag et where  lower(et.tag ) like concat('%', lower(:search) , '%') and et.isAggregate = :isAggregate")
  Page<EntityTag> findEntityTagsIsAggregate(boolean isAggregate,
      Pageable pageable, String search);

  @Query(value = "select et from EntityTag et where et.isAggregate = :isAggregate ")
  List<EntityTag> findEntityTagsByIsAggregate(
      boolean isAggregate);


  @Query(value = "select et from EntityTag et where et.isAggregate = :isAggregate ")
  Page<EntityTag> findEntityTagsByIsAggregate(
      boolean isAggregate,Pageable pageable);

  Set<EntityTag> findEntityTagsByTagIn(Set<String> tags);

  Set<EntityTag> findEntityTagsByReferencedTagIn(List<UUID> id);

  List<EntityTag> findEntityTagsByMetadataImport_Identifier(UUID id);

  List<EntityTag> findEntityTagsByIdentifierIn(List<UUID> ids);

}
