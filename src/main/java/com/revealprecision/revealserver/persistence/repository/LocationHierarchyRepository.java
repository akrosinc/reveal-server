package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface
LocationHierarchyRepository extends JpaRepository<LocationHierarchy, UUID> {

  @Query(value = "SELECT * FROM location_hierarchy l WHERE l.node_order = CAST(:nodes AS VARCHAR[]) AND entity_status = 'ACTIVE'", nativeQuery = true)
  List<LocationHierarchy> findByNodeOrderArray(@Param("nodes") String nodeOrder);

  @Query(value = "SELECT * FROM location_hierarchy l WHERE :name = ANY(l.node_order) AND entity_status = 'ACTIVE'", nativeQuery = true)
  List<LocationHierarchy> findLocationHierarchiesByNodeOrderContaining(@Param("name") String name);

  Optional<LocationHierarchy> findByName(String name);

  Set<LocationHierarchy> findLocationHierarchiesByIdentifierIn(Set<UUID> locationIdentifiers);

  @Query("SELECT l.identifier from LocationHierarchy l WHERE l.name = :hierarchyName")
  UUID findLocationHierarchyByName(@Param("hierarchyName") String hierarchyName);

  @Query(value = "SELECT * from location_hierarchy lh where lh.identifier = :identifier", nativeQuery = true)
  Optional<LocationHierarchy> findLocationHierarchyById(UUID identifier);

  @Query("SELECT l.identifier from LocationHierarchy l WHERE l.identifier = :hierarchyIdentifier")
  Optional<UUID> findLocationHierarchyIdentifierByIdentifier(@Param("hierarchyIdentifier") UUID hierarchyIdentifier);

  @Query("SELECT l.identifier from LocationHierarchy l WHERE l.identifier = :hierarchyIdentifier")
  UUID findLocationHierarchyByIdentifier(@Param("hierarchyIdentifier") UUID hierarchyIdentifier);

  @Query(value = "SELECT l. from location_hierarchy l WHERE l.identifier = :hierarchyIdentifier",nativeQuery = true)
  Optional<LocationHierarchy> findLocationHierarchyObjByIdentifier(@Param("hierarchyIdentifier") UUID hierarchyIdentifier);

  @Query(value = "SELECT count(*) FROM location_hierarchy lh WHERE entity_status = 'ACTIVE'",nativeQuery = true)
  Long activeHierarchyCount();

  @Query(value = "SELECT * FROM location_hierarchy lh WHERE entity_status = 'ACTIVE' and is_base_hierarchy = true",nativeQuery = true)
  Optional<LocationHierarchy> getBaseHierarchy();

  @Query(value = "SELECT count(*) FROM location_hierarchy lh WHERE entity_status = 'ACTIVE' and is_base_hierarchy = true",nativeQuery = true)
  Long activeBaseHierarchyCount();

  @Query(value = "SELECT  lh.nodeOrder  FROM LocationHierarchy lh where lh.identifier = :identifier")
  String findNodeOrderByIdentifier(UUID identifier);

  @Query(value = "SELECT  lh FROM LocationHierarchy lh where lh.baseHierarchy = false")
  Page<LocationHierarchy> findAllWithoutBaseHierarchy(Pageable pageable);

  @Query("SELECT inst.locationHierarchy from Instance inst   WHERE inst.identifier = :instanceIdentifier")
  Optional<LocationHierarchy> findLocationHierarchyByInstanceIdentifier(UUID instanceIdentifier);
}
