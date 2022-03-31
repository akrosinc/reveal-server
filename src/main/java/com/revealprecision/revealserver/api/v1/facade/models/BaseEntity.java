package com.revealprecision.revealserver.api.v1.facade.models;


import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public abstract class BaseEntity extends BaseDataObject {


  private String baseEntityId;

  private Map<String, String> identifiers;

  private List<Address> addresses;

  private Map<String, Object> attributes;

  private List<ContactPoint> contactPoints;

  private List<Photo> photos;


}
