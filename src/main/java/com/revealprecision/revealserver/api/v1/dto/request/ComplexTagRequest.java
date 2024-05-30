package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.api.v1.dto.response.ComplexTagDto.TagWithFormulaSymbol;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent.OrgGrant;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent.UserGrant;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter
public class ComplexTagRequest {

  private String hierarchyId;

  private String hierarchyType;

  private String tagName;

  private List<TagWithFormulaSymbol> tags;

  private String formula;

  private boolean isPublic;

  private List<OrgGrant> resultingOrgs;
  private List<UserGrant> resultingUsers;

}
