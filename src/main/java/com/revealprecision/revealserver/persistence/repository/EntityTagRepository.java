package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.FormField;
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

  Page<EntityTag> findEntityTagsByScopeAndIsAggregate(String scope, boolean isAggregate,
      Pageable pageable);


  Page<EntityTag> findEntityTagsByScope(String scope, Pageable pageable);


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

}
