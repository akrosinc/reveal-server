package com.revealprecision.revealserver.api.v1.facade.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.revealprecision.revealserver.enums.LocationStatus;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LocationPropertyFacade implements Serializable {

  private static final long serialVersionUID = 1L;

  private String uid;

  private String code;

  private String type;

  private LocationStatus status;

  private String parentId;

  private String name;

  private String geographicLevel;

  private Date effectiveStartDate;

  private Date effectiveEndDate;

  private int version;

  private String username;

  @JsonInclude(Include.NON_ABSENT)
  private String surveyLocationType;

  private transient Map<String, String> customProperties;

}