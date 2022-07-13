package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.FormField;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FormFieldRepository extends JpaRepository<FormField, UUID> {

  FormField findByNameAndFormTitle(String formFieldName, String formTitle);
}
