package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent.OrgGrant;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent.UserGrant;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.EntityTagAccGrantsOrganization;
import com.revealprecision.revealserver.persistence.domain.EntityTagAccGrantsUser;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.User;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EntityTagEventFactory {

  public static EntityTagEvent getEntityTagEvent(EntityTag entityTag) {
    return EntityTagEvent
        .builder()
        .aggregationMethod(entityTag.getAggregationMethod())
        .definition(entityTag.getDefinition())
        .tag(entityTag.getTag())
        .valueType(entityTag.getValueType())
        .identifier(entityTag.getIdentifier())
        .isAggregate(entityTag.isAggregate())
        .metadataImportId(
            entityTag.getMetadataImport() != null ? entityTag.getMetadataImport().getIdentifier()
                : null)
        .referencedTag(entityTag.getReferencedTag())

        .isPublic(entityTag.isPublic())
        .build();
  }

  public static EntityTagEvent getEntityTagEventWithGrantData(EntityTag entityTag,
      Set<Organization> orgGrants, Set<User> userGrants) {

    List<OrgGrant> orgGrantObj = orgGrants.stream().filter(
            organization -> entityTag.getEntityTagAccGrantsOrganizations().stream().map(
                EntityTagAccGrantsOrganization::getOrganizationId).collect(
                Collectors.toList()).contains(organization.getIdentifier()))
        .map(organization -> new OrgGrant(organization.getIdentifier(), organization.getName()))
        .collect(
            Collectors.toList());

    List<UserGrant> userGrantObj = userGrants.stream().filter(
            user -> entityTag.getEntityTagAccGrantsUsers().stream().map(
                EntityTagAccGrantsUser::getUserSid).collect(
                Collectors.toList()).contains(user.getSid()))
        .map(user -> new UserGrant(user.getSid(), user.getUsername()))
        .collect(
            Collectors.toList());

    return EntityTagEvent
        .builder()
        .aggregationMethod(entityTag.getAggregationMethod())
        .definition(entityTag.getDefinition())
        .tag(entityTag.getTag())
        .valueType(entityTag.getValueType())
        .identifier(entityTag.getIdentifier())
        .isAggregate(entityTag.isAggregate())
        .metadataImportId(
            entityTag.getMetadataImport() != null ? entityTag.getMetadataImport().getIdentifier()
                : null)
        .referencedTag(entityTag.getReferencedTag())
        .tagAccGrantsOrganization(orgGrantObj)
        .tagAccGrantsUser(userGrantObj)
        .isPublic(entityTag.isPublic())
        .build();
  }
}
