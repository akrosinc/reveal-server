package com.revealprecision.revealserver.api.v1.facade.models;

import com.revealprecision.revealserver.persistence.domain.Geometry;
import java.io.Serializable;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(of = "id")
@Data
@Builder
public class PhysicalLocation implements Serializable {

  private static final long serialVersionUID = -4863877528673921296L;

  private String type;

  private String id;

  private Geometry geometry;

  private LocationPropertyFacade properties;

  private Long serverVersion;

  private Set<LocationTag> locationTags;

  private transient boolean isJurisdiction;

}