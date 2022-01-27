package com.revealprecision.revealserver.persistence.projection;

import com.revealprecision.revealserver.enums.OrganizationTypeEnum;

public interface OrganizationProjection {

  String getIdentifier();

  String getParentId();

  String getName();

  OrganizationTypeEnum getType();

  boolean getActive();

  int getLvl();
}
