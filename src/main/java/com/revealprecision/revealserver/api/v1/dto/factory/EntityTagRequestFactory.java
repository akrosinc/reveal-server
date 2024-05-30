package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.EntityTagRequest;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.EntityTagAccGrantsOrganization;
import com.revealprecision.revealserver.persistence.domain.EntityTagAccGrantsUser;
import java.util.stream.Collectors;

public class EntityTagRequestFactory {

  public static EntityTagRequest getCopy(EntityTagRequest entityTag) {
    return EntityTagRequest.builder()
        .tag(entityTag.getTag())
        .aggregationMethod(entityTag.getAggregationMethod())
        .definition(entityTag.getDefinition())
        .valueType(entityTag.getValueType())
        .isAggregate(entityTag.isAggregate())
        .metadataImport(entityTag.getMetadataImport())
        .referencedTag(entityTag.getReferencedTag())
        .build();
  }

  public static EntityTagRequest getEntity(EntityTag entityTag) {
    return EntityTagRequest.builder()
        .tag(entityTag.getTag())
        .aggregationMethod(entityTag.getAggregationMethod())
        .definition(entityTag.getDefinition())
        .valueType(entityTag.getValueType())
        .isAggregate(entityTag.isAggregate())
        .metadataImport(entityTag.getMetadataImport())
        .referencedTag(entityTag.getReferencedTag())
        .tagAccGrantsOrganization(
            entityTag.getEntityTagAccGrantsOrganizations() != null
                && entityTag.getEntityTagAccGrantsOrganizations().size() > 0 ?
                entityTag.getEntityTagAccGrantsOrganizations().stream()
                    .map(
                        EntityTagAccGrantsOrganization::getOrganizationId)
                    .collect(Collectors.toList()) : null
        )
        .tagAccGrantsUser(
            entityTag.getEntityTagAccGrantsUsers() != null
            && entityTag.getEntityTagAccGrantsUsers().size() > 0 ?
            entityTag.getEntityTagAccGrantsUsers().stream()
                .map(
                    EntityTagAccGrantsUser::getUserSid)
                .collect(Collectors.toList()) : null)
        .build();
  }
}
