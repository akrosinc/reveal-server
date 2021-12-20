package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.request.GeographicLevelRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.exceptions.constant.Error;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel.Fields;
import com.revealprecision.revealserver.persistence.repository.GeographicLevelRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GeographicLevelService {

  private GeographicLevelRepository geographicLevelRepository;

  @Autowired
  public GeographicLevelService(GeographicLevelRepository geographicLevelRepository) {
    this.geographicLevelRepository = geographicLevelRepository;
  }

  public GeographicLevel createGeographicLevel(GeographicLevelRequest geographicLevelRequest) {
    if (findByName(geographicLevelRequest.getName()).isPresent()) {
      throw new ConflictException(
          String.format(Error.NON_UNIQUE, StringUtils.capitalize(Fields.name),
              geographicLevelRequest.getName()));
    }
    GeographicLevel geographicLevel = GeographicLevel.builder()
        .name(geographicLevelRequest.getName())
        .title(geographicLevelRequest.getTitle()).build();
    geographicLevel.setEntityStatus(EntityStatus.ACTIVE);
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

  public GeographicLevel update(UUID identifier, GeographicLevelRequest geographicLevelRequest) {
    if (findByName(geographicLevelRequest.getName()).isPresent()) {
      throw new ConflictException(
          String.format(Error.NON_UNIQUE, StringUtils.capitalize(Fields.name),
              geographicLevelRequest.getName()));
    }
    GeographicLevel updateGeographicLevel = findGeographicLevelByIdentifier(identifier);
    return geographicLevelRepository.save(updateGeographicLevel.update(geographicLevelRequest));
  }

  public void deleteGeographicLevel(UUID identifier) {
    GeographicLevel deleteGeographicLevel = findGeographicLevelByIdentifier(identifier);
    geographicLevelRepository.delete(deleteGeographicLevel);
  }

  public Optional<GeographicLevel> findByName(String name) {
    return geographicLevelRepository.findByName(name);
  }
}
