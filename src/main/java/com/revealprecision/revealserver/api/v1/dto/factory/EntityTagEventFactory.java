package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent.OrgGrant;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent.Owner;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent.UploadGeo;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent.UserGrant;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.EntityTagAccGrantsOrganization;
import com.revealprecision.revealserver.persistence.domain.EntityTagAccGrantsUser;
import com.revealprecision.revealserver.persistence.domain.EntityTagOwnership;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.User;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
        .uploadGeo(entityTag.getUploadGeographicLevel() != null ?
            UploadGeo.builder().id(entityTag.getUploadGeographicLevel().getIdentifier())
                .name(entityTag.getUploadGeographicLevel().getName()).build() : null)
        .isPublic(entityTag.isPublic())
        .build();
  }

  public static EntityTagEvent getEntityTagEventWithGrantData(EntityTag entityTag,
      Set<Organization> orgGrants, Set<User> userGrants, User currentUser, Set<User> owners) {

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

    Map<UUID, User> ownerUserObjs = owners.stream().collect(
        Collectors.toMap(User::getSid, owner -> owner, (owner1, owner2) -> owner2));

    List<Owner> userOwners = entityTag.getOwners().stream()
        .map(owner -> ownerUserObjs.get(owner.getUserSid()))
        .map(owner -> new Owner(owner.getSid(), owner.getUsername()))
        .collect(
            Collectors.toList());

    Optional<EntityTagOwnership> entityTagOwnership1 = entityTag.getOwners().stream()
        .filter(entityTagOwnership -> entityTagOwnership.getUserSid().equals(currentUser.getSid()))
        .findFirst();

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
        .isOwner(entityTagOwnership1.isPresent())
        .referencedTag(entityTag.getReferencedTag())
        .tagAccGrantsOrganization(orgGrantObj)
        .tagAccGrantsUser(userGrantObj)
        .isPublic(entityTag.isPublic())
        .owners(userOwners)
        .isDeleting(entityTag.isDeleting())
        .uploadGeo(entityTag.getUploadGeographicLevel() != null ?
            UploadGeo.builder().id(entityTag.getUploadGeographicLevel().getIdentifier())
                .name(entityTag.getUploadGeographicLevel().getName()).build() : null)
        .build();
  }
}
