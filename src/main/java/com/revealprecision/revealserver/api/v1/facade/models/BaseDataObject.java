package com.revealprecision.revealserver.api.v1.facade.models;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.james.mime4j.field.datetime.DateTime;

@Getter
@Setter
@NoArgsConstructor
public abstract class BaseDataObject {

  private String creator;

  private DateTime dateCreated;

  private String editor;

  private DateTime dateEdited;

  private Boolean voided;

  private DateTime dateVoided;

  private String voider;

  private String voidReason;

  private long serverVersion;

  private Integer clientApplicationVersion;

  private Integer clientDatabaseVersion;

}
