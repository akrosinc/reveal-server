package com.revealprecision.revealserver.api.v1.facade.models;

import com.revealprecision.revealserver.api.v1.dto.request.SubjectCodableConcept;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExpressionFacade {

  private static final long serialVersionUID = 1L;

  private String description;

  private String name;

  private String language;

  private String expression;

  private String reference;

  private SubjectCodableConcept subjectCodableConcept;
}