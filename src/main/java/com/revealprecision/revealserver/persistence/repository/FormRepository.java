package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Form;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FormRepository extends JpaRepository<Form, UUID> {

  Optional<Form> findByName(String name);

  @Query(value = "select f from Form f "
      + "where f.template = :template "
      + "and (lower(f.name) like lower(concat('%', :search, '%')) "
      + "or lower(f.title) like lower(concat('%', :search, '%')))")
  Page<Form> findAllByCriteriaAndTemplate(Pageable pageable, @Param("search") String search,
      @Param("template") boolean template);

  @Query(value = "select f from Form f "
      + "where (lower(f.name) like lower(concat('%', :search, '%')) "
      + "or lower(f.title) like lower(concat('%', :search, '%')))")
  Page<Form> findAllByCriteria(Pageable pageable, @Param("search") String search);

  @Query(value = "select f from Form f where f.identifier IN :identifiers")
  List<Form> getFormsByIdentifiers(@Param("identifiers") List<UUID> identifiers);
}
