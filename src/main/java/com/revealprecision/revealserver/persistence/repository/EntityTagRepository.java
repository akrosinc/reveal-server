package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.FormField;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EntityTagRepository extends JpaRepository<EntityTag, UUID> {

  Set<EntityTag> findByLookupEntityType_Identifier(UUID lookupEntityTypeIdentifier);

  Optional<EntityTag> getFirstByTag(String tag);

  List<EntityTag> findEntityTagsByFormFields(FormField formField);


  @Query(value = "select * from entity_tag et WHERE CAST(STRING_TO_ARRAY(:tagName,',') as character varying[]) &&  et.referenced_fields ORDER BY et.created_datetime", nativeQuery = true)
  Set<EntityTag> findEntityTagByReferencedFields(@Param("tagName") String tagName);

  Optional<EntityTag> findEntityTagsByTagAndLookupEntityType_Code(String tagName,
      String actionCode);
}
