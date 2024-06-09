package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.MetadataFileImportResponse;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent.Owner;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.MetadataImport;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.User;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MetadataImportResponseFactory {

  public static MetadataFileImportResponse fromEntity(MetadataImport metadataImport,
      Map<UUID, List<EntityTag>> entityMap, Set<Organization> orgGrants, Set<User> userGrants,
      User currentUser, Set<User> owners) {

    Map<UUID, User> ownerUserObjs = owners.stream().collect(
        Collectors.toMap(User::getSid, owner -> owner, (owner1, owner2) -> owner2));

    List<Owner> owners1 = metadataImport.getOwners().stream()
        .filter(metadataImportOwnership ->
            ownerUserObjs.containsKey(metadataImportOwnership.getUserSid())
                && ownerUserObjs.get(metadataImportOwnership.getUserSid()).getUsername() != null)
        .map(
            metadataImportOwnership -> new Owner(metadataImportOwnership.getUserSid(),
                ownerUserObjs.get(metadataImportOwnership.getUserSid()).getUsername())).collect(
            Collectors.toList());

    MetadataFileImportResponse build = MetadataFileImportResponse.builder()
        .filename(metadataImport.getFilename())
        .identifier(metadataImport.getIdentifier())
        .uploadDatetime(metadataImport.getUploadedDatetime())
        .status(metadataImport.getStatus())
        .uploadedBy(metadataImport.getUploadedBy())
        .owners(owners1)
        .isOwner(owners1.stream().anyMatch(owner -> owner.getId().equals(currentUser.getSid())))
        .build();

    if (entityMap != null && entityMap.containsKey(metadataImport.getIdentifier())) {
      List<EntityTag> entityTags = entityMap.get(metadataImport.getIdentifier());

      List<EntityTagEvent> collect = entityTags.stream()
          .map(entityTag -> EntityTagEventFactory.getEntityTagEventWithGrantData(entityTag,
              orgGrants, userGrants, currentUser, owners)).collect(Collectors.toList());

      build.setEntityTagEvents(collect);
    }
    return build;
  }

  public static Page<MetadataFileImportResponse> fromEntityPage(
      Page<MetadataImport> metadataImports, Map<UUID, List<EntityTag>> entityMap,
      Set<Organization> orgGrants, Set<User> userGrants, User currentUser, Set<User> owners,
      Pageable pageable) {
    var response = metadataImports.getContent().stream()
        .map(metadataImport -> MetadataImportResponseFactory.fromEntity(metadataImport, entityMap,
            orgGrants, userGrants, currentUser, owners))
        .collect(Collectors.toList());
    return new PageImpl<>(response, pageable, metadataImports.getTotalElements());
  }
}
