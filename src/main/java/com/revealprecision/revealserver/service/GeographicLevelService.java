package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.repository.GeographicLevelRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
public class GeographicLevelService {

  private GeographicLevelRepository geographicLevelRepository;

  @Autowired
  public GeographicLevelService(GeographicLevelRepository geographicLevelRepository) {
    this.geographicLevelRepository = geographicLevelRepository;
  }

  public GeographicLevel createGeographicLevel(GeographicLevel geographicLevel) {
    return geographicLevelRepository.save(geographicLevel);
  }

  public Page<GeographicLevel> getGeographicLevels(Pageable pageable) {
    return geographicLevelRepository.findAll(pageable);
  }

  public GeographicLevel findGeographicLevelByIdentifier(UUID identifier) {
    return geographicLevelRepository.findById(identifier)
        .orElseThrow(
            () -> new NotFoundException(Pair.of(GeographicLevel.Fields.identifier, identifier),
                GeographicLevel.class));
  }

  public GeographicLevel update(UUID identifier, GeographicLevel geographicLevel) {
    GeographicLevel updateGeographicLevel = findGeographicLevelByIdentifier(identifier);
    return geographicLevelRepository.save(updateGeographicLevel.update(geographicLevel));
  }

  public void deleteGeographicLevel(UUID identifier) {
    GeographicLevel deleteGeographicLevel = findGeographicLevelByIdentifier(identifier);
    geographicLevelRepository.delete(deleteGeographicLevel);
  }
}
