package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface
LocationHierarchyRepository extends JpaRepository<LocationHierarchy, UUID> {

  @Query(value = "SELECT * FROM location_hierarchy l WHERE l.node_order = CAST(:nodes AS VARCHAR[]) AND entity_status = 'ACTIVE'", nativeQuery = true)
  List<LocationHierarchy> findByNodeOrderArray(@Param("nodes") String nodeOrder);

  @Query(value = "SELECT * FROM location_hierarchy l WHERE :name = ANY(l.node_order) AND entity_status = 'ACTIVE'", nativeQuery = true)
  List<LocationHierarchy> findLocationHierarchiesByNodeOrderContaining(@Param("name") String name);

  List<LocationHierarchy> findByName(String name);

  Set<LocationHierarchy> findLocationHierarchiesByIdentifierIn(Set<UUID> locationIdentifiers);

  @Query("SELECT l.identifier from LocationHierarchy l WHERE l.name = :hierarchyName")
  UUID findLocationHierarchyByName(@Param("hierarchyName") String hierarchyName);

  @Query("SELECT l.identifier from LocationHierarchy l WHERE l.identifier = :hierarchyIdentifier")
  UUID findLocationHierarchyByIdentifier(@Param("hierarchyIdentifier") UUID hierarchyIdentifier);

  @Query(value = "SELECT count(*) FROM location_hierarchy lh WHERE entity_status = 'ACTIVE'",nativeQuery = true)
  Long activeHierarchyCount();

  @Query(value = "SELECT  lh.nodeOrder  FROM LocationHierarchy lh where lh.identifier = :identifier")
  String findNodeOrderByIdentifier(UUID identifier);
}
