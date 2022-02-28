package com.revealprecision.revealserver.api.v1.facade.models;

import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;


@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LocationFacade extends BaseDataObject {

  private String locationId;
  private String name;
  private Address address;
  private Map<String, String> identifiers;
  private LocationFacade parentLocation;
  private Set<String> tags;
  private Map<String, Object> attributes;

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}