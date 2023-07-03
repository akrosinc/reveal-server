package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.FormField;
import com.revealprecision.revealserver.persistence.domain.LookupEntityType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EntityTagRepository extends JpaRepository<EntityTag, UUID> {

  List<EntityTag> findByLookupEntityType_Identifier(UUID lookupEntityTypeIdentifier);

  Optional<EntityTag> getFirstByTag(String tag);

  Set<EntityTag> findEntityTagsByFormFields(FormField formField);


  @Query(value = "select et from EntityTag et where et.scope = :scope and lower(et.tag ) like concat('%', lower(:search) , '%') and et.isAggregate = :isAggregate")
  Page<EntityTag> findEntityTagsByScopeAndIsAggregate(String scope, boolean isAggregate,
      Pageable pageable, String search);


  @Query(value = "select et from EntityTag et where lower(et.tag) like concat('%', lower(:search) , '%') ")
  Page<EntityTag> findAllWithSearch(Pageable pageable, String search);

  @Query(value = "select et from EntityTag et where et.scope = :scope and lower(et.tag ) like concat('%', lower(:search) , '%') ")
  Page<EntityTag> findEntityTagsByScope(String scope, Pageable pageable, String search);


  List<EntityTag> findEntityTagsByScopeAndIsAggregateAndLookupEntityType_Identifier(String scope,
      boolean isAggregate, UUID lookupEntityTypeIdentifier);


  List<EntityTag> findEntityTagsByScopeAndLookupEntityType_Identifier(String scope,
      UUID lookupEntityTypeIdentifier);


  @Query(value = "select * from entity_tag et WHERE CAST(STRING_TO_ARRAY(:tagName,',') as character varying[]) &&  et.referenced_fields ORDER BY et.created_datetime", nativeQuery = true)
  Set<EntityTag> findEntityTagByReferencedFields(@Param("tagName") String tagName);

  Optional<EntityTag> findEntityTagsByTagAndLookupEntityType_Code(String tagName,
      String actionCode);

  List<EntityTag> findEntityTagsByIdentifierIn(Set<UUID> identifiers);


  Set<EntityTag> findEntityTagsByTagIn(Set<String> tags);

  List<EntityTag> findEntityTagsByLookupEntityTypeAndTagIn(LookupEntityType lookupEntityType, List<String> tags);

}
