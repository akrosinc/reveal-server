package com.revealprecision.revealserver.messaging.message;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@FieldNameConstants
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class EntityTagEvent extends Message {

  private UUID identifier;

  private String tag;

  private String valueType;

  private String definition;

  private List<String> aggregationMethod;

  private boolean isAggregate;

  private UUID metadataImportId;

  private UUID referencedTag;

  private boolean isCreated;

  private List<OrgGrant> tagAccGrantsOrganization;

  private List<UserGrant> tagAccGrantsUser;
  private List<Owner> owners;

  private boolean isPublic;

  private boolean isOwner;

  private boolean isDeleting;

  private UploadGeo uploadGeo;

  @Setter @Getter
  @AllArgsConstructor
  public static class OrgGrant{
    private UUID id;
    private String name;
  }
  @Setter @Getter
  @AllArgsConstructor
  public static class UserGrant{
    private UUID id;
    private String username;
  }

  @Setter @Getter
  @AllArgsConstructor
  public static class Owner{
    private UUID id;
    private String username;
  }

  @Setter @Getter
  @AllArgsConstructor
  @Builder
  public static class UploadGeo{
    private UUID id;
    private String name;
  }

}
