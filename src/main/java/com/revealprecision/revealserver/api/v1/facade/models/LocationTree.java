package com.revealprecision.revealserver.api.v1.facade.models;

import com.revealprecision.revealserver.util.Tree;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationTree {

  private Tree<String, LocationFacade> locationsHierarchy;

  public LocationTree() {
    this.locationsHierarchy = new Tree<>();
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
}