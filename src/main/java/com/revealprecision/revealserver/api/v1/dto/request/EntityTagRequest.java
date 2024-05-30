package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.messaging.message.EntityTagEvent.OrgGrant;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent.UserGrant;
import com.revealprecision.revealserver.persistence.domain.MetadataImport;
import java.util.List;
import java.util.UUID;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityTagRequest {

  private String identifier;

  private String tag;

  @NotBlank(message = "must not be blank")
  private String valueType;

  private String definition;

  private List<String> aggregationMethod;

  private boolean isAggregate;

  private List<EntityTagItem> tags;

  private MetadataImport metadataImport;

  private UUID referencedTag;

  private boolean isCreated;

  private boolean isPublic;

  private List<UUID> tagAccGrantsOrganization;

  private List<UUID> tagAccGrantsUser;

  private List<OrgGrant> resultingOrgs;
  private List<UserGrant> resultingUsers;
}
