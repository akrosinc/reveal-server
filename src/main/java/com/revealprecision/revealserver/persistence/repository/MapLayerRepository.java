package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.LayerType;
import com.revealprecision.revealserver.persistence.domain.MapLayer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MapLayerRepository extends JpaRepository<MapLayer, UUID> {

  Optional<MapLayer> findByLayerIdentifier(String layerIdentifier);

  @Query(value = "select * from map_layer m where m.layer_identifier = :layerIdentifier and m.entity_status != 'DELETED'", nativeQuery = true)
  Optional<MapLayer> getByLayerIdentifier(@Param("layerIdentifier") String layerIdentifier);

  List<MapLayer> findByType(LayerType type);

  Optional<MapLayer> findByName(String name);

  List<MapLayer> findByEntityStatus(EntityStatus entityStatus);
}
