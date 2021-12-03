package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.repository.GeographicLevelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

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

    public Page<GeographicLevel> getGeographicLevels(Integer pageNumber, Integer pageSize) {
        return geographicLevelRepository.findAll(PageRequest.of(pageNumber, pageSize));
    }

    public GeographicLevel findGeographicLevelByIdentifier(UUID identifier) {
        return geographicLevelRepository.findById(identifier)
                .orElseThrow(() -> new NotFoundException(Pair.of(GeographicLevel.Fields.identifier, identifier), GeographicLevel.class));
    }

    public GeographicLevel update(UUID identifier, GeographicLevel geographicLevel) {
        GeographicLevel updateGeographicLevel = findGeographicLevelByIdentifier(identifier);
        return geographicLevelRepository.save(updateGeographicLevel.update(geographicLevel));
    }
}
