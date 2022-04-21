package com.revealprecision.revealserver.api.v1.facade.models;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public abstract class BaseDataObject extends BaseDataEntity {

  private String creator;

  private String dateCreated;

  private String editor;

  private String dateEdited;

  private Boolean voided;

  private String dateVoided;

  private String voider;

  private String voidReason;

  private long serverVersion;

  private Integer clientApplicationVersion;

  private Integer clientDatabaseVersion;

  private  String clientApplicationVersionName;

}
