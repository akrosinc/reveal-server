package com.revealprecision.revealserver.api.v1.facade.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationPropertyFacade implements Serializable {

  private static final long serialVersionUID = 1L;

  public enum PropertyStatus {
    @JsonProperty("Active")
    ACTIVE,
    @JsonProperty("Inactive")
    INACTIVE,
    @JsonProperty("Pending Review")
    PENDING_REVIEW,
    @JsonProperty("Not Eligible")
    NOT_ELIGIBLE

  }

  private String uid;

  private String code;

  private String type;

  private PropertyStatus status;

  private String parentId;

  private String name;

  private String geographicLevel;

  private Date effectiveStartDate;

  private Date effectiveEndDate;

  private int version;

  private String username;

  private transient Map<String, String> customProperties = new HashMap<>();

}