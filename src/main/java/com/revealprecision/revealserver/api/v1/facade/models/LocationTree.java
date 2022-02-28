package com.revealprecision.revealserver.api.v1.facade.models;

import com.revealprecision.revealserver.util.Tree;
import com.revealprecision.revealserver.util.TreeNode;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

public class LocationTree {

  private Tree<String, LocationFacade> locationsHierarchy;

  public LocationTree() {
    this.locationsHierarchy = new Tree<String, LocationFacade>();
  }

  public void addLocation(LocationFacade l) {
    if (!locationsHierarchy.hasNode(l.getLocationId())) {
      if (l.getParentLocation() == null) {
        locationsHierarchy.addNode(l.getLocationId(), l.getName(), l, null);
      } else {
        locationsHierarchy.addNode(l.getLocationId(), l.getName(), l, l.getParentLocation().getLocationId());
      }
    }
  }

  /**
   * WARNING: Overrides existing locations
   *
   * @param locationFaces
   */
  public void buildTreeFromList(List<LocationFacade> locationFaces) {
    for (LocationFacade locationFace : locationFaces) {
      addLocation(locationFace);
    }
  }

  public LocationFacade findLocation(String locationId) {
    return locationsHierarchy.getNode(locationId).getNode();
  }

  public boolean hasLocation(String locationId) {
    return locationsHierarchy.hasNode(locationId);
  }

  public boolean hasChildLocation(String locationId, String childLocationId) {
    return locationsHierarchy.getNode(locationId).findChild(childLocationId) != null;
  }

  public LinkedHashMap<String, TreeNode<String, LocationFacade>> getLocationsHierarchy() {
    return locationsHierarchy.getTree();
  }

  public LinkedHashMap<String, LinkedHashSet<String>> getChildParent() {
    return locationsHierarchy.getChildParent();
  }

  public void deleteLocation(String locationId) {
    locationsHierarchy.deleteNode(locationId);
  }
}