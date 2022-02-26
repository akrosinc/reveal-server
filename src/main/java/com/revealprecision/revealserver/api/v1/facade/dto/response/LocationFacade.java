package com.revealprecision.revealserver.api.v1.facade.dto.response;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;


@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class LocationFacade extends BaseDataObject {

  private String locationId;
  private String name;
  private Address address;
  private Map<String, String> identifiers;
  private LocationFacade parentLocationFacade;
  private Set<String> tags;
  private Map<String, Object> attributes;

  public String getLocationId() {
    return locationId;
  }

  public void setLocationId(String locationId) {
    this.locationId = locationId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public Map<String, String> getIdentifiers() {
    return identifiers;
  }

  public String getIdentifier(String identifierType) {
    return identifiers.get(identifierType);
  }

  /**
   * WARNING: Overrides all existing identifiers
   *
   * @param identifiers
   * @return
   */
  public void setIdentifiers(Map<String, String> identifiers) {
    this.identifiers = identifiers;
  }

  public void addIdentifier(String identifierType, String identifier) {
    if (identifiers == null) {
      identifiers = new HashMap<>();
    }

    identifiers.put(identifierType, identifier);
  }

  public void removeIdentifier(String identifierType) {
    identifiers.remove(identifierType);
  }

  public LocationFacade getParentLocation() {
    return parentLocationFacade;
  }

  public void setParentLocation(LocationFacade parentLocationFace) {
    this.parentLocationFacade = parentLocationFace;
  }

  public Set<String> getTags() {
    return tags;
  }

  public boolean hasTag(String tag) {
    return tags.contains(tag);
  }

  /**
   * WARNING: Overrides all existing tags
   *
   * @param tags
   * @return
   */
  public void setTags(Set<String> tags) {
    this.tags = tags;
  }

  public void addTag(String tag) {
    if (tags == null) {
      tags = new HashSet<>();
    }

    tags.add(tag);
  }

  public boolean removeTag(String tag) {
    return tags.remove(tag);
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public Object getAttribute(String name) {
    return attributes == null ? null : attributes.get(name);
  }

  /**
   * WARNING: Overrides all existing attributes
   *
   * @param attributes
   * @return
   */
  public void setAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  public void addAttribute(String name, Object value) {
    if (attributes == null) {
      attributes = new HashMap<>();
    }

    attributes.put(name, value);
  }

  public void removeAttribute(String name) {
    attributes.remove(name);
  }

  public LocationFacade withLocationId(String locationId) {
    this.locationId = locationId;
    return this;
  }

  public LocationFacade withName(String name) {
    this.name = name;
    return this;
  }

  public LocationFacade withAddress(Address address) {
    this.address = address;
    return this;
  }

  /**
   * WARNING: Overrides all existing identifiers
   *
   * @param identifiers
   * @return
   */
  public LocationFacade withIdentifiers(Map<String, String> identifiers) {
    this.identifiers = identifiers;
    return this;
  }

  public LocationFacade withIdentifier(String identifierType, String identifier) {
    if (identifiers == null) {
      identifiers = new HashMap<>();
    }

    identifiers.put(identifierType, identifier);
    return this;
  }

  public LocationFacade withParentLocation(LocationFacade parentLocationFace) {
    this.parentLocationFacade = parentLocationFace;
    return this;
  }

  /**
   * WARNING: Overrides all existing tags
   *
   * @param tags
   * @return
   */
  public LocationFacade withTags(Set<String> tags) {
    this.tags = tags;
    return this;
  }

  public LocationFacade withTag(String tag) {
    if (tags == null) {
      tags = new HashSet<>();
    }

    tags.add(tag);
    return this;
  }

  /**
   * WARNING: Overrides all existing attributes
   *
   * @param attributes
   * @return
   */
  public LocationFacade withAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;
    return this;
  }

  public LocationFacade withAttribute(String name, Object value) {
    if (attributes == null) {
      attributes = new HashMap<>();
    }

    attributes.put(name, value);
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}