package com.revealprecision.revealserver.api.v1.dto.response;

import com.revealprecision.revealserver.messaging.message.EntityTagEvent.OrgGrant;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent.Owner;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent.UserGrant;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter
@Builder
public class ComplexTagDto {

  private String id;

  private String hierarchyId;

  private String hierarchyType;

  private String tagName;

  private List<TagWithFormulaSymbol> tags;

  private String formula;

  private List<OrgGrant> tagAccGrantsOrganization;

  private List<UserGrant> tagAccGrantsUser;

  private boolean isPublic;

  private List<OrgGrant> resultingOrgs;
  private List<UserGrant> resultingUsers;

  private boolean isOwner;

  private List<Owner> owners;


  @Builder
  @Setter @Getter
  public static class TagWithFormulaSymbol{

    private String name;

    private String symbol;
  }
}
