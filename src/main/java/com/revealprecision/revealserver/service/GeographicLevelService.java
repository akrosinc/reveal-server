package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.request.GeographicLevelRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.exceptions.constant.Error;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel.Fields;
import com.revealprecision.revealserver.persistence.repository.GeographicLevelRepository;
import java.util.ArrayList;
import java.util.List;
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
    isDuplicate(geographicLevelRequest.getName());
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
    GeographicLevel updateGeographicLevel = findGeographicLevelByIdentifier(identifier);
    updateGeographicLevel.setTitle(geographicLevelRequest.getTitle());
    return geographicLevelRepository.save(updateGeographicLevel.update(geographicLevelRequest));
  }

  public void deleteGeographicLevel(UUID identifier) {
    GeographicLevel deleteGeographicLevel = findGeographicLevelByIdentifier(identifier);
    geographicLevelRepository.delete(deleteGeographicLevel);
  }

  public GeographicLevel findByName(String name) {
    return geographicLevelRepository.findByName(name)
        .orElseThrow(() -> new NotFoundException(Pair.of(GeographicLevel.Fields.name, name),
            GeographicLevel.class));
  }

  public void isDuplicate(String name) {
    geographicLevelRepository.findByName(name).ifPresent(geographicLevel -> {
      throw new ConflictException(
          String.format(Error.NON_UNIQUE, StringUtils.capitalize(Fields.name),
              name));
    });
  }

  public void validateGeographyLevels(List<String> names) {
    List<String> findLevels = new ArrayList<>(names);
    List<String> foundLevels = geographicLevelRepository.getNames(findLevels);
    findLevels.removeAll(foundLevels);
    if (!findLevels.isEmpty()) {
      throw new NotFoundException("Geography levels: " + findLevels + " does not exist");
    }
  }
}
