package com.revealprecision.revealserver.api.v1.facade.dto.response;

import com.revealprecision.revealserver.util.Tree;
import com.revealprecision.revealserver.util.TreeNode;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

public class LocationTree {

  private Tree<String, Location> locationsHierarchy;

  public LocationTree() {
    this.locationsHierarchy = new Tree<String, Location>();
  }

  public void addLocation(Location l) {
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
   * @param locations
   */
  public void buildTreeFromList(List<Location> locations) {
    for (Location location : locations) {
      addLocation(location);
    }
  }

  public Location findLocation(String locationId) {
    return locationsHierarchy.getNode(locationId).getNode();
  }

  public boolean hasLocation(String locationId) {
    return locationsHierarchy.hasNode(locationId);
  }

  public boolean hasChildLocation(String locationId, String childLocationId) {
    return locationsHierarchy.getNode(locationId).findChild(childLocationId) != null;
  }

  public LinkedHashMap<String, TreeNode<String, Location>> getLocationsHierarchy() {
    return locationsHierarchy.getTree();
  }

  public LinkedHashMap<String, LinkedHashSet<String>> getChildParent() {
    return locationsHierarchy.getChildParent();
  }

  public void deleteLocation(String locationId) {
    locationsHierarchy.deleteNode(locationId);
  }
}