package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.EntityTagResponse;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EntityTagResponseFactory {

  public static Page<EntityTagResponse> fromEntityPage(Page<EntityTag> entityTagPages) {
    return new PageImpl<>(entityTagPages.stream().map(EntityTagResponseFactory::fromEntity)
        .collect(Collectors.toList()));
  }

  public static EntityTagResponse fromEntity(EntityTag entityTag) {

    return EntityTagResponse.builder()
        .identifier(entityTag.getIdentifier())
        .tag(entityTag.getTag())
        .definition(entityTag.getDefinition())
        .valueType(entityTag.getValueType())
        .generationFormula(entityTag.getGenerationFormula())
        .isGenerated(entityTag.isGenerated())
        .resultExpression(entityTag.getResultExpression())
        .referenceFields(entityTag.getReferencedFields())
        .isResultLiteral(entityTag.isResultLiteral())
        .lookupEntityType(
            LookupEntityTagResponseFactory.fromEntity(entityTag.getLookupEntityType()))
        .build();
  }

}
