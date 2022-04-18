package com.revealprecision.revealserver.api.v1.dto.response;

import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.util.Tree;
import com.revealprecision.revealserver.util.TreeNode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GeoTree {

  public GeoTree() {
    this.locationsHierarchy = new Tree<>();
  }

  private Tree<UUID, Location> locationsHierarchy;

  public void addLocation(LocationRelationship locationRelationship) {
    if (!locationsHierarchy.hasNode(locationRelationship.getLocation().getIdentifier())) {
      if (locationRelationship.getParentLocation() == null) {
        locationsHierarchy.addNode(locationRelationship.getLocation().getIdentifier(),
            locationRelationship.getLocation().getName(), locationRelationship.getLocation(), null);
      } else {
        locationsHierarchy.addNode(locationRelationship.getLocation().getIdentifier(),
            locationRelationship.getLocation().getName(), locationRelationship.getLocation(),
            locationRelationship.getParentLocation().getIdentifier());
      }
    }
  }

  public void buildTreeFromList(List<LocationRelationship> locationRelationships) {
    for (var locationRelationship : locationRelationships) {
      if(locationRelationship.getLocation().getIdentifier().equals(UUID.fromString("10486a20-0caf-46e4-8d58-88a8ed6df5cd"))) {
        System.out.println("IMA PARENTA");
      }
      addLocation(locationRelationship);
    }
  }

  public Map<UUID, TreeNode<UUID, Location>> getLocationsHierarchy() {
    return locationsHierarchy.getTree();
  }
}
