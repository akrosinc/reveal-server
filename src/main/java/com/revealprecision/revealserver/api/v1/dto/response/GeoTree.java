package com.revealprecision.revealserver.api.v1.dto.response;

import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.util.Tree;
import com.revealprecision.revealserver.util.TreeNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public class GeoTree {

  public GeoTree() {
    this.locationsHierarchy = new Tree<>();
  }

  private Tree<UUID, Location> locationsHierarchy;

  public void addLocation(LocationRelationship locationRelationship) {
    if (!locationsHierarchy.hasNode(locationRelationship.getLocation().getIdentifier())) {
      if (locationRelationship.getParentLocation() == null) {
        locationsHierarchy.addNode(locationRelationship.getLocation().getIdentifier(), locationRelationship.getLocation().getName(), locationRelationship.getLocation(), null);
      } else {
        locationsHierarchy.addNode(locationRelationship.getLocation().getIdentifier(), locationRelationship.getLocation().getName(), locationRelationship.getLocation(), locationRelationship.getParentLocation().getIdentifier());
      }
    }
  }

  public void buildTreeFromList(List<LocationRelationship> locationRelationships) {
    for (var locationRelationship : locationRelationships) {
      addLocation(locationRelationship);
    }
  }
  public LinkedHashMap<UUID, TreeNode<UUID, Location>> getLocationsHierarchy() {
    return locationsHierarchy.getTree();
  }
}
